package com.fersaiyan.cyanbridge.ui.recordings

import android.content.ContentUris
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.fersaiyan.cyanbridge.R
import com.fersaiyan.cyanbridge.databinding.ActivitySyncedMediaGalleryBinding
import com.fersaiyan.cyanbridge.media.SyncedMediaFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SyncedMediaGalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySyncedMediaGalleryBinding
    private lateinit var adapter: SyncedMediaAdapter
    private val mediaFilter: String by lazy {
        intent.getStringExtra(EXTRA_MEDIA_FILTER)?.lowercase() ?: FILTER_ALL
    }

    private val uiScope = MainScope()
    private var loadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySyncedMediaGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = titleForFilter()

        binding.tvFolderHint.text = getString(
            R.string.synced_media_folder_hint,
            SyncedMediaFolder.relativePath
        )

        adapter = SyncedMediaAdapter(
            context = this,
            onItemClick = ::openMediaItem,
        )

        binding.recyclerSyncedMedia.layoutManager = GridLayoutManager(this, gridSpanCount())
        binding.recyclerSyncedMedia.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        loadSyncedMedia()
    }

    override fun onStop() {
        super.onStop()
        loadJob?.cancel()
        loadJob = null
    }

    override fun onDestroy() {
        super.onDestroy()
        uiScope.cancel()
        adapter.release()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadSyncedMedia() {
        loadJob?.cancel()
        binding.progressLoading.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        loadJob = uiScope.launch {
            val mediaItems = withContext(Dispatchers.IO) {
                querySyncedMediaItems()
            }

            adapter.submitList(mediaItems)
            binding.progressLoading.visibility = View.GONE
            binding.emptyState.visibility = if (mediaItems.isEmpty()) View.VISIBLE else View.GONE
            binding.emptyState.text = emptyTextForFilter()
        }
    }

    private fun querySyncedMediaItems(): List<SyncedMediaItem> {
        val items = mutableListOf<SyncedMediaItem>()

        val projection = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
        )

        val mediaTypes = mediaTypesForFilter()
        val selection: String
        val selectionArgs: Array<String>

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection += MediaStore.MediaColumns.RELATIVE_PATH
            selection = buildString {
                append("(")
                mediaTypes.forEachIndexed { index, _ ->
                    if (index > 0) append(" OR ")
                    append(MediaStore.Files.FileColumns.MEDIA_TYPE)
                    append("=?")
                }
                append(") AND (")
                append(MediaStore.MediaColumns.RELATIVE_PATH)
                append("=? OR ")
                append(MediaStore.MediaColumns.RELATIVE_PATH)
                append("=? OR ")
                append(MediaStore.MediaColumns.RELATIVE_PATH)
                append(" LIKE ?)")
            }
            selectionArgs = mediaTypes.map { it.toString() }.toTypedArray() + arrayOf(
                SyncedMediaFolder.relativePath,
                SyncedMediaFolder.relativePathWithTrailingSlash,
                SyncedMediaFolder.relativePathLikePattern(),
            )
        } else {
            @Suppress("DEPRECATION")
            run {
                projection += MediaStore.MediaColumns.DATA
            }
            selection = buildString {
                append("(")
                mediaTypes.forEachIndexed { index, _ ->
                    if (index > 0) append(" OR ")
                    append(MediaStore.Files.FileColumns.MEDIA_TYPE)
                    append("=?")
                }
                append(") AND ")
                append(MediaStore.MediaColumns.DATA)
                append(" LIKE ?")
            }
            selectionArgs = mediaTypes.map { it.toString() }.toTypedArray() + arrayOf(
                SyncedMediaFolder.legacyAbsolutePathLikePattern(),
            )
        }

        val sortOrder = "${MediaStore.MediaColumns.DATE_TAKEN} DESC, ${MediaStore.MediaColumns.DATE_ADDED} DESC"

        val uri = MediaStore.Files.getContentUri("external")
        contentResolver.query(uri, projection.toTypedArray(), selection, selectionArgs, sortOrder)
            ?.use { cursor ->
                val idIdx = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val dateTakenIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                val dateAddedIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                val typeIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)

                while (cursor.moveToNext()) {
                    if (idIdx < 0 || typeIdx < 0) continue

                    val mediaType = cursor.getInt(typeIdx)
                    val kind = kindForMediaType(mediaType) ?: continue

                    val id = cursor.getLong(idIdx)
                    val contentUri = when (kind) {
                        SyncedMediaKind.IMAGE ->
                            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                        SyncedMediaKind.VIDEO ->
                            ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                        SyncedMediaKind.AUDIO ->
                            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    }

                    val name = if (nameIdx >= 0 && !cursor.isNull(nameIdx)) {
                        cursor.getString(nameIdx)
                    } else {
                        "media_$id"
                    }

                    val mime = if (mimeIdx >= 0 && !cursor.isNull(mimeIdx)) {
                        cursor.getString(mimeIdx)
                    } else {
                        when (kind) {
                            SyncedMediaKind.IMAGE -> "image/jpeg"
                            SyncedMediaKind.VIDEO -> "video/mp4"
                            SyncedMediaKind.AUDIO -> "audio/ogg"
                        }
                    }

                    val dateTakenMs = if (dateTakenIdx >= 0 && !cursor.isNull(dateTakenIdx)) {
                        cursor.getLong(dateTakenIdx)
                    } else {
                        0L
                    }

                    val dateAddedMs = if (dateAddedIdx >= 0 && !cursor.isNull(dateAddedIdx)) {
                        cursor.getLong(dateAddedIdx) * 1000L
                    } else {
                        0L
                    }

                    items += SyncedMediaItem(
                        id = id,
                        contentUri = contentUri,
                        displayName = name,
                        mimeType = mime,
                        kind = kind,
                        takenAtMs = if (dateTakenMs > 0L) dateTakenMs else dateAddedMs,
                    )
                }
            }

        return items
    }

    private fun openMediaItem(item: SyncedMediaItem) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            val type = when (item.kind) {
                SyncedMediaKind.IMAGE -> "image/*"
                SyncedMediaKind.VIDEO -> "video/*"
                SyncedMediaKind.AUDIO -> "audio/*"
            }
            setDataAndType(item.contentUri, type)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        runCatching { startActivity(intent) }
            .onFailure {
                Toast.makeText(this, getString(R.string.synced_media_open_failed), Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun gridSpanCount(): Int {
        val widthDp = resources.configuration.screenWidthDp
        return when {
            widthDp >= 900 -> 4
            widthDp >= 600 -> 3
            else -> 2
        }
    }

    private fun mediaTypesForFilter(): List<Int> {
        return when (mediaFilter) {
            FILTER_IMAGES -> listOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)
            FILTER_VIDEOS -> listOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
            FILTER_AUDIOS -> listOf(MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO)
            else -> listOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE,
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO,
                MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO,
            )
        }
    }

    private fun kindForMediaType(mediaType: Int): SyncedMediaKind? {
        return when (mediaType) {
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> SyncedMediaKind.IMAGE
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> SyncedMediaKind.VIDEO
            MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO -> SyncedMediaKind.AUDIO
            else -> null
        }
    }

    private fun titleForFilter(): String {
        return when (mediaFilter) {
            FILTER_IMAGES -> getString(R.string.synced_media_images_title)
            FILTER_VIDEOS -> getString(R.string.synced_media_videos_title)
            FILTER_AUDIOS -> getString(R.string.synced_media_audios_title)
            else -> getString(R.string.synced_media_title)
        }
    }

    private fun emptyTextForFilter(): String {
        return when (mediaFilter) {
            FILTER_IMAGES -> getString(R.string.synced_media_images_empty)
            FILTER_VIDEOS -> getString(R.string.synced_media_videos_empty)
            FILTER_AUDIOS -> getString(R.string.synced_media_audios_empty)
            else -> getString(R.string.synced_media_empty)
        }
    }

    companion object {
        const val EXTRA_MEDIA_FILTER = "extra_media_filter"
        const val FILTER_ALL = "all"
        const val FILTER_IMAGES = "images"
        const val FILTER_VIDEOS = "videos"
        const val FILTER_AUDIOS = "audios"
    }
}
