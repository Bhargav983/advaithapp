package com.fersaiyan.cyanbridge

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.widget.ArrayAdapter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.fersaiyan.cyanbridge.agent.AgentProviderType
import com.fersaiyan.cyanbridge.agent.LocalAgentPrefs as AutomationPrefs
import com.fersaiyan.cyanbridge.ui.VersionUpdateChecker
import com.fersaiyan.cyanbridge.localagent.LocalAgentController
import com.fersaiyan.cyanbridge.localagent.LocalAgentIntents
import com.fersaiyan.cyanbridge.localagent.LocalAgentPrefs
import com.fersaiyan.cyanbridge.audio.CaptureSource
import com.fersaiyan.cyanbridge.audio.MeetingCapturePrefs
import com.fersaiyan.cyanbridge.audio.MeetingCaptureService
import com.fersaiyan.cyanbridge.media.GlassesMediaPrefs
import com.fersaiyan.cyanbridge.media.SyncedMediaFolder
import com.fersaiyan.cyanbridge.media.autocapture.AutoAudioCapturePrefs
import com.fersaiyan.cyanbridge.media.autocapture.GlassesSyncedAudioIngestor
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.oudmon.ble.base.communication.utils.ByteUtil
import com.oudmon.ble.base.bluetooth.BleOperateManager
import com.oudmon.ble.base.bluetooth.DeviceManager
import com.oudmon.ble.base.communication.Constants
import com.oudmon.ble.base.communication.LargeDataHandler
import com.oudmon.ble.base.communication.bigData.resp.GlassModelControlResponse
import com.oudmon.ble.base.communication.bigData.resp.GlassesDeviceNotifyListener
import com.oudmon.ble.base.communication.bigData.resp.GlassesDeviceNotifyRsp
import com.fersaiyan.cyanbridge.databinding.AcitivytMainBinding
import com.fersaiyan.cyanbridge.ui.DeviceBindActivity
import com.fersaiyan.cyanbridge.ui.ChatListActivity
import com.fersaiyan.cyanbridge.ui.ChatThreadActivity
import com.fersaiyan.cyanbridge.ui.CommunityPluginPrefs
import com.fersaiyan.cyanbridge.ui.CommunityPluginsActivity
import com.fersaiyan.cyanbridge.ui.SettingsActivity
// import com.fersaiyan.cyanbridge.ui.notes.NotesListActivity
import com.fersaiyan.cyanbridge.ui.recordings.RecordingsListActivity
import com.fersaiyan.cyanbridge.ui.recordings.SyncedMediaGalleryActivity
import com.fersaiyan.cyanbridge.ui.BluetoothUtils
import com.fersaiyan.cyanbridge.ui.BluetoothEvent
import com.fersaiyan.cyanbridge.ui.AutoPairManager
import com.fersaiyan.cyanbridge.ui.SyncLogBuffer
import com.fersaiyan.cyanbridge.chat.ChatStore
import com.fersaiyan.cyanbridge.devices.DeviceProfileStore
import com.fersaiyan.cyanbridge.devices.GlassesManagerGating
import com.fersaiyan.cyanbridge.ai.transcription.DefaultTranscriptionService
import com.fersaiyan.cyanbridge.ai.transcription.Mp4AudioChunker
import com.fersaiyan.cyanbridge.ai.transcription.NoOpAudioChunker
import com.fersaiyan.cyanbridge.ai.transcription.OpenAIWhisperTranscriptionProvider
import com.fersaiyan.cyanbridge.ai.transcription.RetryPolicy
import com.fersaiyan.cyanbridge.ai.transcription.RetryingTranscriptionProvider
import com.fersaiyan.cyanbridge.ai.transcription.vosk.VoskModelManager
import com.fersaiyan.cyanbridge.ai.transcription.vosk.VoskTranscriptionProvider
import com.fersaiyan.cyanbridge.ai.transcription.TranscriptionProgress
import com.fersaiyan.cyanbridge.ai.transcription.TranscriptionResult
import com.fersaiyan.cyanbridge.ai.transcription.TranscriptionService
import com.fersaiyan.cyanbridge.privacy.PrivacyPrefs
import com.fersaiyan.cyanbridge.ui.MyApplication
import com.fersaiyan.cyanbridge.ui.bleIpBridge
import com.fersaiyan.cyanbridge.ui.hasBluetooth
import com.fersaiyan.cyanbridge.ui.requestAllPermission
import com.fersaiyan.cyanbridge.ui.requestBluetoothPermission
import com.fersaiyan.cyanbridge.ui.requestLocationPermission
import com.fersaiyan.cyanbridge.ui.requestNearbyWifiDevicesPermission
import com.fersaiyan.cyanbridge.ui.setOnClickListener
import com.fersaiyan.cyanbridge.ui.startKtxActivity
import com.fersaiyan.cyanbridge.ui.wifi.p2p.WifiP2pManagerSingleton
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.provider.MediaStore
import android.content.ContentValues
import android.media.MediaScannerConnection
import android.os.Environment
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import com.fersaiyan.cyanbridge.ui.BatteryOptimizationGuideActivity
// import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicReference
import javax.net.SocketFactory
import java.text.SimpleDateFormat
import java.util.Locale
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.Date
import androidx.core.content.FileProvider
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

import android.provider.Settings
import android.net.Uri
import android.app.KeyguardManager

import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.fersaiyan.cyanbridge.agent.ProSubscriptionAiPrefs
import com.fersaiyan.cyanbridge.ai.router.AiProviderPrefs
import com.fersaiyan.cyanbridge.ai.router.AiProviderType as RelayProviderType
import com.fersaiyan.cyanbridge.ai.router.CliRelayClient
import com.fersaiyan.cyanbridge.localagent.context.LocalAgentContextBuilder
import com.fersaiyan.cyanbridge.localagent.dailyfacts.DailyFactsStorage
import com.fersaiyan.cyanbridge.localagent.memory.LocalAgentMemorySearch
import com.fersaiyan.cyanbridge.localagent.memory.LocalAgentMemoryStore
import com.fersaiyan.cyanbridge.localagent.userfacts.CandidateUserFactsStorage
import com.fersaiyan.cyanbridge.localmodels.provider.LocalModelsProvider
import com.fersaiyan.cyanbridge.localmodels.settings.LocalModelRuntime
import com.fersaiyan.cyanbridge.localmodels.settings.LocalModelSettingsRepository
import com.fersaiyan.cyanbridge.localmodels.storage.LocalModelStorageRepository
import com.fersaiyan.cyanbridge.memoryvault.MemoryPolicyService


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private val ttsDoneCallbacks = ConcurrentHashMap<String, () -> Unit>()

    // Optional Local Agent UI status
    private var agentReceiverRegistered = false
    private val agentStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return

            val status = intent.getStringExtra(LocalAgentIntents.EXTRA_STATUS)
            val lastError = intent.getStringExtra(LocalAgentIntents.EXTRA_LAST_ERROR)

            if (!status.isNullOrBlank()) {
                LocalAgentPrefs.setStatus(this@MainActivity, status)
            }
            if (!lastError.isNullOrBlank()) {
                LocalAgentPrefs.setLastError(this@MainActivity, lastError)
            }

            refreshAgentStatusUi()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    private fun speak(text: String) {
        speak(text, utteranceId = null, onDone = null)
    }

    private fun speak(
        text: String,
        utteranceId: String?,
        onDone: (() -> Unit)?,
    ) {
        val id = utteranceId ?: "utt_${System.currentTimeMillis()}"
        if (onDone != null) {
            ttsDoneCallbacks[id] = onDone
        }

        val bundle = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id)
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, bundle, id)
    }
    companion object {
        const val EXTRA_TASKER_COMMAND = "tasker_command"
        private var loggedLargeDataHandlerMethods = false
        private const val AI_MODE_GEMINI = "Gemini"
        private const val AI_MODE_CHATGPT = "ChatGPT"
        private const val AI_MODE_TASKER = "Tasker"
        private const val AI_MODE_CHOSEN_PROVIDER = "ChosenProvider"
        private const val TASKER_PACKAGE_NAME = "net.dinglisch.android.taskerm"
        private const val QUERY_MAX_AGENT_PERSONA_CHARS = 1200
        private const val QUERY_MAX_USER_FACTS_CHARS = 1400
        private const val QUERY_MAX_CONFIRMED_FACTS_CHARS = 1800
        private const val QUERY_MAX_DAILY_SUMMARY_CHARS = 2200
        private const val QUERY_MAX_TOTAL_CONTEXT_CHARS = 6500

        // Max age for a fallback image to be considered "recent enough" for AI analysis.
        private const val IMAGE_FALLBACK_MAX_AGE_MS = 3L * 60L * 1000L

        fun actionTaskerCommand(appPackageName: String): String =
            "$appPackageName.ACTION_TASKER_COMMAND"

        fun aiEventAction(appPackageName: String): String =
            "$appPackageName.AI_EVENT"

        // Edit this URL before using the pull-mode OTA test button.
        // In the official app, the phone runs an HTTP server on its own
        // Wi‑Fi Direct address and the glasses fetch the file from there.
        // For experiments you can point this at a simple `python -m http.server`
        // instance on the phone or on a reachable host.
        private const val TEST_PULL_OTA_URL =
            "http://192.168.49.1:8080/dummy.swu"
    }

    private lateinit var binding: AcitivytMainBinding
    private val deviceNotifyListener by lazy { MyDeviceNotifyListener() }

    // AI Hijack settings
    private var isAiHijackEnabled = true // Default to enabled
    private var isImageAssistantMode = true // Use assistant vs share intent
    private var aiAssistantMode = AI_MODE_GEMINI

    // State used by the BLE+WiFi P2P data-download flow
    private var downloadP2pConnected = false
    private var downloadBleIp: String? = null
    private var downloadWifiIp: String? = null
    private var downloadPhoneIsGroupOwner: Boolean = true
    private var downloadInProgress = false
    private var downloadAttemptJob: Job? = null
    private var downloadResolvedHttpIp: String? = null
    private var downloadP2pNetwork: Network? = null
    private var boundNetwork: Network? = null
    private var lastP2pResetAtMs: Long = 0L
    private var downloadWifiP2pManager: WifiP2pManagerSingleton? = null
    private var downloadWifiP2pCallback: WifiP2pManagerSingleton.WifiP2pCallback? = null
    private var downloadCancelledByUser = false
    private var lastDownloadBleIpAtMs: Long = 0L
    private var downloadPeerTimeoutJob: Job? = null
    private var downloadBleIpTimeoutJob: Job? = null
    private var downloadTransferCommandAttempts = 0
    private var downloadTransferCommandSent = false
    private var downloadTransferCommandProgressDetected = false
    private var downloadTransferCallbackError: Int? = null
    private var downloadPausedForReconnect = false
    private var takePhotoAndDownloadJob: Job? = null
    private var officialWifiNetworkCallback: ConnectivityManager.NetworkCallback? = null
    private var wifiCommandTestJob: Job? = null
    private var wifiCommandTestManager: WifiP2pManagerSingleton? = null
    private var wifiCommandTestCallback: WifiP2pManagerSingleton.WifiP2pCallback? = null
    private val wifiCommandTestNotifyListener by lazy { WifiCommandTestNotifyListener() }
    private var wifiCommandTestNotifyRegistered = false
    private var wifiCommandTestCurrentCommand: String? = null
    private var wifiCommandTestPeerFound = false

    // Guard against concurrent/duplicate image queries
    private val imageQueryInProgress = java.util.concurrent.atomic.AtomicBoolean(false)
    private var lastImageQueryAtMs: Long = 0L

    // Official app registers the notify listener with cmdType=2 for album import.
    // Keep our main listener (cmdType=100) for general events, and add a narrow
    // one for the download flow so we don't duplicate thumbnail/audio handling.
    private val downloadNotifyListener by lazy { DownloadNotifyListener() }
    private var downloadNotifyListenerRegistered = false

    // UI state for P2P sync progress
    private var transferTotalJpg = 0
    private var transferTotalMp4 = 0
    private var transferTotalOpus = 0
    private var transferDoneJpg = 0
    private var transferDoneMp4 = 0
    private var transferDoneOpus = 0
    private var batteryPollJob: Job? = null
    private val batteryPollIntervalMs = 60_000L
    private var pendingBatteryToast = false
    private var batteryCallbackRegistered = false
    private val technicalSyncRows = ArrayList<TechnicalSyncRow>()

    // Chapter 5: meeting capture UI + state
    private val meetingTimerOptions: List<Pair<Long?, String>> = listOf(
        null to "No timer",
        15L * 60L to "15 min",
        60L * 60L to "1 hour",
        3L * 60L * 60L to "3 hours",
    )
    private var meetingCaptureStateReceiver: BroadcastReceiver? = null

    // Transcription UI moved to the "Transcriptions & recordings" section

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AcitivytMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBottomNavigation()
        initView()
        setupMeetingCaptureUi()
        setupAgentControlsUi()
        // Transcription UI moved to the "Transcriptions & recordings" section
        logLargeDataHandlerMethodsOnce()
        // Check for app updates
        VersionUpdateChecker.checkForUpdates(this)
        // Initialize TTS
        tts = TextToSpeech(this, this)
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
            }

            override fun onDone(utteranceId: String?) {
                utteranceId?.let { ttsDoneCallbacks.remove(it)?.invoke() }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                utteranceId?.let { ttsDoneCallbacks.remove(it)?.invoke() }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                utteranceId?.let { ttsDoneCallbacks.remove(it)?.invoke() }
            }
        })

        // Ensure we always listen for glasses reports (battery, AI, volume, etc.)
        try {
            LargeDataHandler.getInstance().addOutDeviceListener(100, deviceNotifyListener)
            recordTechnicalSyncRow(
                action = "Enable main notification listener",
                uuid = bleNotifyUuidLabel(),
                command = "N/A",
                response = "cmdType=100 notification listener enabled",
                status = "Success",
            )
        } catch (t: Throwable) {
            recordTechnicalSyncRow(
                action = "Enable main notification listener",
                uuid = bleNotifyUuidLabel(),
                command = "N/A",
                response = "failed: ${t.message}",
                status = "Failed",
            )
        }

        // Lazily register the import/download notify listener the first time we need it.
        handleTaskerCommand(intent)

        BatteryOptimizationGuideActivity.launchIfNeeded(this)
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        updateConnectionStatus(BleOperateManager.getInstance().isConnected)
        registerMeetingCaptureReceiver()
        syncMeetingCaptureUiFromPrefs()

        if (!agentReceiverRegistered) {
            LocalBroadcastManager.getInstance(this)
                .registerReceiver(agentStatusReceiver, IntentFilter(LocalAgentIntents.ACTION_STATUS_CHANGED))
            agentReceiverRegistered = true
        }
        LocalAgentController.requestStatus(this)
        refreshAgentStatusUi()
    }

    override fun onStop() {
        super.onStop()
        stopBatteryPolling()
        unregisterMeetingCaptureReceiver()

        if (agentReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(agentStatusReceiver)
            agentReceiverRegistered = false
        }

        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wifiCommandTestJob?.cancel()
        takePhotoAndDownloadJob?.cancel()
        cleanupWifiCommandTest()
        tts?.stop()
        tts?.shutdown()
    }
    inner class PermissionCallback : OnPermissionCallback {
        override fun onGranted(permissions: MutableList<String>, all: Boolean) {
            if (!all) {
                // Permissions not fully granted; do nothing for now
            } else {
                this@MainActivity.startKtxActivity<DeviceBindActivity>()
            }
        }

        override fun onDenied(permissions: MutableList<String>, never: Boolean) {
            super.onDenied(permissions, never)
            if(never){
                XXPermissions.startPermissionActivity(this@MainActivity, permissions);
            }
        }

    }


    override fun onResume() {
        super.onResume()
        try {
            if (!BluetoothUtils.isEnabledBluetooth(this)) {
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                }
                startActivityForResult(intent, 300)
            }
        } catch (e: Exception) {
        }
        if (!hasBluetooth(this)) {
            requestBluetoothPermission(this, BluetoothPermissionCallback())
        }

        requestAllPermission(this, OnPermissionCallback { permissions, all ->  })

        // Check for Overlay permission needed for background launch
        if (isAiHijackEnabled && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, 1234)
            Toast.makeText(this, "Please enable Overlay permission for background AI", Toast.LENGTH_LONG).show()
        }

        // Ensure correct nav highlight when returning via CLEAR_TOP/SINGLE_TOP.
        binding.bottomNavigation.post {
            binding.bottomNavigation.menu.findItem(R.id.nav_glasses).isChecked = true
        }
        refreshAiQueryButtonsState()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTaskerCommand(intent)
    }

    inner class BluetoothPermissionCallback : OnPermissionCallback {
        override fun onGranted(permissions: MutableList<String>, all: Boolean) {
            if (!all) {

            }
        }

        override fun onDenied(permissions: MutableList<String>, never: Boolean) {
            super.onDenied(permissions, never)
            if (never) {
                XXPermissions.startPermissionActivity(this@MainActivity, permissions)
            }
        }

    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_glasses
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_glasses -> true
                R.id.nav_chats -> {
                    binding.bottomNavigation.post {
                        val last = ChatStore.listNonEmptyThreads().firstOrNull()
                        val now = System.currentTimeMillis()

                        fun lastUserMessageAtMs(chatId: String): Long? {
                            val msgs = ChatStore.listMessages(chatId)
                            return msgs.lastOrNull { it.role == com.fersaiyan.cyanbridge.chat.ChatRole.USER }?.createdAt
                        }

                        val openChatId = if (last != null) {
                            val lastUserAt = lastUserMessageAtMs(last.id) ?: 0L
                            if (lastUserAt > 0L && (now - lastUserAt) < 30 * 60 * 1000) last.id else null
                        } else null

                        val intent = Intent(this, ChatThreadActivity::class.java)
                        if (openChatId != null) {
                            intent.putExtra(ChatThreadActivity.EXTRA_CHAT_ID, openChatId)
                        }
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(intent)
                    }
                    true
                }
                R.id.nav_transcriptions_recordings -> {
                    binding.bottomNavigation.post {
                        startActivity(Intent(this, RecordingsListActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        })
                    }
                    true
                }
                R.id.nav_settings -> {
                    binding.bottomNavigation.post {
                        startActivity(Intent(this, SettingsActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        })
                    }
                    true
                }
                R.id.nav_community_plugins -> {
                    binding.bottomNavigation.post {
                        startActivity(Intent(this, CommunityPluginsActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        })
                    }
                    true
                }
                else -> false
            }
        }
    }


    private fun initView() {
        setOnClickListener(
            binding.btnScan,
            binding.btnConnect,
            binding.btnDisconnect,
            binding.btnAddListener,
            binding.btnSetTime,
            binding.btnVersion,
            binding.btnCamera,
            binding.btnTakePhotoAndDownload,
            binding.btnTakeVideoAndDownload,
            binding.btnTakeAudioAndDownload,
            binding.btnVideo,
            binding.btnRecord,
            binding.btnBt,
            binding.btnBattery,
            binding.btnVolume,
            binding.btnMediaCount,
            binding.btnDataDownload,
            binding.btnOfficialEnableWifi,
            binding.btnWifiCommandTest,
            binding.btnCopySyncLogs,
            binding.btnCopyTechnicalSyncLog,
            binding.btnShareSyncLogs,
            binding.btnClearSyncLogs,
            binding.btnOtaInfo,
            binding.btnPullOtaTest,
            binding.btnGalleryImages,
            binding.btnGalleryVideos,
            binding.btnGalleryAudios,
            binding.btnTestHijackVoice,
            binding.btnTestHijackImage,
            binding.btnToggleAdvanced,
            // binding.btnNotes,
            binding.btnMeetingStart,
            binding.btnMeetingStop,
            binding.btnMeetingBannerStop,
            binding.btnTransferStop,
        ) {
            // Safety: stop glasses audio recording before most actions.
            // Users often press camera/video/etc while audio is running.
            val shouldStopGlassesAudio = this != binding.btnScan &&
                this != binding.btnConnect &&
                this != binding.btnTransferStop &&
                this != binding.btnDataDownload &&
                this != binding.btnOfficialEnableWifi &&
                this != binding.btnWifiCommandTest &&
                this != binding.btnCopySyncLogs &&
                this != binding.btnCopyTechnicalSyncLog &&
                this != binding.btnShareSyncLogs &&
                this != binding.btnClearSyncLogs
            if (shouldStopGlassesAudio) {
                controlAudioRecording(false)
                // If auto audio capture is enabled, give the user a short window to operate other controls.
                if (AutoAudioCapturePrefs.isEnabled(this@MainActivity) && this != binding.btnRecord) {
                    AutoAudioCapturePrefs.pauseForMs(this@MainActivity, 90_000)
                }
            }

            when (this) {
                binding.btnToggleAdvanced -> {
                    val container = binding.layoutAdvancedContainer
                    if (container.visibility == android.view.View.VISIBLE) {
                        container.visibility = android.view.View.GONE
                        binding.btnToggleAdvanced.text = "Advanced ▼"
                    } else {
                        container.visibility = android.view.View.VISIBLE
                        binding.btnToggleAdvanced.text = "Advanced ▲"
                    }
                }

                binding.btnTestHijackVoice -> {
                    triggerAssistantVoiceQuery()
                }

                binding.btnTestHijackImage -> {
                    val unsupportedReason = imageQueryUnsupportedReasonForCurrentSelection()
                    if (unsupportedReason != null) {
                        Toast.makeText(
                            this@MainActivity,
                            unsupportedReason,
                            Toast.LENGTH_SHORT,
                        ).show()
                        return@setOnClickListener
                    }

                    if (maybeShowGeminiChatGptImageRequirementsWarning()) {
                        return@setOnClickListener
                    }

                    triggerCliRelayImageCaptureAndQuery()
                }

                binding.btnGalleryImages -> openSyncedMediaGallery(SyncedMediaGalleryActivity.FILTER_IMAGES)
                binding.btnGalleryVideos -> openSyncedMediaGallery(SyncedMediaGalleryActivity.FILTER_VIDEOS)
                binding.btnGalleryAudios -> openSyncedMediaGallery(SyncedMediaGalleryActivity.FILTER_AUDIOS)

                // Notes & Summaries entry removed (moved to Transcriptions & recordings section)

                binding.btnMeetingStart -> {
                    startMeetingCaptureFromUi()
                }

                binding.btnMeetingStop, binding.btnMeetingBannerStop -> {
                    stopMeetingCaptureFromUi()
                }

                binding.btnScan -> {
                    requestLocationPermission(this@MainActivity, PermissionCallback())
                }

                binding.btnConnect -> {
                    // User explicitly wants to reconnect, so re-enable auto pairing.
                    AutoPairManager.setAutoReconnectSuppressed(false, reason = "user_reconnect_button")
                    Toast.makeText(this@MainActivity, "Reconnecting to glasses…", Toast.LENGTH_SHORT).show()
                    BleOperateManager.getInstance()
                        .connectDirectly(DeviceManager.getInstance().deviceAddress)
                }

                binding.btnDisconnect -> {
                    // Prevent the background reconnection loop from immediately reconnecting.
                    AutoPairManager.setAutoReconnectSuppressed(true, reason = "user_disconnect_button")
                    Toast.makeText(this@MainActivity, "Disconnecting from glasses…", Toast.LENGTH_SHORT).show()
                    BleOperateManager.getInstance().unBindDevice()
                }

                binding.btnAddListener -> {
                    Toast.makeText(this@MainActivity, "Registering device event listener…", Toast.LENGTH_SHORT).show()
                    LargeDataHandler.getInstance().addOutDeviceListener(100, deviceNotifyListener)
                }

                binding.btnSetTime -> {
                    Toast.makeText(this@MainActivity, "Syncing glasses time…", Toast.LENGTH_SHORT).show()
                    Log.i("setTime", "setTime" + BleOperateManager.getInstance().isConnected)
                    LargeDataHandler.getInstance().syncTime { _, _ -> }
                }

                binding.btnVersion -> {
                    Toast.makeText(this@MainActivity, "Reading device version…", Toast.LENGTH_SHORT).show()
                    LargeDataHandler.getInstance().syncDeviceInfo { _, response ->
                        if (response != null) {
                            val message =
                                "WiFi FW: ${response.wifiFirmwareVersion}, BT FW: ${response.firmwareVersion}"
                            Log.i("DeviceInfo", message)
                            recordTechnicalSyncRow(
                                action = "Read version",
                                uuid = bleWriteUuidLabel(),
                                command = "SDK syncDeviceInfo",
                                response = message,
                                status = "Confirmed",
                            )
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                            }
                        } else {
                            recordTechnicalSyncRow(
                                action = "Read version",
                                uuid = bleWriteUuidLabel(),
                                command = "SDK syncDeviceInfo",
                                response = "null response",
                                status = "Failed",
                            )
                            runOnUiThread {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Failed to get device version",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                binding.btnCamera -> {
                    LargeDataHandler.getInstance().glassesControl(
                        byteArrayOf(0x02, 0x01, 0x01)
                    ) { _, it ->
                        recordTechnicalSyncRow(
                            action = "Capture photo",
                            uuid = bleWriteUuidLabel(),
                            command = "02 01 01",
                            response = "dataType=${it.dataType}, errorCode=${it.errorCode}, workTypeIng=${it.workTypeIng}, p2pIp=${it.p2pIp}",
                            status = if (it.dataType == 1 && it.errorCode == 0) "Confirmed" else "Not confirmed",
                        )
                        if (it.dataType == 1 && it.errorCode == 0) {
                            when (it.workTypeIng) {
                                2 -> {
                                    //Glasses are recording video
                                }
                                4 -> {
                                    //Glasses are in transfer mode
                                }
                                5 -> {
                                    //Glasses are in OTA mode
                                }
                                1, 6 ->{
                                    //Glasses are in camera mode
                                }
                                7 -> {
                                    //Glasses are in AI conversation
                                }
                                8 ->{
                                    //Glasses are in recording mode
                                }
                            }
                        } else {
                            //Execute start and end
                        }
                    }
                }

                binding.btnTakePhotoAndDownload -> {
                    takePhotoAndDownload()
                }

                binding.btnTakeVideoAndDownload -> {
                    takeTimedMediaAndDownload(
                        mediaLabel = "Video",
                        startCommand = byteArrayOf(0x02, 0x01, 0x02),
                        stopCommand = byteArrayOf(0x02, 0x01, 0x03),
                        recordingMs = 10_000L,
                        saveWaitMs = 5_000L,
                    )
                }

                binding.btnTakeAudioAndDownload -> {
                    takeTimedMediaAndDownload(
                        mediaLabel = "Audio",
                        startCommand = byteArrayOf(0x02, 0x01, 0x08),
                        stopCommand = byteArrayOf(0x02, 0x01, 0x0C),
                        recordingMs = 10_000L,
                        saveWaitMs = 4_000L,
                    )
                }

                binding.btnVideo -> {
                    // Toggle video recording. While video is active, pause the auto audio loop.
                    val isRecording = GlassesMediaPrefs.isVideoRecording(this@MainActivity)
                    if (isRecording) {
                        Toast.makeText(this@MainActivity, "Stopping video recording…", Toast.LENGTH_SHORT).show()
                        controlVideoRecording(false)
                    } else {
                        Toast.makeText(this@MainActivity, "Starting video recording…", Toast.LENGTH_SHORT).show()
                        controlVideoRecording(true)
                    }
                }

                binding.btnRecord -> {
                    // Default UI behavior: start audio recording
                    controlAudioRecording(true)
                }

                binding.btnBt -> {
                    Toast.makeText(this@MainActivity, "Starting classic Bluetooth scan…", Toast.LENGTH_SHORT).show()
                    //BT scan
                    BleOperateManager.getInstance().classicBluetoothStartScan()

                }
                binding.btnBattery -> {
                    requestBatteryStatus(showToast = true)
                }
                binding.btnVolume ->{
                    Toast.makeText(this@MainActivity, "Requesting volume info…", Toast.LENGTH_SHORT).show()
                    //Read volume control and show values
                    LargeDataHandler.getInstance().getVolumeControl { _, response ->
                        if (response != null) {
                            val msg = """
                                Music: ${response.currVolumeMusic}/${response.maxVolumeMusic}
                                Call: ${response.currVolumeCall}/${response.maxVolumeCall}
                                System: ${response.currVolumeSystem}/${response.maxVolumeSystem}
                                Mode: ${response.currVolumeType}
                            """.trimIndent()
                            Log.i("VolumeControl", msg.replace('\n', ' '))
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Failed to read volume info",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
                binding.btnMediaCount ->{
                    Toast.makeText(this@MainActivity, "Requesting media count…", Toast.LENGTH_SHORT).show()
                    LargeDataHandler.getInstance().glassesControl(byteArrayOf(0x02, 0x04)) { _, it ->
                        recordTechnicalSyncRow(
                            action = "Get media count",
                            uuid = bleWriteUuidLabel(),
                            command = "02 04",
                            response = "dataType=${it.dataType}, errorCode=${it.errorCode}, image=${it.imageCount}, video=${it.videoCount}, record=${it.recordCount}",
                            status = if (it.dataType == 4) "Confirmed" else "Failed",
                        )
                        if (it.dataType == 4) {
                            val mediaCount = it.imageCount + it.videoCount + it.recordCount
                            val msg = if (mediaCount > 0) {
                                "Media not uploaded - Photos: ${it.imageCount}, Videos: ${it.videoCount}, Records: ${it.recordCount}"
                            } else {
                                "No pending media on glasses"
                            }
                            Log.i("MediaCount", msg)
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                binding.btnDataDownload -> {
                    syncInfo("DataDownload", "Sync button clicked")
                    Toast.makeText(this@MainActivity, "Starting data download…", Toast.LENGTH_SHORT).show()
                    // Check and request necessary permissions
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // Android 13+ requires NEARBY_WIFI_DEVICES permission
                        requestNearbyWifiDevicesPermission(this@MainActivity, object : OnPermissionCallback {
                            override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                                syncInfo("DataDownload", "NEARBY_WIFI_DEVICES permission callback: all=$all permissions=$permissions")
                                if (all) {
                                    // Start BLE+WiFi P2P data download
                                    startDataDownload()
                                }
                            }

                            override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                                super.onDenied(permissions, never)
                                syncWarn("DataDownload", "NEARBY_WIFI_DEVICES denied: never=$never permissions=$permissions")
                                if (never) {
                                    XXPermissions.startPermissionActivity(this@MainActivity, permissions)
                                }
                            }
                        })
                    } else {
                        // Android 12 and below start download directly
                        startDataDownload()
                    }
                }
                binding.btnOfficialEnableWifi -> {
                    runOfficialEnableWifiDebug()
                }
                binding.btnWifiCommandTest -> {
                    runWifiCommandTest()
                }
                binding.btnCopySyncLogs -> {
                    copySyncLogsToClipboard()
                }
                binding.btnCopyTechnicalSyncLog -> {
                    copyTechnicalSyncLogToClipboard()
                }
                binding.btnShareSyncLogs -> {
                    shareSyncLogs()
                }
                binding.btnClearSyncLogs -> {
                    SyncLogBuffer.clear()
                    Toast.makeText(this@MainActivity, "Sync logs cleared", Toast.LENGTH_SHORT).show()
                }
                binding.btnTransferStop -> {
                    syncWarn("DataDownload", "User tapped Stop sync")
                    cancelDataDownloadAttempt(
                        reason = "Sync stopped by user",
                        showToast = true,
                    )
                }
                binding.btnOtaInfo -> {
                    Toast.makeText(this@MainActivity, "Dumping OTA server info…", Toast.LENGTH_SHORT).show()
                    dumpOtaServerInfo()
                }
                binding.btnPullOtaTest -> {
                    Toast.makeText(this@MainActivity, "Triggering pull‑mode OTA test…", Toast.LENGTH_SHORT).show()
                    testPullModeOta()
                }
            }
        }

        refreshAiModeButtons()

        binding.cbHijackEnabled.setOnCheckedChangeListener { _, isChecked ->

            isAiHijackEnabled = isChecked
            Toast.makeText(this, "Hijack ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        binding.cbImageAsAssistant.isChecked = isImageAssistantMode
        binding.cbImageAsAssistant.text = if (isImageAssistantMode) "Direct Assistant" else "App Sharing"
        
        binding.cbImageAsAssistant.setOnCheckedChangeListener { _, isChecked ->
            isImageAssistantMode = isChecked
            val modeName = if (isChecked) "Direct Assistant" else "App Sharing"
            binding.cbImageAsAssistant.text = modeName
            Toast.makeText(this, "Image Hijack: $modeName", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dumpOtaServerInfo() {
        if (!BleOperateManager.getInstance().isConnected) {
            Log.e("OTAProbe", "Bluetooth not connected. Please connect to glasses first.")
            Toast.makeText(
                this,
                "Bluetooth not connected. Please connect to glasses first.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        LargeDataHandler.getInstance().syncDeviceInfo { _, response ->
            if (response == null) {
                Log.e("OTAProbe", "syncDeviceInfo returned null response")
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Failed to read device info for OTA",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@syncDeviceInfo
            }

            val wifiHw = response.wifiHardwareVersion ?: ""
            val wifiFw = response.wifiFirmwareVersion ?: ""
            val btFw = response.firmwareVersion ?: ""
            val hw = response.hardwareVersion ?: ""

            // OTA binary URL used by the official app's debug/down path.
            val otaBinaryUrl =
                "https://qcwxfactory.oss-cn-beijing.aliyuncs.com/bin/glasses/${wifiHw}.swu"

            // Try to download the OTA file directly into the app's files dir
            // so you can pull it with `adb` for inspection.
            val otaDir = File(getExternalFilesDir(null), "ota")
            if (!otaDir.exists()) {
                otaDir.mkdirs()
            }
            val outFile = File(otaDir, "${wifiHw}.swu")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.i(
                        "OTAProbe",
                        "Attempting OTA binary download to: ${outFile.absolutePath}"
                    )
                    val url = URL(otaBinaryUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 15000
                    conn.readTimeout = 60000

                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        conn.inputStream.use { input ->
                            FileOutputStream(outFile).use { output ->
                                val buffer = ByteArray(8 * 1024)
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read <= 0) break
                                    output.write(buffer, 0, read)
                                }
                                output.flush()
                            }
                        }
                        Log.i(
                            "OTAProbe",
                            "OTA binary download completed: ${outFile.absolutePath} (size=${outFile.length()} bytes)"
                        )
                    } else {
                        Log.e(
                            "OTAProbe",
                            "OTA binary download failed, HTTP ${conn.responseCode}"
                        )
                    }
                    conn.disconnect()
                } catch (e: Exception) {
                    Log.e(
                        "OTAProbe",
                        "Exception while downloading OTA binary: ${e.message}",
                        e
                    )
                }
            }

            Log.i("OTAProbe", "==== OTA SERVER INFO START ====")
            Log.i("OTAProbe", "Device hardware version     : $hw")
            Log.i("OTAProbe", "WiFi hardware version       : $wifiHw")
            Log.i("OTAProbe", "WiFi firmware version       : $wifiFw")
            Log.i("OTAProbe", "Bluetooth firmware version  : $btFw")
            Log.i(
                "OTAProbe",
                "OTA metadata API (global)   : https://www.qlifesnap.com/glasses/app-update/last-ota"
            )
            Log.i(
                "OTAProbe",
                "OTA metadata API (China)    : https://www.qlifesnap.com/glasses/app-update/last-ota/china"
            )
            Log.i("OTAProbe", "OTA binary URL candidate    : $otaBinaryUrl")

            val lastOtaJsonTemplate = """
                {
                  "appId": <APP_ID>,
                  "uid": <USER_ID>,
                  "hardwareVersion": "$wifiHw",
                  "romVersion": "$wifiFw",
                  "os": 1,
                  "mac": "<PHONE_OR_BT_MAC>",
                  "country": "<COUNTRY_CODE>",
                  "dev": 2
                }
            """.trimIndent()

            Log.i("OTAProbe", "Sample LastOtaRequest JSON (fill in placeholders):")
            Log.i("OTAProbe", lastOtaJsonTemplate)
            Log.i(
                "OTAProbe",
                "Sample curl (metadata): curl -X POST 'https://www.qlifesnap.com/glasses/app-update/last-ota' -H 'Content-Type: application/json' -d '<JSON_ABOVE>'"
            )
            Log.i(
                "OTAProbe",
                "Sample curl (binary)  : curl -o '${wifiHw}.swu' '$otaBinaryUrl'"
            )
            Log.i("OTAProbe", "==== OTA SERVER INFO END ====")

            runOnUiThread {
                Toast.makeText(
                    this,
                    "OTA server info dumped to logcat (tag: OTAProbe)",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Minimal wrapper around LargeDataHandler.writeIpToSoc so we can observe
     * how the glasses behave when asked to fetch an OTA image from an HTTP
     * server under our control.
     *
     * This does not start any HTTP server on the phone; you must run one
     * yourself and point TEST_PULL_OTA_URL at it.
     */
    private fun testPullModeOta() {
        if (!BleOperateManager.getInstance().isConnected) {
            Log.e("PullOtaTest", "Bluetooth not connected. Please connect to glasses first.")
            Toast.makeText(
                this,
                "Bluetooth not connected. Please connect to glasses first.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val url = TEST_PULL_OTA_URL
        if (url.isBlank()) {
            Log.e("PullOtaTest", "TEST_PULL_OTA_URL is blank; edit MainActivity to set it.")
            Toast.makeText(
                this,
                "TEST_PULL_OTA_URL is blank. Edit MainActivity first.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        Log.i("PullOtaTest", "Calling writeIpToSoc with URL: $url")
        LargeDataHandler.getInstance().writeIpToSoc(url) { cmdType, response ->
            Log.i(
                "PullOtaTest",
                "writeIpToSoc callback: cmdType=$cmdType, response=$response"
            )
        }
    }
    
    private fun controlVideoRecording(start: Boolean) {
        val value = if (start) 0x02 else 0x03

        // While video is recording, pause the auto audio loop.
        if (start) {
            AutoAudioCapturePrefs.setPausedForVideo(this, true)
            GlassesMediaPrefs.setVideoRecording(this, true) // optimistic
        }

        LargeDataHandler.getInstance().glassesControl(
            byteArrayOf(0x02, 0x01, value.toByte())
        ) { _, rsp ->
            recordTechnicalSyncRow(
                action = if (start) "Start video" else "Stop video before transfer",
                uuid = bleWriteUuidLabel(),
                command = if (start) "02 01 02" else "02 01 03",
                response = "dataType=${rsp.dataType}, errorCode=${rsp.errorCode}, workTypeIng=${rsp.workTypeIng}, p2pIp=${rsp.p2pIp}, image=${rsp.imageCount}, video=${rsp.videoCount}, record=${rsp.recordCount}",
                status = if (rsp.errorCode == 0) "Confirmed" else "Non-fatal error",
            )
            if (rsp.dataType == 1) {
                if (rsp.errorCode == 0) {
                    when (rsp.workTypeIng) {
                        2 -> {
                            // Glasses are recording video
                            GlassesMediaPrefs.setVideoRecording(this, true)
                            AutoAudioCapturePrefs.setPausedForVideo(this, true)
                        }
                        else -> {
                            // Anything other than 2 means not actively recording video.
                            GlassesMediaPrefs.setVideoRecording(this, false)
                            AutoAudioCapturePrefs.setPausedForVideo(this, false)
                        }
                    }
                } else {
                    // Command failed; revert optimistic state.
                    if (start) {
                        GlassesMediaPrefs.setVideoRecording(this, false)
                        AutoAudioCapturePrefs.setPausedForVideo(this, false)
                    }
                }
            }
        }
    }
    
    private fun controlAudioRecording(start: Boolean) {
        val value = if (start) 0x08 else 0x0c
        LargeDataHandler.getInstance().glassesControl(
            byteArrayOf(0x02, 0x01, value.toByte())
        ) { _, it ->
            recordTechnicalSyncRow(
                action = if (start) "Start audio" else "Stop audio before transfer",
                uuid = bleWriteUuidLabel(),
                command = if (start) "02 01 08" else "02 01 0C",
                response = "dataType=${it.dataType}, errorCode=${it.errorCode}, workTypeIng=${it.workTypeIng}, p2pIp=${it.p2pIp}, image=${it.imageCount}, video=${it.videoCount}, record=${it.recordCount}",
                status = if (it.errorCode == 0) "Confirmed" else "Non-fatal error",
            )
            if (it.dataType == 1) {
                if (it.errorCode == 0) {
                    when (it.workTypeIng) {
                        2 -> {
                            //Glasses are recording video
                        }
                        4 -> {
                            //Glasses are in transfer mode
                        }
                        5 -> {
                            //Glasses are in OTA mode
                        }
                        1, 6 ->{
                            //Glasses are in camera mode
                        }
                        7 -> {
                            //Glasses are in AI conversation
                        }
                        8 ->{
                            //Glasses are in recording mode
                        }
                    }
                } else {
                    //Execute start and end
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBluetoothEvent(event: BluetoothEvent) {
        updateConnectionStatus(event.connect)
        if (event.connect) {
            requestBatteryStatus(showToast = false)
        } else {
            updateBatteryText(null)
        }
    }

    private fun startBatteryPolling() {
        if (batteryPollJob?.isActive == true) {
            return
        }
        batteryPollJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                if (BleOperateManager.getInstance().isConnected) {
                    requestBatteryStatus(showToast = false)
                } else {
                    updateBatteryText(null)
                }
                delay(batteryPollIntervalMs)
            }
        }
    }

    private fun stopBatteryPolling() {
        batteryPollJob?.cancel()
        batteryPollJob = null
    }

    private fun resolveEffectiveAiAssistantMode(): String {
        if (aiAssistantMode != AI_MODE_CHOSEN_PROVIDER) {
            return aiAssistantMode
        }
        return when (AutomationPrefs.getProviderType(this)) {
            AgentProviderType.TASKER -> AI_MODE_TASKER
            AgentProviderType.PRO_SUBSCRIPTION -> AI_MODE_CHOSEN_PROVIDER
            AgentProviderType.LOCAL_AGENT -> AI_MODE_CHOSEN_PROVIDER
        }
    }

    private fun isChosenProviderMode(): Boolean = aiAssistantMode == AI_MODE_CHOSEN_PROVIDER

    private fun isChosenProviderCloudEndpoint(): Boolean {
        if (!isChosenProviderMode()) return false
        return when (AutomationPrefs.getProviderType(this)) {
            AgentProviderType.PRO_SUBSCRIPTION -> true
            AgentProviderType.LOCAL_AGENT,
            AgentProviderType.TASKER -> false
        }
    }

    private fun imageQueryUnsupportedReasonForCurrentSelection(): String? {
        if (!isChosenProviderMode()) return null
        if (AutomationPrefs.getProviderType(this) != AgentProviderType.LOCAL_AGENT) return null

        val selected = LocalModelStorageRepository.resolveSelectedModel(this)
            ?: return "No local model selected. Install/select Gemma 4 LiteRT first."
        val settings = LocalModelSettingsRepository.getForModel(this, selected.id)
        if (settings.modelRuntime != LocalModelRuntime.LITERT) {
            return "Image questions require Local Runtime = LiteRT for the selected model."
        }

        val modelHint = "${selected.displayName} ${selected.catalogId.orEmpty()} ${selected.fileName}".lowercase(Locale.US)
        if (!modelHint.contains("gemma")) {
            return "Select a Gemma LiteRT model for local image questions."
        }
        return null
    }

    private fun isGeminiOrChatGptModeSelected(): Boolean {
        return aiAssistantMode == AI_MODE_GEMINI || aiAssistantMode == AI_MODE_CHATGPT
    }

    private fun requiresTaskerAutomationForImageQuestions(): Boolean {
        if (!isGeminiOrChatGptModeSelected()) return false
        return AiProviderPrefs.getProvider(this) != RelayProviderType.CLI_RELAY
    }

    private fun isTaskerInstalled(): Boolean {
        return runCatching {
            packageManager.getPackageInfo(TASKER_PACKAGE_NAME, 0)
            true
        }.getOrDefault(false)
    }

    private fun maybeShowGeminiChatGptImageRequirementsWarning(): Boolean {
        if (!requiresTaskerAutomationForImageQuestions()) return false

        val taskerInstalled = isTaskerInstalled()
        val pluginEnabled = CommunityPluginPrefs.isGeminiChatGptImageAutomationEnabled(this)
        if (taskerInstalled && pluginEnabled) return false

        val msg = when {
            !taskerInstalled && !pluginEnabled ->
                "AI image questions won't work until Tasker and the Gemini/ChatGPT Image Questions automation plugin are enabled."
            !taskerInstalled ->
                "AI image questions won't work until Tasker is installed and enabled."
            else ->
                "AI image questions won't work until the Gemini/ChatGPT app automation plugin for Image Questions is downloaded from Community Plugins and enabled."
        }

        AlertDialog.Builder(this)
            .setTitle("AI image setup required")
            .setMessage(msg)
            .setNegativeButton("Not now", null)
            .setPositiveButton("Open Plugins") { _, _ ->
                startActivity(Intent(this, CommunityPluginsActivity::class.java))
            }
            .show()

        return true
    }

    private fun refreshAiQueryButtonsState() {
        val unsupportedReason = imageQueryUnsupportedReasonForCurrentSelection()
        val imageSupported = unsupportedReason == null
        binding.btnTestHijackImage.isEnabled = imageSupported
        binding.btnTestHijackImage.alpha = if (imageSupported) 1f else 0.45f

        if (!imageSupported) {
            binding.btnTestHijackImage.text = "Image query unavailable"
        } else {
            binding.btnTestHijackImage.text = "Test Image AI description"
        }
    }

    private fun refreshAiModeButtons() {
        refreshAiQueryButtonsState()
    }

    private fun openSyncedMediaGallery(filter: String) {
        startActivity(Intent(this, SyncedMediaGalleryActivity::class.java).apply {
            putExtra(SyncedMediaGalleryActivity.EXTRA_MEDIA_FILTER, filter)
        })
    }

    private fun sendAiBroadcast(type: String, path: String? = null, assistantMode: String = resolveEffectiveAiAssistantMode()) {
        val intent = Intent(aiEventAction(packageName)).apply {
            putExtra("type", type)
            path?.let { putExtra("path", it) }
            putExtra("assistant", assistantMode)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        }
        sendBroadcast(intent)
        Log.i("AIHijack", "Sent Broadcast to Tasker: $type")
    }

    private fun todayDateString(tsMs: Long = System.currentTimeMillis()): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(java.util.Date(tsMs))
    }

    private fun tokenizeMemoryQuery(text: String): List<String> {
        val stopwords = setOf(
            "the", "and", "for", "with", "that", "this", "from", "into", "what", "when",
            "how", "who", "why", "are", "was", "were", "can", "could", "should", "would",
            "will", "just", "like", "your", "you", "about", "have", "has", "had", "then",
            "que", "para", "com", "uma", "nao", "não", "isso", "essa", "esse", "foi", "tem",
            "como", "porque", "por", "das", "dos", "uns", "umas"
        )

        return text
            .lowercase(Locale.US)
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .map { it.trim() }
            .filter { it.length >= 3 && it !in stopwords }
            .distinct()
    }

    private fun selectRelevantMemoryItems(items: List<String>, queryText: String, maxItems: Int): List<String> {
        val clean = items
            .map { it.trim().removePrefix("- ").removePrefix("* ").trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (clean.isEmpty()) return emptyList()
        val tokens = tokenizeMemoryQuery(queryText)
        if (tokens.isEmpty()) return clean.take(minOf(maxItems, 2))

        val scored = clean.map { item ->
            val hay = item.lowercase(Locale.US)
            var score = 0
            for (token in tokens) {
                if (hay.contains(token)) score += 1
            }
            item to score
        }

        val hits = scored
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first.length })
            .map { it.first }
            .take(maxItems)

        return if (hits.isNotEmpty()) hits else clean.take(minOf(maxItems, 2))
    }

    private fun buildCompactMemoryAwareSystemPrompt(queryText: String, date: String): String {
        val extraSections = mutableListOf<LocalAgentContextBuilder.Section>()

        val retrieval = LocalAgentMemorySearch.buildRelevantMemoryBlock(
            context = this,
            queryText = queryText,
            date = date,
            lookbackDaysFacts = 5,
            topFacts = 4,
            topSummaryLines = 3,
            maxChars = 900,
        )
        if (retrieval.isNotBlank()) {
            extraSections += LocalAgentContextBuilder.Section(
                title = "Relevant memory (search hits)",
                content = retrieval,
            )
        }

        val draftFacts = runCatching { DailyFactsStorage.load(this, date).draft }.getOrDefault(emptyList())
        val draftRef = LocalAgentMemoryStore.memoryRefForFile(
            this,
            LocalAgentMemoryStore.dailyFactsFileForDate(this, date),
        )
        val relevantDraft = if (MemoryPolicyService.isMemoryRefSearchEligible(this, draftRef)) {
            selectRelevantMemoryItems(draftFacts, queryText, maxItems = 4)
        } else {
            emptyList()
        }
        if (relevantDraft.isNotEmpty()) {
            extraSections += LocalAgentContextBuilder.Section(
                title = "Today's draft daily facts (unconfirmed)",
                content = relevantDraft.joinToString("\n") { "- $it" },
            )
        }

        val candidateFacts = runCatching { CandidateUserFactsStorage.load(this, date) }.getOrDefault(emptyList())
        val candidateRef = LocalAgentMemoryStore.memoryRefForFile(
            this,
            LocalAgentMemoryStore.userFactsCandidatesFileForDate(this, date),
        )
        val relevantCandidates = if (MemoryPolicyService.isMemoryRefSearchEligible(this, candidateRef)) {
            selectRelevantMemoryItems(candidateFacts, queryText, maxItems = 3)
        } else {
            emptyList()
        }
        if (relevantCandidates.isNotEmpty()) {
            extraSections += LocalAgentContextBuilder.Section(
                title = "Candidate user facts (pending review)",
                content = relevantCandidates.joinToString("\n") { "- $it" },
            )
        }

        val builder = LocalAgentContextBuilder(
            maxAgentPersonaChars = QUERY_MAX_AGENT_PERSONA_CHARS,
            maxUserFactsChars = QUERY_MAX_USER_FACTS_CHARS,
            maxConfirmedDailyFactsChars = QUERY_MAX_CONFIRMED_FACTS_CHARS,
            maxDailySummaryChars = QUERY_MAX_DAILY_SUMMARY_CHARS,
            maxTotalChars = QUERY_MAX_TOTAL_CONTEXT_CHARS,
        )

        return builder.buildSystemMessage(
            context = this,
            date = date,
            extraSections = extraSections,
        )
    }

    private suspend fun runMemoryAwareChosenProviderQuery(
        userPrompt: String,
        providerType: AgentProviderType,
        imagePaths: List<String> = emptyList(),
        audioPath: String? = null,
    ): String {
        val date = todayDateString()
        val systemPrompt = buildCompactMemoryAwareSystemPrompt(queryText = userPrompt, date = date)

        val messages = listOf(
            mapOf("role" to "System", "content" to systemPrompt),
            mapOf("role" to "User", "content" to userPrompt),
        )

        return when (providerType) {
            AgentProviderType.PRO_SUBSCRIPTION -> {
                CliRelayClient.chat(
                    context = this,
                    chatId = "glasses_${System.currentTimeMillis()}",
                    prompt = userPrompt,
                    messages = messages,
                    modelOverride = ProSubscriptionAiPrefs.getRequestsModel(this),
                ).getOrElse {
                    "Pro endpoint error: ${it.message ?: "unknown error"}"
                }
            }

            AgentProviderType.LOCAL_AGENT ->
                runCatching {
                    val modelIssue = validateSelectedGemmaForChosenProvider(imageRequested = imagePaths.isNotEmpty())
                    if (modelIssue != null) {
                        return@runCatching modelIssue
                    }
                    LocalModelsProvider().streamChat(
                        context = this,
                        messages = messages,
                        imagePaths = imagePaths,
                        audioPath = audioPath,
                    )
                }.getOrElse {
                    "Local Models error: ${it.message ?: "unknown error"}"
                }

            AgentProviderType.TASKER -> {
                CliRelayClient.chat(
                    context = this,
                    chatId = "glasses_${System.currentTimeMillis()}",
                    prompt = userPrompt,
                    messages = messages,
                ).getOrElse { "Endpoint unavailable: ${it.message ?: "unknown error"}" }
            }
        }.trim()
    }

    private fun validateSelectedGemmaForChosenProvider(imageRequested: Boolean): String? {
        val selected = LocalModelStorageRepository.resolveSelectedModel(this)
            ?: return "No local model selected. Install/select Gemma 4 LiteRT in Settings."
        val settings = LocalModelSettingsRepository.getForModel(this, selected.id)
        if (settings.modelRuntime != LocalModelRuntime.LITERT) {
            return "Selected local model runtime is not LiteRT. Switch runtime to LiteRT for Gemma 4 flows."
        }

        val modelHint = "${selected.displayName} ${selected.catalogId.orEmpty()} ${selected.fileName}".lowercase(Locale.US)
        if (!modelHint.contains("gemma")) {
            return "Selected local model is not Gemma. Please select a Gemma 4 LiteRT model."
        }

        if (imageRequested && !modelHint.contains("gemma-4") && !modelHint.contains("gemma4")) {
            return "Image questions on glasses are configured for Gemma 4 LiteRT. Please select Gemma 4 E2B/E4B."
        }
        return null
    }

    private fun triggerMemoryAwareImageQuery(
        imagePath: String,
        providerType: AgentProviderType,
        userQuestion: String?,
    ) {
        Log.i("AIHijack", "Running memory-aware image query for chosen provider $providerType: $imagePath")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val finalReply = when (providerType) {
                    AgentProviderType.PRO_SUBSCRIPTION -> {
                        val visionResult = CliRelayClient.imageQuery(
                            context = this@MainActivity,
                            imagePath = imagePath,
                            modelOverride = ProSubscriptionAiPrefs.getQuestionsModel(this@MainActivity),
                        )

                        if (visionResult.isFailure) {
                            val errorMsg = visionResult.exceptionOrNull()?.message ?: "unknown error"
                            Log.e("AIHijack", "Image query failed: $errorMsg")
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Vision error: ${errorMsg.take(80)}", Toast.LENGTH_LONG).show()
                            }
                            "I couldn't analyze the image. Please try again."
                        } else {
                            val visionReply = visionResult.getOrNull()?.trim() ?: ""
                            if (visionReply.isBlank()) {
                                "I couldn't analyze that image right now. Please try again."
                            } else if (looksLikeVisionFailed(visionReply)) {
                                Log.w("AIHijack", "Vision relay couldn't process image. Reply: ${visionReply.take(100)}")
                                runOnUiThread {
                                    Toast.makeText(this@MainActivity, "Vision model couldn't process image", Toast.LENGTH_LONG).show()
                                }
                                "I couldn't analyze the image. Please try again."
                            } else {
                                val leadPrompt = userQuestion?.trim().takeUnless { it.isNullOrBlank() }
                                    ?: "Describe and translate to English the following picture if it isn't in English."
                                val followUpPrompt = buildString {
                                    appendLine(leadPrompt)
                                    appendLine("Use this vision observation:")
                                    appendLine(visionReply.take(1400))
                                    appendLine()
                                    appendLine("Keep the final answer concise (1-3 short sentences).")
                                }
                                runMemoryAwareChosenProviderQuery(
                                    userPrompt = followUpPrompt,
                                    providerType = AgentProviderType.PRO_SUBSCRIPTION,
                                )
                                null // Don't speak here - follow-up query will handle it
                            }
                        }
                    }

                    AgentProviderType.LOCAL_AGENT -> {
                        val multimodalPrompt = userQuestion?.trim().takeUnless { it.isNullOrBlank() }
                            ?: "Describe this image clearly, and translate any visible non-English text to English. Keep it concise."
                        runMemoryAwareChosenProviderQuery(
                            userPrompt = multimodalPrompt,
                            providerType = AgentProviderType.LOCAL_AGENT,
                            imagePaths = listOf(imagePath),
                        )
                    }

                    AgentProviderType.TASKER -> {
                        val visionResult = CliRelayClient.imageQuery(this@MainActivity, imagePath)
                        if (visionResult.isFailure) {
                            val errorMsg = visionResult.exceptionOrNull()?.message ?: "unknown error"
                            Log.e("AIHijack", "Image query failed: $errorMsg")
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Vision error: ${errorMsg.take(80)}", Toast.LENGTH_LONG).show()
                            }
                            "I couldn't analyze the image. Please try again."
                        } else {
                            val visionReply = visionResult.getOrNull()?.trim() ?: ""
                            if (visionReply.isBlank()) {
                                "I couldn't analyze that image right now. Please try again."
                            } else {
                                visionReply
                            }
                        }
                    }
                }

                if (finalReply != null) {
                    runOnUiThread {
                        speak(finalReply)
                    }
                }
            } finally {
                imageQueryInProgress.set(false)
            }
        }
    }

    private fun triggerCliRelayImageCaptureAndQuery() {
        handleGlassesImageButtonPressed(triggerCapture = true, sourceTag = "test_button")
    }

    private fun handleGlassesImageButtonPressed(triggerCapture: Boolean, sourceTag: String) {
        if (!BleOperateManager.getInstance().isConnected) {
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "Glasses are not connected. Connect first to use image query.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
            return
        }

        if (triggerCapture) {
            Toast.makeText(this, "Triggering glasses camera…", Toast.LENGTH_SHORT).show()
        }

        val outDir = getExternalFilesDir("DCIM") ?: filesDir
        val fileName = "AI_Thumb_${sourceTag}_${System.currentTimeMillis()}.jpg"
        val file = File(outDir, fileName)
        runCatching {
            file.parentFile?.mkdirs()
            if (file.exists()) file.delete()
        }

        val gotChunk = java.util.concurrent.atomic.AtomicBoolean(false)
        val completed = java.util.concurrent.atomic.AtomicBoolean(false)
        val imageProcessed = java.util.concurrent.atomic.AtomicBoolean(false)

        val thumbCallback: (Int, Boolean, ByteArray?) -> Unit = { _, isComplete, data ->
            if (data != null && data.isNotEmpty()) {
                gotChunk.set(true)
                runCatching {
                    FileOutputStream(file, true).use { it.write(data) }
                }.onFailure {
                    Log.e("AIHijack", "Failed to write thumbnail chunk: ${it.message}")
                }
            }

            if (isComplete && completed.compareAndSet(false, true)) {
                Log.i("AIHijack", "[$sourceTag] Thumbnail transfer complete: ${file.absolutePath} (${file.length()} bytes)")
                if (imageProcessed.compareAndSet(false, true)) {
                    onImageThumbnailReadyForQuestion(file.absolutePath)
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            if (triggerCapture) {
                runCatching {
                    LargeDataHandler.getInstance().glassesControl(
                        byteArrayOf(0x02, 0x01, 0x06, 0x02, 0x02)
                    ) { _, _ -> }
                }
                delay(250)
                LargeDataHandler.getInstance().glassesControl(byteArrayOf(0x02, 0x01, 0x01)) { _, _ -> }
                delay(3000)
            }

            LargeDataHandler.getInstance().getPictureThumbnails(thumbCallback)

            // Wait for BLE transfer to complete. Total: 5s + 8s = 13s.
            delay(5000)
            if (!gotChunk.get() && !completed.get()) {
                Log.w("AIHijack", "[$sourceTag] No thumbnail chunks yet; retrying getPictureThumbnails()…")
                LargeDataHandler.getInstance().getPictureThumbnails(thumbCallback)
            }

            delay(8000)
            if (!completed.get() && imageProcessed.compareAndSet(false, true)) {
                Log.w("AIHijack", "[$sourceTag] BLE thumbnail timed out, falling back to latest image")
                useLatestImageFallback(sourceTag)
            }
        }
    }

    /**
     * Use the most recent Glasses_AI_*.jpg already on the phone.
     */
    private suspend fun useLatestImageFallback(sourceTag: String) {
        val fallbackImage = findLatestGlassesAiImage()
        if (fallbackImage != null) {
            val fallbackFile = File(fallbackImage)
            val ageMs = System.currentTimeMillis() - fallbackFile.lastModified()
            if (ageMs > IMAGE_FALLBACK_MAX_AGE_MS || ageMs < 0) {
                Log.w("AIHijack", "[$sourceTag] Image too old: age=${ageMs / 1000}s")
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Last image is ${ageMs / 60000} min old — too old to use.",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            } else {
                Log.i("AIHijack", "[$sourceTag] Using latest captured image (age=${ageMs / 1000}s)")
                onImageThumbnailReadyForQuestion(fallbackImage)
            }
        } else {
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "No image found. Take a photo with the glasses first.",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    private fun onImageThumbnailReadyForQuestion(imagePath: String) {
        val imageFile = File(imagePath)
        if (!imageFile.exists() || imageFile.length() < 1000) {
            Log.e("AIHijack", "Image file missing or too small: $imagePath (${imageFile.length()} bytes)")
            runOnUiThread {
                Toast.makeText(this, "Image transfer incomplete. Please try again.", Toast.LENGTH_LONG).show()
            }
            return
        }

        val ageMs = System.currentTimeMillis() - imageFile.lastModified()
        if (ageMs > IMAGE_FALLBACK_MAX_AGE_MS || ageMs < 0) {
            Log.w("AIHijack", "Thumbnail too old: age=${ageMs / 1000}s, path=$imagePath")
            runOnUiThread {
                Toast.makeText(
                    this,
                    "Thumbnail is ${ageMs / 60000} min old — too old to use.",
                    Toast.LENGTH_LONG,
                ).show()
            }
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val publicPath = copyImageToPublicCamera(imagePath)

            Log.i("AIHijack", "Image ready for AI query: $imagePath (size=${imageFile.length()} bytes, age=${ageMs / 1000}s)")

            // Process the image query first (model inference + TTS reply).
            // triggerAssistantImageQuery launches a background coroutine and returns immediately,
            // so we must wait for TTS to finish before opening the follow-up voice window.
            triggerAssistantImageQuery(imagePath, userQuestion = null)

            // Wait for the model's TTS reply to finish (polls tts?.isSpeaking every 500ms).
            waitForTtsToFinish(timeoutMs = 90_000L)

            // Brief pause after TTS so the user knows it's their turn.
            delay(500)

            // Now open the voice window for a follow-up question.
            withContext(Dispatchers.Main) {
                val spokenQuestion = captureOptionalImageQuestionFromBluetoothMic(timeoutMs = 3_000L)
                if (!spokenQuestion.isNullOrBlank()) {
                    triggerAssistantImageQuery(imagePath, spokenQuestion)
                }
            }
        }
    }

    private suspend fun waitForTtsToFinish(timeoutMs: Long) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var warned = false
        while (isTtsSpeaking() && System.currentTimeMillis() < deadline) {
            if (!warned) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Replying…", Toast.LENGTH_SHORT).show()
                }
                warned = true
            }
            delay(500)
        }
        if (isTtsSpeaking()) {
            Log.w("AIHijack", "TTS still speaking after ${timeoutMs}ms, proceeding anyway")
        }
    }

    private fun isTtsSpeaking(): Boolean {
        return tts?.isSpeaking == true
    }

    /**
     * Copy an image file to DCIM/Camera/ with the Glasses_AI_ naming convention.
     * Returns the public file path on success, null on failure.
     */
    private fun copyImageToPublicCamera(sourcePath: String): String? {
        val source = File(sourcePath)
        if (!source.exists() || source.length() == 0L) {
            Log.w("AIHijack", "Source image missing or empty: $sourcePath")
            return null
        }
        return try {
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val cameraDir = File(publicDir, "Camera")
            if (!cameraDir.exists()) cameraDir.mkdirs()
            val publicFile = File(cameraDir, "Glasses_AI_${System.currentTimeMillis()}.jpg")
            source.copyTo(publicFile, overwrite = true)
            // Scan so MediaStore / Tasker file picker can see it immediately
            MediaScannerConnection.scanFile(this, arrayOf(publicFile.absolutePath), arrayOf("image/jpeg")) { _, _ ->
                Log.i("AIHijack", "Scanned to gallery: ${publicFile.absolutePath} (${publicFile.length()} bytes)")
            }
            Log.i("AIHijack", "Copied thumbnail to public: ${publicFile.absolutePath}")
            publicFile.absolutePath
        } catch (e: Exception) {
            Log.e("AIHijack", "Failed to copy image to public DCIM: ${e.message}")
            null
        }
    }

    /** Find the most recent Glasses_AI_*.jpg in DCIM/Camera/. */
    private fun findLatestGlassesAiImage(): String? {
        return try {
            val cameraDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "Camera"
            )
            if (!cameraDir.isDirectory) return null
            cameraDir.listFiles { f ->
                f.isFile && f.name.startsWith("Glasses_AI_") && f.name.endsWith(".jpg", ignoreCase = true)
            }
                ?.filter { it.length() > 0 }
                ?.maxByOrNull { it.lastModified() }
                ?.absolutePath
        } catch (_: Exception) { null }
    }

    /** Detect when the vision model couldn't actually see the image (server-side issue). */
    private fun looksLikeVisionFailed(reply: String): Boolean {
        val lower = reply.lowercase()
        return lower.contains("upload") && lower.contains("image") ||
            lower.contains("please provide the image") ||
            lower.contains("i can't see") ||
            lower.contains("no image") && lower.contains("provided") ||
            lower.contains("attach") && lower.contains("image") ||
            lower.contains("invalid") && lower.contains("image") ||
            lower.contains("does not represent a valid image") ||
            lower.contains("image data") && lower.contains("invalid") ||
            lower.contains("vision") && lower.contains("failed") ||
            lower.contains("couldn't analyze") ||
            lower.contains("openrouter_image_failed")
    }

    private suspend fun captureOptionalImageQuestionFromBluetoothMic(timeoutMs: Long): String? {
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                var recognizer: SpeechRecognizer? = null
                var timeoutJob: Job? = null
                var finished = false
                var heardSpeech = false

                fun cleanup() {
                    runCatching {
                        recognizer?.destroy()
                    }
                    recognizer = null

                    runCatching {
                        audioManager.isBluetoothScoOn = false
                        audioManager.stopBluetoothSco()
                        audioManager.mode = android.media.AudioManager.MODE_NORMAL
                    }
                }

                fun finish(result: String?) {
                    if (finished) return
                    finished = true
                    timeoutJob?.cancel()
                    timeoutJob = null
                    val cleaned = result?.trim()?.takeIf { it.isNotBlank() }

                    runCatching {
                        val tone = android.media.ToneGenerator(android.media.AudioManager.STREAM_VOICE_CALL, 90)
                        tone.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 170)
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(190)
                            runCatching { tone.release() }
                            cleanup()
                            if (cont.isActive) {
                                cont.resume(cleaned)
                            }
                        }
                    }.onFailure {
                        cleanup()
                        if (cont.isActive) {
                            cont.resume(cleaned)
                        }
                    }
                }

                runCatching {
                    audioManager.mode = android.media.AudioManager.MODE_IN_COMMUNICATION
                    audioManager.startBluetoothSco()
                    audioManager.isBluetoothScoOn = true
                }

                runCatching {
                    val tone = android.media.ToneGenerator(android.media.AudioManager.STREAM_VOICE_CALL, 90)
                    tone.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 180)
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(250)
                        runCatching { tone.release() }
                    }
                }

                recognizer = SpeechRecognizer.createSpeechRecognizer(this@MainActivity)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
                }

                recognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {
                        heardSpeech = true
                        timeoutJob?.cancel()
                        timeoutJob = null
                    }
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        Log.i("AIHijack", "Image question listener ended with error code=$error")
                        finish(null)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        finish(matches?.firstOrNull())
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                timeoutJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(timeoutMs)
                    if (!heardSpeech) {
                        finish(null)
                    }
                }

                recognizer?.startListening(intent)

                cont.invokeOnCancellation {
                    finish(null)
                }
            }
        }
    }

    private fun triggerCliRelayVoiceQuery(
        memoryAwareChosenProvider: Boolean = false,
        chosenProviderType: AgentProviderType? = null,
    ) {
        // Wake up screen if locked
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }

        // Tell glasses to stop proprietary AI audio stream
        LargeDataHandler.getInstance().glassesControl(byteArrayOf(0x02, 0x01, 0x0b)) { _, _ -> }

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager

        fun stopSco() {
            runCatching {
                audioManager.isBluetoothScoOn = false
                audioManager.stopBluetoothSco()
                audioManager.mode = android.media.AudioManager.MODE_NORMAL
            }
        }

        runCatching {
            audioManager.mode = android.media.AudioManager.MODE_IN_COMMUNICATION
            audioManager.startBluetoothSco()
            audioManager.isBluetoothScoOn = true
        }

        Toast.makeText(this, "Listening for voice query…", Toast.LENGTH_SHORT).show()

        val recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                speak("I am listening")
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                Toast.makeText(this@MainActivity, "Voice query failed: $error", Toast.LENGTH_SHORT).show()
                recognizer.destroy()
                stopSco()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val prompt = matches?.firstOrNull()?.trim().orEmpty()

                if (prompt.isBlank()) {
                    recognizer.destroy()
                    stopSco()
                    return
                }

                Toast.makeText(this@MainActivity, "Asking: $prompt", Toast.LENGTH_SHORT).show()

                CoroutineScope(Dispatchers.IO).launch {
                    val reply = if (memoryAwareChosenProvider) {
                        runMemoryAwareChosenProviderQuery(
                            userPrompt = prompt,
                            providerType = chosenProviderType ?: AgentProviderType.PRO_SUBSCRIPTION,
                        )
                    } else {
                        val selectedProvider = AutomationPrefs.getProviderType(this@MainActivity)
                        val modelOverride = if (selectedProvider == AgentProviderType.PRO_SUBSCRIPTION) {
                            ProSubscriptionAiPrefs.getQuestionsModel(this@MainActivity)
                        } else {
                            null
                        }

                        CliRelayClient.voiceQuery(
                            context = this@MainActivity,
                            prompt = prompt,
                            modelOverride = modelOverride,
                        )
                            .getOrElse { "Relay unavailable: ${it.message ?: "unknown error"}" }
                    }

                    runOnUiThread {
                        speak(reply, utteranceId = "AI_REPLY") {
                            stopSco()
                        }
                    }
                }

                recognizer.destroy()
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)
    }

    private fun triggerAssistantVoiceQuery() {
        val effectiveMode = resolveEffectiveAiAssistantMode()
        Log.i("AIHijack", "Triggering Voice Query for $effectiveMode")

        val selectedProvider = AutomationPrefs.getProviderType(this)
        val useChosenProviderMemoryAware =
            aiAssistantMode == AI_MODE_CHOSEN_PROVIDER &&
                (selectedProvider == AgentProviderType.PRO_SUBSCRIPTION ||
                    selectedProvider == AgentProviderType.LOCAL_AGENT)
        if (useChosenProviderMemoryAware) {
            triggerCliRelayVoiceQuery(
                memoryAwareChosenProvider = true,
                chosenProviderType = selectedProvider,
            )
            return
        }

        // Spike branch feature: CLI Relay AI provider (hosted Gemini/Codex via HTTP).
        // Only route through CLI relay when NOT in Gemini/ChatGPT mode (those use native apps).
        if (effectiveMode != AI_MODE_GEMINI && effectiveMode != AI_MODE_CHATGPT) {
            val relayProvider = AiProviderPrefs.getProvider(this)
            if (relayProvider == RelayProviderType.CLI_RELAY) {
                triggerCliRelayVoiceQuery()
                return
            }
        }

        if (effectiveMode == AI_MODE_TASKER) {
            sendAiBroadcast(type = "voice", assistantMode = AI_MODE_TASKER)
            return
        }

        // Wake up screen if locked

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }

        // Tell glasses to stop proprietary AI audio stream
        LargeDataHandler.getInstance().glassesControl(byteArrayOf(0x02, 0x01, 0x0b)) { _, _ -> }

        try {
            val intent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (effectiveMode == AI_MODE_CHATGPT) {
                    setPackage("com.openai.chatgpt")
                }
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("AIHijack", "Failed to trigger assistant: ${e.message}")
            runOnUiThread {
                Toast.makeText(this, "Assistant not found or failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun triggerAssistantImageQuery(imagePath: String, userQuestion: String? = null) {
        // Debounce: prevent duplicate requests within 5 seconds
        val now = System.currentTimeMillis()
        if (now - lastImageQueryAtMs < 5000) {
            Log.w("AIHijack", "Image query debounced (last was ${now - lastImageQueryAtMs}ms ago)")
            return
        }
        
        // Guard against concurrent requests
        if (!imageQueryInProgress.compareAndSet(false, true)) {
            Log.w("AIHijack", "Image query already in progress, skipping")
            return
        }
        
        lastImageQueryAtMs = now
        
        val selectedProvider = AutomationPrefs.getProviderType(this)
        val isChosenProviderMode = aiAssistantMode == AI_MODE_CHOSEN_PROVIDER

        // Route ChosenProvider with memory-aware providers
        val useChosenProviderMemoryAware =
            isChosenProviderMode &&
                (selectedProvider == AgentProviderType.PRO_SUBSCRIPTION ||
                    selectedProvider == AgentProviderType.LOCAL_AGENT)
        if (useChosenProviderMemoryAware) {
            triggerMemoryAwareImageQuery(imagePath, selectedProvider, userQuestion)
            return
        }

        val relayProvider = AiProviderPrefs.getProvider(this)
        if (relayProvider == RelayProviderType.CLI_RELAY) {
            Log.i("AIHijack", "Sending image query to CLI relay: $imagePath")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val modelOverride = if (AutomationPrefs.getProviderType(this@MainActivity) == AgentProviderType.PRO_SUBSCRIPTION) {
                        ProSubscriptionAiPrefs.getQuestionsModel(this@MainActivity)
                    } else {
                        null
                    }

                    val result = CliRelayClient.imageQuery(
                        context = this@MainActivity,
                        imagePath = imagePath,
                        modelOverride = modelOverride,
                    )

                    runOnUiThread {
                        if (result.isFailure) {
                            val errorMsg = result.exceptionOrNull()?.message ?: "unknown error"
                            Log.e("AIHijack", "Image query failed: $errorMsg")
                            Toast.makeText(this@MainActivity, "Vision error: ${errorMsg.take(80)}", Toast.LENGTH_LONG).show()
                            speak("I couldn't analyze the image. Please try again.")
                        } else {
                            val reply = result.getOrNull() ?: ""
                            if (looksLikeVisionFailed(reply)) {
                                Toast.makeText(this@MainActivity, "Vision model couldn't process image", Toast.LENGTH_LONG).show()
                                speak("I couldn't analyze the image. Please try again.")
                            } else {
                                speak(reply)
                            }
                        }
                    }
                } finally {
                    imageQueryInProgress.set(false)
                }
            }
            return
        }

        Log.i("AIHijack", "Redirecting Image Query to Tasker logic with $imagePath")

        try {
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            val isLocked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                keyguardManager.isDeviceLocked
            } else {
                keyguardManager.isKeyguardLocked
            }

            // Wake and dismiss keyguard only when needed
            if (isLocked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
                keyguardManager.requestDismissKeyguard(this, null)
            }

            if (isLocked) {
                speak("Unlock your phone to answer the image query")
            }

            // Stop glasses AI mode
            LargeDataHandler.getInstance().glassesControl(byteArrayOf(0x02, 0x01, 0x0b)) { _, _ -> }

            val file = File(imagePath)
            if (!file.exists()) {
                Log.e("AIHijack", "Image file does not exist: $imagePath")
                return
            }

            // Copy file to public DCIM folder so it shows up in Gallery/Recents
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val cameraDir = File(publicDir, "Camera")
            if (!cameraDir.exists()) cameraDir.mkdirs()
            
            val publicFile = File(cameraDir, "Glasses_AI_${System.currentTimeMillis()}.jpg")
            file.copyTo(publicFile, overwrite = true)
            
            // Scan the file so MediaStore/Gallery sees it immediately
            MediaScannerConnection.scanFile(this, arrayOf(publicFile.absolutePath), arrayOf("image/jpeg")) { path, uri ->
                Log.i("AIHijack", "Scanned to Gallery: $path")
                // Once scanned, trigger the Tasker broadcast
                runOnUiThread {
                    sendAiBroadcast("image", path)
                }
            }
        } catch (e: Exception) {
            Log.e("AIHijack", "Failed to process image for Tasker: ${e.message}")
        } finally {
            imageQueryInProgress.set(false)
        }
    }


    private fun updateConnectionStatus(connected: Boolean) {
        val status = if (connected) {
            "Smart Glass connected"
        } else {
            "Disconnected"
        }
        binding.statusText.text = status
        updateDeviceClassText()
        if (!connected) {
            updateBatteryText(null)
        }
    }

    private fun updateDeviceClassText() {
        val profile = DeviceProfileStore.loadLastSelected(this)
        if (BleOperateManager.getInstance().isConnected) {
            binding.tvDeviceClass.visibility = View.GONE
        } else {
            binding.tvDeviceClass.visibility = View.VISIBLE
            val classLabel = profile?.selectedClass?.displayName() ?: "Unknown"
            binding.tvDeviceClass.text = "Class: $classLabel"
        }

        applyGlassesManagerGating(profile)
    }

    /**
     * Chapter 4: Capability gating for the Glasses Manager screen.
     *
     * - HEY_CYAN: show extra controls + battery/storage placeholders.
     * - Other classes: show meeting capture only (plus basic connection UI).
     */
    private fun applyGlassesManagerGating(profile: com.fersaiyan.cyanbridge.devices.DeviceProfile?) {
        val model = GlassesManagerGating.uiModel(profile)

        // Expanded controls panel (HeyCyan-only in MVP baseline)
        binding.layoutHeycyanExtras.visibility =
            if (model.isVisible(GlassesManagerGating.Action.HEY_CYAN_EXTRAS)) android.view.View.VISIBLE else android.view.View.GONE

        // Status placeholders
        val showBattery = model.isVisible(GlassesManagerGating.Action.STATUS_BATTERY)
        val showStorage = model.isVisible(GlassesManagerGating.Action.STATUS_STORAGE)

        binding.layoutBattery.visibility = if (showBattery) android.view.View.VISIBLE else android.view.View.GONE
        binding.layoutStorage.visibility = if (showStorage) android.view.View.VISIBLE else android.view.View.GONE
        binding.layoutStatusMetrics.visibility =
            if (showBattery || showStorage) android.view.View.VISIBLE else android.view.View.GONE

        // Only poll battery for profiles that claim to support it.
        if (showBattery) {
            startBatteryPolling()
        } else {
            stopBatteryPolling()
            updateBatteryText(null)
        }

        if (!showStorage) {
            binding.storageText.text = "--"
        }
    }

    // --- Chapter 5: Meeting capture pipeline (start/stop, timer, indicator) ---

    private fun setupMeetingCaptureUi() {
        val labels = meetingTimerOptions.map { it.second }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerMeetingTimer.adapter = adapter
        syncMeetingCaptureUiFromPrefs()
    }

    private fun setupAgentControlsUi() {
        binding.btnAgentStart.setOnClickListener {
            val res = LocalAgentController.start(this)
            if (res.ok) {
                LocalAgentPrefs.setStatus(this, "Starting…")
                LocalAgentPrefs.clearLastError(this)
            } else {
                LocalAgentPrefs.setStatus(this, "Error")
                LocalAgentPrefs.setLastError(this, res.error ?: res.userMessage)
            }
            refreshAgentStatusUi()
            Toast.makeText(this, res.userMessage, Toast.LENGTH_SHORT).show()
            LocalAgentController.requestStatus(this)
        }

        binding.btnAgentStop.setOnClickListener {
            val res = LocalAgentController.stop(this)
            if (res.ok) {
                LocalAgentPrefs.setStatus(this, "Stopping…")
                LocalAgentPrefs.clearLastError(this)
            } else {
                LocalAgentPrefs.setStatus(this, "Error")
                LocalAgentPrefs.setLastError(this, res.error ?: res.userMessage)
            }
            refreshAgentStatusUi()
            Toast.makeText(this, res.userMessage, Toast.LENGTH_SHORT).show()
            LocalAgentController.requestStatus(this)
        }

        binding.btnAgentDemo.setOnClickListener {
            Toast.makeText(
                this,
                "Demo: I will read the screen content through your glasses in 5 seconds…",
                Toast.LENGTH_LONG
            ).show()

            val res = LocalAgentController.demo(this)
            if (res.ok) {
                LocalAgentPrefs.setStatus(this, "Running demo…")
                LocalAgentPrefs.clearLastError(this)
            } else {
                LocalAgentPrefs.setStatus(this, "Error")
                LocalAgentPrefs.setLastError(this, res.error ?: res.userMessage)
            }
            refreshAgentStatusUi()
            Toast.makeText(this, res.userMessage, Toast.LENGTH_SHORT).show()
            LocalAgentController.requestStatus(this)
        }

        refreshAgentStatusUi()
    }

    private fun refreshAgentStatusUi() {
        binding.tvAgentStatus.text = "Status: ${LocalAgentPrefs.getStatus(this)}"
        binding.tvAgentLastError.text = "Last error: ${LocalAgentPrefs.getLastError(this)}"
    }

    private fun selectedMeetingTimerDurationSec(): Long? { 
        val idx = binding.spinnerMeetingTimer.selectedItemPosition
        return meetingTimerOptions.getOrNull(idx)?.first
    }

    private fun requestMeetingCapturePermissions(onGranted: () -> Unit) {
        val perms = mutableListOf<String>(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        XXPermissions.with(this)
            .permission(perms)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) onGranted() else {
                        Toast.makeText(this@MainActivity, "Missing permissions for recording", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    super.onDenied(permissions, never)
                    Toast.makeText(this@MainActivity, "Recording permission denied", Toast.LENGTH_SHORT).show()
                    if (never) {
                        XXPermissions.startPermissionActivity(this@MainActivity, permissions)
                    }
                }
            })
    }

    private fun startMeetingCaptureFromUi() {
        requestMeetingCapturePermissions {
            val deviceClass = DeviceProfileStore.loadLastSelected(this)?.selectedClass?.name ?: "UNKNOWN"
            val durationSec = selectedMeetingTimerDurationSec()

            // Optimistic UI so user instantly sees a recording indicator.
            setRecordingUi(isRecording = true, source = null)
            binding.tvMeetingBanner.text = "Starting recording…"

            MeetingCaptureService.start(this, timerDurationSec = durationSec, deviceClass = deviceClass)
        }
    }

    private fun stopMeetingCaptureFromUi() {
        binding.tvMeetingBanner.text = "Stopping…"
        MeetingCaptureService.stop(this)
    }

    private fun registerMeetingCaptureReceiver() {
        if (meetingCaptureStateReceiver != null) return

        meetingCaptureStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != MeetingCaptureService.ACTION_STATE) return

                val isRecording = intent.getBooleanExtra(MeetingCaptureService.EXTRA_IS_RECORDING, false)
                val source = intent.getStringExtra(MeetingCaptureService.EXTRA_SOURCE)?.let {
                    runCatching { CaptureSource.valueOf(it) }.getOrNull()
                }
                val stopReason = intent.getStringExtra(MeetingCaptureService.EXTRA_STOP_REASON)
                val error = intent.getStringExtra(MeetingCaptureService.EXTRA_ERROR)

                setRecordingUi(isRecording = isRecording, source = source)

                if (!isRecording && stopReason == "timer") {
                    Toast.makeText(this@MainActivity, "Meeting capture auto-stopped (timer)", Toast.LENGTH_SHORT).show()
                }
                if (!error.isNullOrBlank()) {
                    Toast.makeText(this@MainActivity, "Recording error: $error", Toast.LENGTH_LONG).show()
                }
            }
        }

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(meetingCaptureStateReceiver!!, IntentFilter(MeetingCaptureService.ACTION_STATE))
    }

    private fun unregisterMeetingCaptureReceiver() {
        val r = meetingCaptureStateReceiver ?: return
        meetingCaptureStateReceiver = null
        runCatching {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(r)
        }
    }

    private fun syncMeetingCaptureUiFromPrefs() {
        val state = MeetingCapturePrefs.getState(this)
        setRecordingUi(isRecording = state.isRecording, source = state.source)
    }

    private fun setRecordingUi(isRecording: Boolean, source: CaptureSource?) {
        binding.btnMeetingStart.isEnabled = !isRecording
        binding.btnMeetingStop.isEnabled = isRecording
        binding.btnMeetingBannerStop.isEnabled = isRecording

        if (isRecording) {
            binding.meetingRecordingBanner.visibility = android.view.View.VISIBLE
            val src = when (source) {
                CaptureSource.BLUETOOTH_MIC -> "Bluetooth mic"
                CaptureSource.PHONE_MIC -> "Phone mic"
                null -> "(detecting…)"
            }
            binding.tvMeetingBanner.text = "Recording active · $src"
            binding.tvMeetingSource.text = "Source: $src"
        } else {
            binding.meetingRecordingBanner.visibility = android.view.View.GONE
            binding.tvMeetingSource.text = "Source: (not recording)"
        }
    }

    // --- end Chapter 5 meeting capture ---

    // --- Transcription UI moved to RecordingsListActivity (per-item) ---



    private fun updateBatteryText(battery: Int?) {
        binding.batteryText.text = battery?.let { "$it%" } ?: "--%"
    }

    private fun requestBatteryStatus(showToast: Boolean) {
        if (showToast) {
            pendingBatteryToast = true
            Toast.makeText(this@MainActivity, "Requesting battery level…", Toast.LENGTH_SHORT).show()
        }
        ensureBatteryCallback()
        // Trigger battery sync
        recordTechnicalSyncRow(
            action = "Read battery",
            uuid = bleWriteUuidLabel(),
            command = "SDK syncBattery",
            response = "battery request sent",
            status = "Pending callback",
        )
        LargeDataHandler.getInstance().syncBattery()
    }

    private fun ensureBatteryCallback() {
        if (batteryCallbackRegistered) {
            return
        }
        batteryCallbackRegistered = true
        // Add battery listener. According to the SDK docs this
        // callback is invoked when syncBattery completes.
        LargeDataHandler.getInstance().addBatteryCallBack("init") { _, response ->
            val result = parseBatteryResponse(response)
            Log.i("BatteryCallback", result.message)
            recordTechnicalSyncRow(
                action = "Read battery",
                uuid = bleWriteUuidLabel(),
                command = "SDK syncBattery",
                response = result.message,
                status = if (result.battery != null) "Confirmed" else "Failed",
            )
            runOnUiThread {
                updateBatteryText(result.battery)
                if (pendingBatteryToast) {
                    Toast.makeText(
                        this@MainActivity,
                        result.message,
                        Toast.LENGTH_LONG
                    ).show()
                    pendingBatteryToast = false
                }
            }
        }
    }

    private data class BatteryResult(
        val battery: Int?,
        val charging: Boolean?,
        val message: String
    )

    private fun parseBatteryResponse(response: Any?): BatteryResult {
        if (response == null) {
            return BatteryResult(null, null, "Battery callback: null response")
        }
        return try {
            val clazz = response.javaClass
            val batteryField = clazz.getDeclaredField("battery").apply {
                isAccessible = true
            }
            val chargingField = clazz.getDeclaredField("charging").apply {
                isAccessible = true
            }

            val battery = batteryField.getInt(response)
            val charging = chargingField.getBoolean(response)
            val message =
                "Battery: $battery% (${if (charging) "charging" else "not charging"})"
            BatteryResult(battery, charging, message)
        } catch (e: Exception) {
            Log.e("BatteryCallback", "Failed to parse BatteryResponse", e)
            BatteryResult(null, null, "Battery: $response")
        }
    }

    private fun handleBatteryReport(battery: Int, charging: Boolean) {
        val message = "Battery: $battery% (${if (charging) "charging" else "not charging"})"
        Log.i("BatteryCallback", message)
        recordTechnicalSyncRow(
            action = "Read battery",
            uuid = bleNotifyUuidLabel(),
            command = "N/A",
            response = message,
            status = "Confirmed",
        )
        runOnUiThread {
            updateBatteryText(battery)
            if (pendingBatteryToast) {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                pendingBatteryToast = false
            }
        }
    }

    private fun handleTaskerCommand(startIntent: Intent?) {
        if (startIntent == null) return

        val isFromTaskerAction = startIntent.action == actionTaskerCommand(packageName)
        val command = startIntent.getStringExtra(EXTRA_TASKER_COMMAND)

        if (!isFromTaskerAction && command.isNullOrBlank()) {
            return
        }

        val normalizedCommand = command?.lowercase() ?: return

        when (normalizedCommand) {
            "scan" -> binding.btnScan.performClick()
            "connect" -> binding.btnConnect.performClick()
            "disconnect" -> binding.btnDisconnect.performClick()
            "add_listener" -> binding.btnAddListener.performClick()
            "set_time" -> binding.btnSetTime.performClick()
            "version" -> binding.btnVersion.performClick()
            "camera" -> binding.btnCamera.performClick()

            // Video recording controls
            "video" -> binding.btnVideo.performClick()
            "video_start" -> controlVideoRecording(true)
            "video_stop" -> controlVideoRecording(false)

            // Audio recording controls
            "record" -> binding.btnRecord.performClick()
            "record_start" -> controlAudioRecording(true)
            "record_stop" -> controlAudioRecording(false)

            "bt_scan" -> binding.btnBt.performClick()
            "battery" -> binding.btnBattery.performClick()
            "volume" -> binding.btnVolume.performClick()
            "media_count" -> binding.btnMediaCount.performClick()
            "data_download" -> binding.btnDataDownload.performClick()
        }
    }

    private fun currentBleMacNoColonUpper(): String? {
        return try {
            DeviceManager.getInstance().deviceAddress
                ?.replace(":", "")
                ?.uppercase(Locale.US)
                ?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun currentBleNameUpper(): String? {
        return try {
            DeviceManager.getInstance().deviceName
                ?.uppercase(Locale.US)
                ?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun syncLog(priority: Int, tag: String, message: String, throwable: Throwable? = null) {
        SyncLogBuffer.log(priority, tag, message, throwable)
    }

    private fun syncInfo(tag: String = "DataDownload", message: String) {
        syncLog(Log.INFO, tag, message)
    }

    private fun syncWarn(tag: String = "DataDownload", message: String, throwable: Throwable? = null) {
        syncLog(Log.WARN, tag, message, throwable)
    }

    private fun syncError(tag: String = "DataDownload", message: String, throwable: Throwable? = null) {
        syncLog(Log.ERROR, tag, message, throwable)
    }

    private data class TechnicalSyncRow(
        val timestamp: String,
        val action: String,
        val uuid: String,
        val command: String,
        val response: String,
        val status: String,
    )

    private data class MediaDownloadItem(
        val type: String,
        val fileName: String,
    )

    private fun bleWriteUuidLabel(): String =
        "service=${Constants.UUID_SERVICE}; write=${Constants.UUID_WRITE}"

    private fun bleNotifyUuidLabel(): String =
        "service=${Constants.UUID_SERVICE}; notify=${Constants.UUID_READ}"

    private fun serialNotifyUuidLabel(): String =
        "serialService=${Constants.SERIAL_PORT_SERVICE}; notify=${Constants.SERIAL_PORT_CHARACTER_NOTIFY}"

    private fun recordTechnicalSyncRow(
        action: String,
        uuid: String,
        command: String,
        response: String,
        status: String,
    ) {
        val row = TechnicalSyncRow(
            timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()),
            action = action,
            uuid = uuid,
            command = command,
            response = response,
            status = status,
        )
        synchronized(technicalSyncRows) {
            technicalSyncRows.add(row)
            if (technicalSyncRows.size > 500) {
                technicalSyncRows.removeAt(0)
            }
        }
    }

    private fun clearTechnicalSyncRows() {
        synchronized(technicalSyncRows) {
            technicalSyncRows.clear()
        }
    }

    private fun markdownCell(value: String): String =
        value.replace("|", "\\|")
            .replace("\r", " ")
            .replace("\n", " ")
            .ifBlank { "N/A" }

    private fun technicalSyncMarkdownTable(): String {
        val rows = synchronized(technicalSyncRows) { technicalSyncRows.toList() }
        val out = StringBuilder()
        out.append("| Date & Time | Purpose / Action | BLE UUID Used | Command Sent | Notification / Response Received | Result / Status |\n")
        out.append("| --- | --- | --- | --- | --- | --- |\n")
        if (rows.isEmpty()) {
            out.append("| ${markdownCell(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))} | No technical sync actions captured yet | N/A | N/A | N/A | No data |\n")
        } else {
            rows.forEach { row ->
                out.append("| ${markdownCell(row.timestamp)} | ${markdownCell(row.action)} | ${markdownCell(row.uuid)} | ${markdownCell(row.command)} | ${markdownCell(row.response)} | ${markdownCell(row.status)} |\n")
            }
        }
        return out.toString()
    }

    private fun syncDebugContext(): String {
        val pm = packageManager
        val appVersion = runCatching {
            val info = pm.getPackageInfo(packageName, 0)
            "${info.versionName} (${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode})"
        }.getOrDefault("unknown")

        val wifiEnabled = runCatching {
            (getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager).isWifiEnabled
        }.getOrDefault(false)
        val bluetoothEnabled = runCatching {
            BluetoothAdapter.getDefaultAdapter()?.isEnabled == true
        }.getOrDefault(false)
        val locationEnabled = runCatching {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lm.isLocationEnabled
            } else {
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }
        }.getOrDefault(false)
        val vpnActive = runCatching {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.allNetworks.any { network ->
                cm.getNetworkCapabilities(network)
                    ?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            }
        }.getOrDefault(false)

        val permissions = listOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ).distinct().joinToString(separator = "\n") { perm ->
            val granted = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                true
            } else {
                ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
            }
            "permission.$perm=$granted"
        }

        return """
            CyanBridge Sync Debug Logs
            timestamp=${SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())}
            appVersion=$appVersion
            android=${Build.VERSION.RELEASE} sdk=${Build.VERSION.SDK_INT}
            phone=${Build.MANUFACTURER} ${Build.MODEL}
            bluetoothEnabled=$bluetoothEnabled
            bluetoothConnected=${BleOperateManager.getInstance().isConnected}
            glassesName=${runCatching { DeviceManager.getInstance().deviceName }.getOrNull()}
            glassesAddress=${runCatching { DeviceManager.getInstance().deviceAddress }.getOrNull()}
            wifiEnabled=$wifiEnabled
            locationEnabled=$locationEnabled
            vpnActive=$vpnActive
            p2pConnected=$downloadP2pConnected
            phoneIsGroupOwner=$downloadPhoneIsGroupOwner
            groupOwnerIp=$downloadWifiIp
            bleReportedIp=$downloadBleIp
            resolvedHttpIp=$downloadResolvedHttpIp
            $permissions

            ---- logs ----
        """.trimIndent()
    }

    private fun syncLogPayload(): String {
        val logs = SyncLogBuffer.snapshot().ifBlank { "(no sync logs yet)" }
        return syncDebugContext() + "\n" + logs
    }

    private fun copySyncLogsToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("CyanBridge sync logs", syncLogPayload()))
        Toast.makeText(this, "Sync logs copied", Toast.LENGTH_SHORT).show()
    }

    private fun copyTechnicalSyncLogToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "CyanBridge technical sync log",
                technicalSyncMarkdownTable(),
            )
        )
        Toast.makeText(this, "Technical sync log copied", Toast.LENGTH_SHORT).show()
    }

    private fun shareSyncLogs() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "CyanBridge sync logs")
            putExtra(Intent.EXTRA_TEXT, syncLogPayload())
        }
        startActivity(Intent.createChooser(intent, "Share sync logs"))
    }

    private fun isLikelyGlassesPeer(device: WifiP2pDevice, bleMacNoColon: String?): Boolean {
        val name = (device.deviceName ?: "").uppercase(Locale.US)
        if (name.isBlank()) return false

        if (!bleMacNoColon.isNullOrBlank() && name.contains(bleMacNoColon)) {
            return true
        }

        val bleName = currentBleNameUpper()
        val bleNameTokens = listOfNotNull(
            bleName,
            bleName?.substringAfter('_', missingDelimiterValue = "")?.takeIf { it.length >= 4 },
            bleName?.takeLast(4)?.takeIf { it.length == 4 },
        )
        if (bleNameTokens.any { token -> token.isNotBlank() && name.contains(token) }) {
            return true
        }

        if (name.startsWith("V03._") ||
            name.contains("V03._") ||
            name.contains("HEY") && name.contains("CYAN") ||
            name.contains("GLASSES") ||
            name.contains("A03PRO") ||
            name.contains("WIFIA03PRO")
        ) {
            return true
        }

        return false
    }

    private fun selectBestLikelyGlassesPeer(peers: Collection<WifiP2pDevice>): WifiP2pDevice? {
        if (peers.isEmpty()) return null

        val bleMacNoColon = currentBleMacNoColonUpper()
        val byBleMac = peers.firstOrNull { p ->
            val p2pName = (p.deviceName ?: "").uppercase(Locale.US)
            !bleMacNoColon.isNullOrBlank() && p2pName.contains(bleMacNoColon)
        }
        if (byBleMac != null) return byBleMac

        val likely = peers.filter { isLikelyGlassesPeer(it, bleMacNoColon) }
        if (likely.isNotEmpty()) return likely.first()

        return null
    }

    private fun takePhotoAndDownload() {
        if (takePhotoAndDownloadJob?.isActive == true) {
            Toast.makeText(this, "Take Photo and Download already running.", Toast.LENGTH_SHORT).show()
            return
        }
        if (downloadAttemptJob?.isActive == true || downloadInProgress) {
            Toast.makeText(this, "Sync already running. Stop it first.", Toast.LENGTH_LONG).show()
            return
        }
        if (!BleOperateManager.getInstance().isConnected) {
            Toast.makeText(this, "Bluetooth not connected. Connect glasses first.", Toast.LENGTH_LONG).show()
            syncError("TakePhotoDownload", "Bluetooth not connected")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !XXPermissions.isGranted(this, "android.permission.NEARBY_WIFI_DEVICES")
        ) {
            requestNearbyWifiDevicesPermission(this, object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        startTakePhotoAndDownloadInternal()
                    } else {
                        Toast.makeText(this@MainActivity, "Wi-Fi permission missing.", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    super.onDenied(permissions, never)
                    syncWarn("TakePhotoDownload", "NEARBY_WIFI_DEVICES denied: never=$never permissions=$permissions")
                    if (never) {
                        XXPermissions.startPermissionActivity(this@MainActivity, permissions)
                    }
                }
            })
            return
        }

        startTakePhotoAndDownloadInternal()
    }

    private fun startTakePhotoAndDownloadInternal() {
        takePhotoAndDownloadJob = CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(this@MainActivity, "Taking photo, then syncing...", Toast.LENGTH_SHORT).show()
            setTransferUiVisible(true)
            resetTransferUiState()
            setTransferDetail("Taking photo...")
            syncInfo("TakePhotoDownload", "Automation started: take photo, then enable Wi-Fi/P2P sync")

            val photoResponse = sendTakePhotoCommandForDownload()
            if (photoResponse == null) {
                syncWarn("TakePhotoDownload", "Photo command timed out; continuing to sync in case the glasses captured it")
            } else {
                syncInfo(
                    "TakePhotoDownload",
                    "Photo command response: dataType=${photoResponse.dataType}, error=${photoResponse.errorCode}, workTypeIng=${photoResponse.workTypeIng}"
                )
            }

            setTransferDetail("Waiting for photo to save on glasses...")
            delay(4_000)
            setTransferDetail("Starting Wi-Fi/P2P sync for new photo...")
            syncInfo("TakePhotoDownload", "Starting Sync Data after photo capture wait")
            startDataDownload()
        }
    }

    private suspend fun sendTakePhotoCommandForDownload(): GlassModelControlResponse? {
        val deferred = CompletableDeferred<GlassModelControlResponse?>()
        try {
            LargeDataHandler.getInstance().glassesControl(
                byteArrayOf(0x02, 0x01, 0x01)
            ) { _, response ->
                recordTechnicalSyncRow(
                    action = "Capture photo",
                    uuid = bleWriteUuidLabel(),
                    command = "02 01 01",
                    response = "dataType=${response.dataType}, errorCode=${response.errorCode}, workTypeIng=${response.workTypeIng}, p2pIp=${response.p2pIp}",
                    status = if (response.dataType == 1 && response.errorCode == 0) "Confirmed" else "Not confirmed",
                )
                if (!deferred.isCompleted) {
                    deferred.complete(response)
                }
            }
        } catch (t: Throwable) {
            syncError("TakePhotoDownload", "Photo command failed: ${t.message}", t)
            if (!deferred.isCompleted) {
                deferred.complete(null)
            }
        }
        return withTimeoutOrNull(5_000) { deferred.await() }
    }

    private fun takeTimedMediaAndDownload(
        mediaLabel: String,
        startCommand: ByteArray,
        stopCommand: ByteArray,
        recordingMs: Long,
        saveWaitMs: Long,
    ) {
        if (takePhotoAndDownloadJob?.isActive == true) {
            Toast.makeText(this, "$mediaLabel and Download already running.", Toast.LENGTH_SHORT).show()
            return
        }
        if (downloadAttemptJob?.isActive == true || downloadInProgress) {
            Toast.makeText(this, "Sync already running. Stop it first.", Toast.LENGTH_LONG).show()
            return
        }
        if (!BleOperateManager.getInstance().isConnected) {
            Toast.makeText(this, "Bluetooth not connected. Connect glasses first.", Toast.LENGTH_LONG).show()
            syncError("Take${mediaLabel}Download", "Bluetooth not connected")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !XXPermissions.isGranted(this, "android.permission.NEARBY_WIFI_DEVICES")
        ) {
            requestNearbyWifiDevicesPermission(this, object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        startTimedMediaAndDownloadInternal(mediaLabel, startCommand, stopCommand, recordingMs, saveWaitMs)
                    } else {
                        Toast.makeText(this@MainActivity, "Wi-Fi permission missing.", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    super.onDenied(permissions, never)
                    syncWarn("Take${mediaLabel}Download", "NEARBY_WIFI_DEVICES denied: never=$never permissions=$permissions")
                    if (never) {
                        XXPermissions.startPermissionActivity(this@MainActivity, permissions)
                    }
                }
            })
            return
        }

        startTimedMediaAndDownloadInternal(mediaLabel, startCommand, stopCommand, recordingMs, saveWaitMs)
    }

    private fun startTimedMediaAndDownloadInternal(
        mediaLabel: String,
        startCommand: ByteArray,
        stopCommand: ByteArray,
        recordingMs: Long,
        saveWaitMs: Long,
    ) {
        takePhotoAndDownloadJob = CoroutineScope(Dispatchers.Main).launch {
            val tag = "Take${mediaLabel}Download"
            Toast.makeText(this@MainActivity, "Recording $mediaLabel for 10 seconds...", Toast.LENGTH_SHORT).show()
            setTransferUiVisible(true)
            resetTransferUiState()
            setTransferDetail("Starting $mediaLabel recording...")
            syncInfo(tag, "Automation started: record $mediaLabel for ${recordingMs / 1000}s, then enable Wi-Fi/P2P sync")

            val startResponse = sendTimedMediaCommandForDownload(
                label = "Start $mediaLabel",
                payload = startCommand,
            )
            syncInfo(tag, "Start $mediaLabel response: ${formatGlassControlResponse(startResponse)}")

            setTransferDetail("Recording $mediaLabel... 10 seconds")
            delay(recordingMs)

            setTransferDetail("Stopping $mediaLabel recording...")
            val stopResponse = sendTimedMediaCommandForDownload(
                label = "Stop $mediaLabel before transfer",
                payload = stopCommand,
            )
            syncInfo(tag, "Stop $mediaLabel response: ${formatGlassControlResponse(stopResponse)}")

            setTransferDetail("Waiting for $mediaLabel to save on glasses...")
            delay(saveWaitMs)
            setTransferDetail("Starting Wi-Fi/P2P sync for new $mediaLabel...")
            syncInfo(tag, "Starting Sync Data after $mediaLabel save wait")
            startDataDownload()
        }
    }

    private suspend fun sendTimedMediaCommandForDownload(
        label: String,
        payload: ByteArray,
    ): GlassModelControlResponse? {
        val deferred = CompletableDeferred<GlassModelControlResponse?>()
        val command = commandToHex(payload)
        try {
            LargeDataHandler.getInstance().glassesControl(payload) { _, response ->
                recordTechnicalSyncRow(
                    action = label,
                    uuid = bleWriteUuidLabel(),
                    command = command,
                    response = formatGlassControlResponse(response),
                    status = if (response.errorCode == 0) "Confirmed" else "Non-fatal error",
                )
                if (!deferred.isCompleted) {
                    deferred.complete(response)
                }
            }
        } catch (t: Throwable) {
            syncError("TimedMediaDownload", "$label command failed: ${t.message}", t)
            if (!deferred.isCompleted) {
                deferred.complete(null)
            }
        }
        return withTimeoutOrNull(5_000) { deferred.await() }
    }

    private fun formatGlassControlResponse(response: GlassModelControlResponse?): String {
        return if (response == null) {
            "timeout/null response"
        } else {
            "dataType=${response.dataType}, errorCode=${response.errorCode}, workTypeIng=${response.workTypeIng}, p2pIp=${response.p2pIp}, image=${response.imageCount}, video=${response.videoCount}, record=${response.recordCount}"
        }
    }

    private fun startDataDownload() {
        syncInfo("DataDownload", "Starting BLE+WiFi P2P data download...")
        syncInfo(
            "DataDownload",
            "Preflight: btConnected=${BleOperateManager.getInstance().isConnected}, deviceName=${DeviceManager.getInstance().deviceName}, deviceAddress=${DeviceManager.getInstance().deviceAddress}"
        )

        // Check Bluetooth connection status
        if (!BleOperateManager.getInstance().isConnected) {
            syncError("DataDownload", "Bluetooth not connected. Please connect to glasses first.")
            Toast.makeText(
                this,
                "Bluetooth not connected. Please connect to glasses first.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Check WiFi is enabled (required for WiFi Direct / P2P)
        val wifiManager = getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        syncInfo("DataDownload", "WiFi enabled=${wifiManager.isWifiEnabled}")
        if (!wifiManager.isWifiEnabled) {
            syncError("DataDownload", "WiFi is disabled. WiFi must be on for P2P sync.")
            Toast.makeText(
                this,
                "Please enable WiFi to sync with glasses.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Check NEARBY_WIFI_DEVICES on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !XXPermissions.isGranted(this, "android.permission.NEARBY_WIFI_DEVICES")
        ) {
            syncError("DataDownload", "NEARBY_WIFI_DEVICES permission not granted")
            Toast.makeText(
                this,
                "NEARBY_WIFI_DEVICES permission not granted.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Tear down any stale session first so retries do not stack callbacks/jobs.
        teardownDownloadP2pSession(sendExitTransfer = false, hideTransferUi = false)
        downloadCancelledByUser = false
        clearTechnicalSyncRows()

        // Reset state for a fresh run
        downloadP2pConnected = false
        downloadBleIp = null
        bleIpBridge.clear()
        downloadWifiIp = null
        downloadInProgress = false
        downloadResolvedHttpIp = null
        lastDownloadBleIpAtMs = 0L
        downloadTransferCommandAttempts = 0
        downloadTransferCommandSent = false
        downloadTransferCommandProgressDetected = false
        downloadTransferCallbackError = null
        downloadPausedForReconnect = false
        downloadPeerTimeoutJob?.cancel()
        downloadBleIpTimeoutJob?.cancel()

        resetTransferUiState()
        setTransferUiVisible(true)
        setTransferDetail("Starting sync...")
        syncInfo("DataDownload", syncDebugContext().substringBefore("---- logs ----").trim())

        if (!downloadNotifyListenerRegistered) {
            try {
                LargeDataHandler.getInstance().addOutDeviceListener(2, downloadNotifyListener)
                downloadNotifyListenerRegistered = true
                syncInfo("DataDownload", "Registered download notify listener (cmdType=2)")
                recordTechnicalSyncRow(
                    action = "Enable extra notification listener",
                    uuid = bleNotifyUuidLabel(),
                    command = "N/A",
                    response = "cmdType=2 download notification listener enabled",
                    status = "Success",
                )
            } catch (e: Exception) {
                syncError("DataDownload", "Failed to register download notify listener", e)
                recordTechnicalSyncRow(
                    action = "Enable extra notification listener",
                    uuid = bleNotifyUuidLabel(),
                    command = "N/A",
                    response = "failed: ${e.message}",
                    status = "Failed",
                )
            }
        }

        val wifiP2pManager = WifiP2pManagerSingleton.getInstance(this)
        downloadWifiP2pManager = wifiP2pManager

        // Mirror vendor flow: clear internal retry state.
        wifiP2pManager.resetFailCount()

        // Register receiver and listen for P2P state/peer changes
        wifiP2pManager.registerReceiver()

        val callback = object : WifiP2pManagerSingleton.WifiP2pCallback {
            override fun onWifiP2pEnabled() {
                syncInfo("DataDownload", "WiFi P2P enabled")
            }

            override fun onWifiP2pDisabled() {
                syncError("DataDownload", "WiFi P2P disabled")
            }

            override fun onPeersChanged(peers: Collection<WifiP2pDevice>) {
                syncInfo("DataDownload", "Found ${peers.size} P2P devices")
                peers.forEach { peer ->
                    syncInfo(
                        "DataDownload",
                        "P2P peer: name=${peer.deviceName}, address=${peer.deviceAddress}, status=${peer.status}"
                    )
                }
                if (peers.isEmpty()) return
                downloadPeerTimeoutJob?.cancel()

                // Guard against redundant connection attempts (official app uses isP2PConnecting).
                if (downloadWifiP2pManager?.isConnecting() == true || downloadWifiP2pManager?.isConnected() == true) {
                    syncInfo("DataDownload", "Already connecting/connected, skipping peer re-evaluation")
                    return
                }

                val target = selectBestLikelyGlassesPeer(peers)
                if (target == null) {
                    syncInfo(
                        "DataDownload",
                        "No matching glasses peer yet; ignoring discovered peers: ${peers.map { "${it.deviceName}/${it.deviceAddress}" }}"
                    )
                    setTransferDetail("Waiting for glasses P2P peer...")
                    return
                }

                markTransferProgressDetected("matching P2P peer found")
                recordTechnicalSyncRow(
                    action = "P2P peer discovered",
                    uuid = "Wi-Fi Direct, not BLE",
                    command = "N/A",
                    response = "name=${target.deviceName}, address=${target.deviceAddress}, status=${target.status}",
                    status = "Confirmed",
                )
                syncInfo(
                    "DataDownload",
                    "Selected glasses P2P peer: ${target.deviceName} / ${target.deviceAddress}; reason=matched glasses pattern"
                )
                wifiP2pManager.connectToDevice(target)
            }

            override fun onThisDeviceChanged(device: WifiP2pDevice) {
                syncInfo(
                    "DataDownload",
                    "This device changed: ${device.deviceName} - ${device.status}"
                )
            }

            override fun onConnected(info: WifiP2pInfo) {
                syncInfo(
                    "DataDownload",
                    "P2P connected: groupFormed=${info.groupFormed}, isGroupOwner=${info.isGroupOwner}, groupOwnerAddress=${info.groupOwnerAddress?.hostAddress}"
                )
                onDownloadP2pConnected(info)
            }

            override fun onDisconnected() {
                syncInfo("DataDownload", "P2P disconnected")
                downloadP2pConnected = false
                downloadP2pNetwork = null
                unbindProcessFromNetwork()

                val shouldRecover = !downloadCancelledByUser &&
                    (downloadAttemptJob?.isActive == true || downloadInProgress)
                if (shouldRecover) {
                    downloadPausedForReconnect = true
                    syncWarn("DataDownload", "P2P disconnected during download, pausing and reconnecting")
                    recordTechnicalSyncRow(
                        action = "P2P disconnected",
                        uuid = "Wi-Fi Direct, not BLE",
                        command = "N/A",
                        response = "disconnect callback",
                        status = "Pause downloads and reconnect",
                    )
                    setTransferDetail("P2P disconnected; pausing downloads and reconnecting...")
                    downloadWifiP2pManager?.discoverPeersStable()
                    downloadWifiP2pManager?.startPeerDiscovery()
                }
            }

            override fun onPeerDiscoveryStarted() {
                syncInfo("DataDownload", "Peer discovery started")
            }

            override fun onPeerDiscoveryFailed(reason: Int) {
                syncError("DataDownload", "Peer discovery failed: $reason")
            }

            override fun onConnectRequestSent() {
                syncInfo("DataDownload", "Connect request sent")
                markTransferProgressDetected("P2P connect request sent")
            }

            override fun onConnectRequestFailed(reason: Int) {
                syncError("DataDownload", "Connect request failed: $reason")
            }

            override fun connecting() {
                syncInfo("DataDownload", "Connecting to P2P device...")
            }

            override fun cancelConnect() {
                syncInfo("DataDownload", "P2P connection cancelled")
            }

            override fun cancelConnectFail(reason: Int) {
                syncError("DataDownload", "Cancel connect failed: $reason")
            }

            override fun retryAlsoFailed() {
                syncError("DataDownload", "P2P connection retry failed")
            }
        }

        downloadWifiP2pCallback = callback
        wifiP2pManager.addCallback(callback)

        // Start scanning for the glasses over WiFi Direct
        scheduleNoPeerTimeout()
        wifiP2pManager.startPeerDiscovery()

        setTransferDetail("Waiting for glasses IP and HTTP server...")

        // Ask the glasses (over BLE) to bring up WiFi/P2P and report their IP,
        // mirroring the official app's importAlbum() flow.
        sendTransferModeCommand()
    }

    private fun sendTransferModeCommand() {
        if (downloadCancelledByUser) return
        if (downloadTransferCommandSent) {
            syncInfo("DataDownload", "Transfer command already sent once for this sync session; not sending again")
            return
        }
        downloadTransferCommandSent = true
        downloadTransferCommandAttempts = 1
        val attempt = downloadTransferCommandAttempts
        syncInfo(
            "DataDownload",
            "Transfer command sent once: byteArrayOf(0x02, 0x01, 0x04), attempt=$attempt"
        )
        recordTechnicalSyncRow(
            action = "Enable transfer / Wi-Fi P2P mode",
            uuid = bleWriteUuidLabel(),
            command = "02 01 04",
            response = "command sent",
            status = "Pending callback/progress",
        )
        LargeDataHandler.getInstance().glassesControl(
            byteArrayOf(0x02, 0x01, 0x04)
        ) { _, resp ->
            syncInfo(
                "DataDownload",
                "glassesControl[0x02,0x01,0x04] attempt=$attempt -> dataType=${resp.dataType}, error=${resp.errorCode}, workTypeIng=${resp.workTypeIng}, p2pIp=${resp.p2pIp}, image=${resp.imageCount}, video=${resp.videoCount}, record=${resp.recordCount}"
            )
            val responseText = "dataType=${resp.dataType}, errorCode=${resp.errorCode}, workTypeIng=${resp.workTypeIng}, p2pIp=${resp.p2pIp}, image=${resp.imageCount}, video=${resp.videoCount}, record=${resp.recordCount}"
            downloadTransferCallbackError = resp.errorCode
            if (resp.errorCode == 0) {
                markTransferProgressDetected("transfer command accepted")
                recordTechnicalSyncRow(
                    action = "Enable transfer / Wi-Fi P2P mode",
                    uuid = bleWriteUuidLabel(),
                    command = "02 01 04",
                    response = responseText,
                    status = "Success",
                )
            } else {
                val status = if (downloadTransferCommandProgressDetected) {
                    "Non-fatal error, Wi-Fi progress detected"
                } else {
                    "Non-fatal pending Wi-Fi/P2P progress"
                }
                syncWarn(
                    "DataDownload",
                    "Transfer command callback error=${resp.errorCode}; not retrying. Waiting for Wi-Fi/P2P progress."
                )
                recordTechnicalSyncRow(
                    action = "Enable transfer / Wi-Fi P2P mode",
                    uuid = bleWriteUuidLabel(),
                    command = "02 01 04",
                    response = responseText,
                    status = status,
                )
            }
            if (!resp.p2pIp.isNullOrBlank()) {
                syncInfo("DataDownload", "Transfer command reported p2pIp=${resp.p2pIp}")
                onDownloadBleIp(resp.p2pIp)
            }
        }
    }

    private fun markTransferProgressDetected(reason: String) {
        if (!downloadTransferCommandProgressDetected) {
            downloadTransferCommandProgressDetected = true
            syncInfo("DataDownload", "Stopping transfer command retries: progress detected ($reason)")
            downloadTransferCallbackError?.let { error ->
                if (error != 0) {
                    syncInfo(
                        "DataDownload",
                        "Transfer command callback error ignored because Wi-Fi/P2P progress detected: error=$error reason=$reason"
                    )
                    recordTechnicalSyncRow(
                        action = "Enable transfer / Wi-Fi P2P mode",
                        uuid = bleWriteUuidLabel(),
                        command = "02 01 04",
                        response = "callback error=$error, later progress=$reason",
                        status = "Non-fatal, Wi-Fi progress detected",
                    )
                }
            }
        }
    }

    private fun runOfficialEnableWifiDebug() {
        if (!BleOperateManager.getInstance().isConnected) {
            syncError("OfficialWifi", "Bluetooth is not connected; connect glasses first.")
            Toast.makeText(this, "Bluetooth not connected. Connect glasses first.", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(this, "Starting official WiFi debug flow...", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.Main).launch {
            syncInfo("OfficialWifi", "Official Enable WiFi clicked")

            try {
                LargeDataHandler.getInstance().initEnable()
                syncInfo("OfficialWifi", "LargeDataHandler.initEnable() called")
            } catch (t: Throwable) {
                syncWarn("OfficialWifi", "initEnable() failed: ${t.message}")
            }

            delay(1_000)
            syncInfo("OfficialWifi", "Stopping video/audio before transfer mode")
            sendGlassesControlAwait(
                label = "stopVideo",
                payload = byteArrayOf(0x02, 0x01, 0x03),
            )
            delay(300)
            sendGlassesControlAwait(
                label = "stopAudio",
                payload = byteArrayOf(0x02, 0x01, 0x0c),
            )
            delay(500)

            syncInfo("OfficialWifi", "Calling getDeviceMedia/media count before enabling WiFi")
            sendGlassesControlAwait(
                label = "getDeviceMedia",
                payload = byteArrayOf(0x02, 0x04),
            )
            delay(500)

            syncInfo(
                "OfficialWifi",
                "Calling Android SDK transfer-mode equivalent: LargeDataHandler.glassesControl(byteArrayOf(0x02, 0x01, 0x04))"
            )
            val enableResponse = sendGlassesControlAwait(
                label = "enableTransferWifi",
                payload = byteArrayOf(0x02, 0x01, 0x04),
                timeoutMs = 8_000,
            )

            if (enableResponse == null) {
                syncWarn("OfficialWifi", "No BLE response for transfer-mode command; will still try known SSID for debug.")
            } else if (enableResponse.errorCode != 0) {
                syncWarn(
                    "OfficialWifi",
                    "Transfer-mode command returned error=${enableResponse.errorCode}; trying known SSID anyway to verify hotspot state."
                )
            } else {
                syncInfo("OfficialWifi", "Transfer-mode command accepted.")
            }

            val mac = currentBleMacNoColonUpper()
            if (mac.isNullOrBlank()) {
                syncError("OfficialWifi", "Could not derive SSID because Bluetooth MAC is unavailable.")
                Toast.makeText(this@MainActivity, "Bluetooth MAC unavailable; cannot derive SSID.", Toast.LENGTH_LONG).show()
                return@launch
            }

            val ssid = "V03._$mac"
            val password = "123456789"
            syncInfo("OfficialWifi", "Derived hotspot SSID=$ssid password=$password deviceIps=192.168.31.2,192.168.31.1")
            connectToOfficialWifiHotspot(ssid, password)
        }
    }

    private suspend fun sendGlassesControlAwait(
        label: String,
        payload: ByteArray,
        timeoutMs: Long = 5_000,
    ): GlassModelControlResponse? {
        val deferred = CompletableDeferred<GlassModelControlResponse?>()
        val hex = payload.joinToString(prefix = "[", postfix = "]") { "0x%02X".format(it.toInt() and 0xff) }
        syncInfo("OfficialWifi", "$label request payload=$hex")

        try {
            LargeDataHandler.getInstance().glassesControl(payload) { _, resp ->
                syncInfo(
                    "OfficialWifi",
                    "$label response -> dataType=${resp.dataType}, error=${resp.errorCode}, workTypeIng=${resp.workTypeIng}, p2pIp=${resp.p2pIp}, image=${resp.imageCount}, video=${resp.videoCount}, record=${resp.recordCount}"
                )
                if (!deferred.isCompleted) {
                    deferred.complete(resp)
                }
            }
        } catch (t: Throwable) {
            syncError("OfficialWifi", "$label glassesControl failed: ${t.message}")
            if (!deferred.isCompleted) {
                deferred.complete(null)
            }
        }

        return withTimeoutOrNull(timeoutMs) { deferred.await() }.also {
            if (it == null) {
                syncWarn("OfficialWifi", "$label timed out after ${timeoutMs}ms")
            }
        }
    }

    private fun connectToOfficialWifiHotspot(ssid: String, password: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            syncWarn("OfficialWifi", "Automatic hotspot request needs Android 10+; please manually join $ssid and retry HTTP probes.")
            return
        }

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        officialWifiNetworkCallback?.let { oldCallback ->
            try {
                connectivityManager.unregisterNetworkCallback(oldCallback)
            } catch (_: Throwable) {
            }
            officialWifiNetworkCallback = null
        }

        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(specifier)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                syncInfo("OfficialWifi", "Android connected to hotspot SSID=$ssid; testing HTTP endpoints")
                CoroutineScope(Dispatchers.IO).launch {
                    testOfficialWifiHttpEndpoints(network)
                }
            }

            override fun onUnavailable() {
                syncError("OfficialWifi", "Android could not connect to hotspot SSID=$ssid. Accept the system WiFi prompt if shown.")
            }

            override fun onLost(network: Network) {
                syncWarn("OfficialWifi", "Hotspot network lost for SSID=$ssid")
            }
        }

        officialWifiNetworkCallback = callback
        syncInfo("OfficialWifi", "Requesting Android WiFi connection to SSID=$ssid. Accept the system prompt if shown.")
        try {
            connectivityManager.requestNetwork(request, callback, 60_000)
        } catch (t: Throwable) {
            syncError("OfficialWifi", "requestNetwork failed for SSID=$ssid: ${t.message}")
        }
    }

    private suspend fun testOfficialWifiHttpEndpoints(network: Network) {
        val urls = listOf(
            "http://192.168.31.2/manifest.json",
            "http://192.168.31.2/media.config",
            "http://192.168.31.2/files/media.config",
            "http://192.168.31.1/manifest.json",
            "http://192.168.31.1/media.config",
            "http://192.168.31.1/files/media.config",
        )

        for (url in urls) {
            val result = probeOfficialWifiUrl(network, url)
            if (result.ok) {
                syncInfo("OfficialWifi", "HTTP OK ${result.url} code=${result.code} bytes=${result.bytes} preview=${result.preview}")
            } else {
                syncWarn("OfficialWifi", "HTTP FAIL ${result.url} code=${result.code} error=${result.error}")
            }
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, "Official WiFi probe finished. Copy/share sync logs.", Toast.LENGTH_LONG).show()
        }
    }

    private fun probeOfficialWifiUrl(network: Network, urlText: String): OfficialWifiProbeResult {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlText)
            connection = network.openConnection(url) as HttpURLConnection
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = false

            val code = connection.responseCode
            val stream = if (code in 200..399) connection.inputStream else connection.errorStream
            val data = stream?.use { input ->
                val buffer = ByteArrayOutputStream()
                val chunk = ByteArray(1024)
                var total = 0
                while (true) {
                    val read = input.read(chunk)
                    if (read <= 0) break
                    val allowed = (4096 - total).coerceAtMost(read)
                    if (allowed > 0) {
                        buffer.write(chunk, 0, allowed)
                        total += allowed
                    }
                    if (total >= 4096) break
                }
                buffer.toByteArray()
            } ?: ByteArray(0)
            val preview = String(data, Charsets.UTF_8)
                .replace('\n', ' ')
                .replace('\r', ' ')
                .take(300)
            OfficialWifiProbeResult(urlText, ok = code in 200..399, code = code, bytes = data.size, preview = preview)
        } catch (t: Throwable) {
            OfficialWifiProbeResult(urlText, ok = false, code = null, bytes = 0, error = t.javaClass.simpleName + ": " + (t.message ?: "unknown"))
        } finally {
            connection?.disconnect()
        }
    }

    private data class OfficialWifiProbeResult(
        val url: String,
        val ok: Boolean,
        val code: Int?,
        val bytes: Int,
        val preview: String = "",
        val error: String? = null,
    )

    private fun runWifiCommandTest() {
        if (wifiCommandTestJob?.isActive == true) {
            Toast.makeText(this, "Wi-Fi command test already running.", Toast.LENGTH_SHORT).show()
            return
        }
        if (downloadAttemptJob?.isActive == true || downloadInProgress) {
            Toast.makeText(this, "Stop normal sync before running command test.", Toast.LENGTH_LONG).show()
            return
        }
        if (!BleOperateManager.getInstance().isConnected) {
            setWifiCommandTestStatus("Bluetooth not connected")
            syncError("WiFiCmdTest", "Bluetooth is not connected; connect glasses first.")
            Toast.makeText(this, "Bluetooth not connected. Connect glasses first.", Toast.LENGTH_LONG).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !XXPermissions.isGranted(this, "android.permission.NEARBY_WIFI_DEVICES")
        ) {
            requestNearbyWifiDevicesPermission(this, object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        startWifiCommandTestInternal()
                    } else {
                        setWifiCommandTestStatus("Wi-Fi permission missing")
                    }
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    super.onDenied(permissions, never)
                    setWifiCommandTestStatus("Wi-Fi permission denied")
                    syncWarn("WiFiCmdTest", "NEARBY_WIFI_DEVICES denied: never=$never permissions=$permissions")
                    if (never) {
                        XXPermissions.startPermissionActivity(this@MainActivity, permissions)
                    }
                }
            })
            return
        }

        startWifiCommandTestInternal()
    }

    private fun startWifiCommandTestInternal() {
        wifiCommandTestJob = CoroutineScope(Dispatchers.Main).launch {
            val commands = wifiCommandTestCandidates()
            syncInfo("WiFiCmdTest", "Starting debug-only Wi-Fi Command Test with ${commands.size} candidate commands")
            setWifiCommandTestStatus("Preparing command test")

            var successCommand: String? = null
            try {
                setupWifiCommandTestP2pProbe()
                registerWifiCommandTestNotifyListener()

                for ((index, command) in commands.withIndex()) {
                    if (!coroutineContext.isActive) break

                    val commandHex = commandToHex(command)
                    wifiCommandTestPeerFound = false
                    wifiCommandTestCurrentCommand = commandHex
                    setWifiCommandTestStatus("Testing command ${index + 1}/${commands.size}: $commandHex")
                    syncInfo("WiFiCmdTest", "Testing command ${index + 1}/${commands.size}: $commandHex")

                    val response = sendWifiCommandTestCommand(command, commandHex)
                    wifiCommandTestManager?.startPeerDiscovery()

                    if (response != null && response.errorCode == 0) {
                        successCommand = commandHex
                        syncInfo("WiFiCmdTest", "SUCCESS response for command $commandHex")
                        break
                    }

                    repeat(20) {
                        if (!coroutineContext.isActive || wifiCommandTestPeerFound || successCommand != null) {
                            return@repeat
                        }
                        delay(100)
                    }

                    if (wifiCommandTestPeerFound) {
                        successCommand = commandHex
                        syncInfo("WiFiCmdTest", "SUCCESS P2P peer appeared after command $commandHex")
                        break
                    }
                }
            } catch (t: Throwable) {
                syncError("WiFiCmdTest", "Command test failed: ${t.message}", t)
            } finally {
                wifiCommandTestCurrentCommand = null
                cleanupWifiCommandTest()
            }

            if (successCommand != null) {
                setWifiCommandTestStatus("Success command: $successCommand")
                Toast.makeText(this@MainActivity, "Success command: $successCommand", Toast.LENGTH_LONG).show()
            } else {
                setWifiCommandTestStatus("All failed")
                syncWarn("WiFiCmdTest", "All Wi-Fi command candidates failed")
                Toast.makeText(this@MainActivity, "Wi-Fi Command Test: All failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun wifiCommandTestCandidates(): List<ByteArray> = listOf(
        byteArrayOf(0x02, 0x01, 0x04),
        byteArrayOf(0x02, 0x01, 0x05),
        byteArrayOf(0x02, 0x01, 0x06),
        byteArrayOf(0x02, 0x01, 0x07),
        byteArrayOf(0x02, 0x01, 0x08),
        byteArrayOf(0x02, 0x01, 0x09),
        byteArrayOf(0x02, 0x02, 0x04),
        byteArrayOf(0x02, 0x03, 0x04),
        byteArrayOf(0x02, 0x04, 0x04),
        byteArrayOf(0x02, 0x05, 0x04),
        byteArrayOf(0x02, 0x01, 0x01),
        byteArrayOf(0x02, 0x01, 0x02),
        byteArrayOf(0x02, 0x01, 0x03),
        byteArrayOf(0x02, 0x01, 0x0A),
        byteArrayOf(0x02, 0x01, 0x0B),
        byteArrayOf(0x02, 0x01, 0x0C),
    )

    private suspend fun sendWifiCommandTestCommand(
        payload: ByteArray,
        commandHex: String,
    ): GlassModelControlResponse? {
        val deferred = CompletableDeferred<GlassModelControlResponse?>()
        try {
            LargeDataHandler.getInstance().glassesControl(payload) { _, resp ->
                syncInfo(
                    "WiFiCmdTest",
                    "response command=$commandHex dataType=${resp.dataType}, errorCode=${resp.errorCode}, workTypeIng=${resp.workTypeIng}, p2pIp=${resp.p2pIp}"
                )
                if (!resp.p2pIp.isNullOrBlank()) {
                    syncInfo("WiFiCmdTest", "command=$commandHex reported p2pIp=${resp.p2pIp}")
                }
                if (!deferred.isCompleted) {
                    deferred.complete(resp)
                }
            }
        } catch (t: Throwable) {
            syncError("WiFiCmdTest", "send failed command=$commandHex: ${t.message}", t)
            if (!deferred.isCompleted) {
                deferred.complete(null)
            }
        }

        return withTimeoutOrNull(2_000) { deferred.await() }.also {
            if (it == null) {
                syncWarn("WiFiCmdTest", "response timeout command=$commandHex")
            }
        }
    }

    private fun setupWifiCommandTestP2pProbe() {
        val manager = WifiP2pManagerSingleton.getInstance(this)
        wifiCommandTestManager = manager
        wifiCommandTestPeerFound = false

        val callback = object : WifiP2pManagerSingleton.WifiP2pCallback {
            override fun onWifiP2pEnabled() {
                syncInfo("WiFiCmdTest", "P2P probe: Wi-Fi P2P enabled")
            }

            override fun onWifiP2pDisabled() {
                syncWarn("WiFiCmdTest", "P2P probe: Wi-Fi P2P disabled")
            }

            override fun onPeersChanged(peers: Collection<WifiP2pDevice>) {
                val command = wifiCommandTestCurrentCommand ?: "none"
                syncInfo("WiFiCmdTest", "P2P peers after command=$command count=${peers.size}")
                peers.forEach { peer ->
                    syncInfo(
                        "WiFiCmdTest",
                        "P2P peer after command=$command name=${peer.deviceName}, address=${peer.deviceAddress}, status=${peer.status}"
                    )
                }
                if (peers.isNotEmpty()) {
                    wifiCommandTestPeerFound = true
                    setWifiCommandTestStatus("Success command: $command")
                }
            }

            override fun onThisDeviceChanged(device: WifiP2pDevice) {
                syncInfo("WiFiCmdTest", "P2P probe this device: ${device.deviceName} status=${device.status}")
            }

            override fun onConnected(info: WifiP2pInfo) {
                val command = wifiCommandTestCurrentCommand ?: "none"
                wifiCommandTestPeerFound = true
                syncInfo(
                    "WiFiCmdTest",
                    "P2P connected after command=$command groupFormed=${info.groupFormed}, isGroupOwner=${info.isGroupOwner}, groupOwnerAddress=${info.groupOwnerAddress?.hostAddress}"
                )
            }

            override fun onDisconnected() {
                syncInfo("WiFiCmdTest", "P2P probe disconnected")
            }

            override fun onPeerDiscoveryStarted() {
                syncInfo("WiFiCmdTest", "P2P peer discovery started")
            }

            override fun onPeerDiscoveryFailed(reason: Int) {
                syncWarn("WiFiCmdTest", "P2P peer discovery failed: $reason")
            }

            override fun onConnectRequestSent() {
                syncInfo("WiFiCmdTest", "P2P connect request sent")
            }

            override fun onConnectRequestFailed(reason: Int) {
                syncWarn("WiFiCmdTest", "P2P connect request failed: $reason")
            }

            override fun connecting() {
                syncInfo("WiFiCmdTest", "P2P connecting")
            }

            override fun cancelConnect() {
                syncInfo("WiFiCmdTest", "P2P cancel connect")
            }

            override fun cancelConnectFail(reason: Int) {
                syncWarn("WiFiCmdTest", "P2P cancel connect failed: $reason")
            }

            override fun retryAlsoFailed() {
                syncWarn("WiFiCmdTest", "P2P retry also failed")
            }
        }

        wifiCommandTestCallback = callback
        manager.registerReceiver()
        manager.addCallback(callback)
        manager.startPeerDiscovery()
    }

    private fun registerWifiCommandTestNotifyListener() {
        if (downloadNotifyListenerRegistered) {
            syncWarn("WiFiCmdTest", "Download notify listener already registered; not replacing cmdType=2 listener")
            return
        }
        try {
            LargeDataHandler.getInstance().addOutDeviceListener(2, wifiCommandTestNotifyListener)
            wifiCommandTestNotifyRegistered = true
            syncInfo("WiFiCmdTest", "Registered temporary notify listener (cmdType=2)")
        } catch (t: Throwable) {
            syncWarn("WiFiCmdTest", "Failed to register temporary notify listener: ${t.message}")
        }
    }

    private fun cleanupWifiCommandTest() {
        if (wifiCommandTestNotifyRegistered) {
            try {
                LargeDataHandler.getInstance().removeOutDeviceListener(2)
                syncInfo("WiFiCmdTest", "Unregistered temporary notify listener (cmdType=2)")
            } catch (t: Throwable) {
                syncWarn("WiFiCmdTest", "Failed to unregister temporary notify listener: ${t.message}")
            }
            wifiCommandTestNotifyRegistered = false
        }

        val manager = wifiCommandTestManager
        val callback = wifiCommandTestCallback
        if (manager != null && callback != null) {
            manager.removeCallback(callback)
        }
        try {
            manager?.unregisterReceiver()
        } catch (t: Throwable) {
            syncWarn("WiFiCmdTest", "Failed to unregister P2P receiver: ${t.message}")
        }
        wifiCommandTestManager = null
        wifiCommandTestCallback = null
    }

    private fun setWifiCommandTestStatus(status: String) {
        runOnUiThread {
            binding.tvWifiCommandTestStatus.text = "Wi-Fi Command Test: $status"
        }
    }

    private fun commandToHex(bytes: ByteArray): String =
        bytes.joinToString(separator = " ") { "%02X".format(it.toInt() and 0xff) }

    private fun scheduleNoPeerTimeout() {
        downloadPeerTimeoutJob?.cancel()
        downloadPeerTimeoutJob = CoroutineScope(Dispatchers.Main).launch {
            delay(45_000)
            if (!downloadCancelledByUser && !downloadP2pConnected) {
                showDownloadError(
                    "No WiFi Direct peer found. Keep Bluetooth connected, WiFi ON, Location ON, disable VPN, and try again.",
                    cleanup = true,
                )
            }
        }
    }

    private fun scheduleBleIpTimeout() {
        downloadBleIpTimeoutJob?.cancel()
        downloadBleIpTimeoutJob = CoroutineScope(Dispatchers.Main).launch {
            delay(45_000)
            if (!downloadCancelledByUser && downloadP2pConnected && downloadBleIp.isNullOrBlank() && bleIpBridge.ip.value.isNullOrBlank()) {
                showDownloadError(
                    "P2P connected but glasses IP was not reported over BLE.",
                    cleanup = true,
                )
            }
        }
    }

    private fun setTransferUiVisible(visible: Boolean) {
        binding.cardTransferProgress.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun resetTransferUiState() {
        transferTotalJpg = 0
        transferTotalMp4 = 0
        transferTotalOpus = 0
        transferDoneJpg = 0
        transferDoneMp4 = 0
        transferDoneOpus = 0

        binding.tvTransferCounts.text = "Photos: --  Videos: --  Audio: --"
        binding.progressTransfer.isIndeterminate = true
        binding.progressTransfer.max = 100
        binding.progressTransfer.progress = 0
        binding.tvTransferDetail.text = "Idle"
    }

    private fun setTransferPlan(jpg: Int, mp4: Int, opus: Int) {
        transferTotalJpg = jpg
        transferTotalMp4 = mp4
        transferTotalOpus = opus
        transferDoneJpg = 0
        transferDoneMp4 = 0
        transferDoneOpus = 0
        renderTransferProgress()
    }

    private fun onTransferItemDone(type: String) {
        when (type) {
            "jpg" -> transferDoneJpg++
            "mp4" -> transferDoneMp4++
            "opus" -> transferDoneOpus++
        }
        renderTransferProgress()
    }

    private fun renderTransferProgress() {
        val total = transferTotalJpg + transferTotalMp4 + transferTotalOpus
        val done = transferDoneJpg + transferDoneMp4 + transferDoneOpus

        binding.tvTransferCounts.text =
            "Photos: ${transferDoneJpg}/${transferTotalJpg}  Videos: ${transferDoneMp4}/${transferTotalMp4}  Audio: ${transferDoneOpus}/${transferTotalOpus}"

        if (total <= 0) {
            binding.progressTransfer.isIndeterminate = true
            binding.progressTransfer.max = 100
            binding.progressTransfer.progress = 0
        } else {
            binding.progressTransfer.isIndeterminate = false
            binding.progressTransfer.max = total
            binding.progressTransfer.progress = done.coerceAtMost(total)
        }
    }

    private fun setTransferDetail(text: String) {
        binding.tvTransferDetail.text = text
    }
    
    private fun getDeviceIpFromBLE(): String? {
        // Prefer IP detected from BLE notifications, fall back to the
        // known sample IP if we have not seen one yet.
        val ipFromBle = bleIpBridge.ip.value
        if (!ipFromBle.isNullOrEmpty()) {
            Log.i("DataDownload", "Device IP from BleIpBridge: $ipFromBle")
            return ipFromBle
        }
        // No safe fallback: the glasses IP varies per session.
        return null
    }
    
    private fun downloadMediaList(deviceIp: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Lock the device IP for the whole transfer session.
                downloadResolvedHttpIp = deviceIp

                withContext(Dispatchers.Main) {
                    binding.progressTransfer.isIndeterminate = true
                    setTransferDetail("Fetching media list...")
                }

                val content = fetchMediaConfigContent(deviceIp)

                if (content != null) {
                    syncInfo("DataDownload", "=== MEDIA CONFIG CONTENT PREVIEW ===")
                    syncInfo("DataDownload", content.take(2000))
                    syncInfo("DataDownload", "=== END MEDIA CONFIG PREVIEW ===")
                    parseMediaList(content, deviceIp)
                } else {
                    syncError("DataDownload", "Failed to download media list from all known endpoints.")
                    withContext(Dispatchers.Main) {
                        showDownloadError("Failed to download media list.")
                    }
                }
            } catch (e: Exception) {
                    syncError("DataDownload", "Error downloading media list: ${e.message}", e)
                    CoroutineScope(Dispatchers.Main).launch {
                        when (e) {
                            is java.io.IOException -> {
                                if (e.message?.contains("Cleartext HTTP traffic") == true) {
                                    showDownloadError("Network security blocked HTTP connection. Please check app settings.")
                                } else if (e.message?.contains("Failed to connect") == true) {
                                    showDownloadError("Cannot connect to glasses device. Please ensure P2P connection is established.")
                                } else {
                                    showDownloadError("Network error: ${e.message}")
                                }
                            }
                            else -> showDownloadError("Download failed: ${e.message}")
                        }
                    }
                }
            }
        }

    private fun mediaListUrls(deviceIp: String): List<URL> = listOf(
        URL("http://$deviceIp/files/media.config"),
        URL("http://$deviceIp/media.config"),
        URL("http://$deviceIp/manifest.json"),
    )

    private fun fetchMediaConfigContent(deviceIp: String): String? {
        for (url in mediaListUrls(deviceIp)) {
            syncInfo("DataDownload", "HTTP GET media list URL: $url")
            var content: String? = null
            val ok = httpGet(url, 10000, 30000) { stream, _ ->
                content = stream.bufferedReader().use { it.readText() }
            }
            if (ok && content != null) {
                syncInfo("DataDownload", "Media list success from $url (${content!!.length} chars)")
                markTransferProgressDetected("media.config reachable")
                recordTechnicalSyncRow(
                    action = "Fetch media.config",
                    uuid = "HTTP, not BLE",
                    command = "N/A",
                    response = "$url returned media list (${content!!.length} chars)",
                    status = "Confirmed",
                )
                return content
            }
            syncWarn("DataDownload", "Media list failed from $url")
        }
        recordTechnicalSyncRow(
            action = "Fetch media.config",
            uuid = "HTTP, not BLE",
            command = "N/A",
            response = "all media.config endpoints failed for $deviceIp",
            status = "Failed",
        )
        return null
    }

    private fun extractMediaEntries(content: String): List<String> {
        val lineEntries = content.trim().lines()
            .map { it.trim().trim('"', '\'', ',', '[', ']') }
            .filter { it.isNotBlank() }
        val directMedia = lineEntries.filter {
            it.endsWith(".jpg", ignoreCase = true) ||
                it.endsWith(".jpeg", ignoreCase = true) ||
                it.endsWith(".mp4", ignoreCase = true) ||
                it.endsWith(".opus", ignoreCase = true)
        }
        if (directMedia.isNotEmpty()) return directMedia

        val regex = Regex("""[A-Za-z0-9_./%-]+\.(?:jpg|jpeg|mp4|opus)""", RegexOption.IGNORE_CASE)
        return regex.findAll(content)
            .map { it.value.substringAfterLast('/') }
            .distinct()
            .toList()
    }

        private fun parseMediaList(content: String, deviceIp: String) {
            // Parse the media configuration file content - this is a text file containing media file names.
            Log.i("DataDownload", "Parsing media list content...")
            
            try {
                // Split by line, each line should be a file name
                val lines = extractMediaEntries(content)
                val jpgFiles = mutableListOf<String>()
                val mp4Files = mutableListOf<String>()
                val opusFiles = mutableListOf<String>()
                var otherFiles = 0
                
                lines.forEach { line ->
                    val trimmedLine = line.trim()
                    if (trimmedLine.isNotEmpty()) {
                        when {
                            trimmedLine.endsWith(".jpg", ignoreCase = true) ||
                                trimmedLine.endsWith(".jpeg", ignoreCase = true) -> {
                                jpgFiles.add(trimmedLine)
                                Log.i("DataDownload", "Found JPG file: $trimmedLine")
                            }

                            trimmedLine.endsWith(".mp4", ignoreCase = true) -> {
                                mp4Files.add(trimmedLine)
                                Log.i("DataDownload", "Found MP4 file: $trimmedLine")
                            }

                            trimmedLine.endsWith(".opus", ignoreCase = true) -> {
                                opusFiles.add(trimmedLine)
                                Log.i("DataDownload", "Found OPUS file: $trimmedLine")
                            }

                            else -> {
                                otherFiles++
                                Log.i("DataDownload", "Found other file: $trimmedLine")
                            }
                        }
                    }
                }

                Log.i(
                    "DataDownload",
                    "Media list parsed: jpg=${jpgFiles.size}, mp4=${mp4Files.size}, opus=${opusFiles.size}, other=$otherFiles"
                )

                CoroutineScope(Dispatchers.Main).launch {
                    setTransferPlan(jpgFiles.size, mp4Files.size, opusFiles.size)
                    val total = jpgFiles.size + mp4Files.size + opusFiles.size
                    setTransferDetail("Preparing downloads (0/$total)...")
                }

                if (jpgFiles.isEmpty() && mp4Files.isEmpty() && opusFiles.isEmpty()) {
                    Log.w("DataDownload", "No JPG/MP4/OPUS files found in media.config")
                    CoroutineScope(Dispatchers.Main).launch {
                        showDownloadError("No JPG/MP4/OPUS files found in media.config")
                    }
                    return
                }

                // Download everything we understand. Keep P2P bound until all downloads finish.
                downloadAllMediaFiles(jpgFiles, mp4Files, opusFiles, deviceIp)
                
            } catch (e: Exception) {
                Log.e("DataDownload", "Error parsing media list: ${e.message}", e)
                CoroutineScope(Dispatchers.Main).launch {
                    showDownloadError("Failed to parse media list: ${e.message}")
                }
            }
        }

    private fun downloadAllMediaFiles(
        jpgFiles: List<String>,
        mp4Files: List<String>,
        opusFiles: List<String>,
        deviceIp: String,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            syncInfo(
                "DataDownload",
                "Starting download: jpg=${jpgFiles.size}, mp4=${mp4Files.size}, opus=${opusFiles.size}"
            )

            val totalAll = jpgFiles.size + mp4Files.size + opusFiles.size
            withContext(Dispatchers.Main) {
                if (totalAll > 0) {
                    binding.progressTransfer.isIndeterminate = false
                }
                setTransferDetail("Downloading 0/$totalAll...")
            }
            
            var jpgSuccess = 0
            var jpgFail = 0
            var mp4Success = 0
            var mp4Fail = 0
            var opusSuccess = 0
            var opusFail = 0
            
            for ((index, fileName) in jpgFiles.withIndex()) {
                try {
                    withContext(Dispatchers.Main) {
                        setTransferDetail("Downloading photo ${index + 1}/${jpgFiles.size}...")
                    }
                    syncInfo("DataDownload", "Downloading photo ${index + 1}/${jpgFiles.size}: $fileName")

                    val success = downloadSingleJpgFileWithReconnect(fileName, deviceIp)
                    if (success) {
                        jpgSuccess++
                        Log.i("DataDownload", "✓ Successfully downloaded: $fileName")
                    } else {
                        jpgFail++
                        Log.e("DataDownload", "✗ Failed to download: $fileName")
                    }

                    withContext(Dispatchers.Main) {
                        onTransferItemDone("jpg")
                        setTransferDetail("Downloaded ${binding.progressTransfer.progress}/${binding.progressTransfer.max}")
                    }
                    
                    // Add a small delay to avoid excessively fast requests
                    delay(500)
                    
                } catch (e: Exception) {
                    jpgFail++
                    syncError("DataDownload", "Error downloading photo $fileName: ${e.message}", e)

                    withContext(Dispatchers.Main) {
                        onTransferItemDone("jpg")
                        setTransferDetail("Downloaded ${binding.progressTransfer.progress}/${binding.progressTransfer.max} (with errors)")
                    }
                }
            }

            for ((index, fileName) in mp4Files.withIndex()) {
                try {
                    withContext(Dispatchers.Main) {
                        setTransferDetail("Downloading video ${index + 1}/${mp4Files.size}...")
                    }
                    syncInfo("DataDownload", "Downloading video ${index + 1}/${mp4Files.size}: $fileName")

                    val success = downloadSingleMp4FileWithReconnect(fileName, deviceIp)
                    if (success) {
                        mp4Success++
                        Log.i("DataDownload", "✓ Successfully downloaded: $fileName")
                    } else {
                        mp4Fail++
                        Log.e("DataDownload", "✗ Failed to download: $fileName")
                    }

                    withContext(Dispatchers.Main) {
                        onTransferItemDone("mp4")
                        setTransferDetail("Downloaded ${binding.progressTransfer.progress}/${binding.progressTransfer.max}")
                    }

                    // Videos are larger; be gentler.
                    delay(800)
                } catch (e: Exception) {
                    mp4Fail++
                    syncError("DataDownload", "Error downloading video $fileName: ${e.message}", e)

                    withContext(Dispatchers.Main) {
                        onTransferItemDone("mp4")
                        setTransferDetail("Downloaded ${binding.progressTransfer.progress}/${binding.progressTransfer.max} (with errors)")
                    }
                }
            }

            for ((index, fileName) in opusFiles.withIndex()) {
                try {
                    withContext(Dispatchers.Main) {
                        setTransferDetail("Downloading audio ${index + 1}/${opusFiles.size}...")
                    }
                    syncInfo("DataDownload", "Downloading audio ${index + 1}/${opusFiles.size}: $fileName")

                    val success = downloadSingleOpusFileWithReconnect(fileName, deviceIp)
                    if (success) {
                        opusSuccess++
                        Log.i("DataDownload", "✓ Successfully downloaded: $fileName")
                    } else {
                        opusFail++
                        Log.e("DataDownload", "✗ Failed to download: $fileName")
                    }

                    withContext(Dispatchers.Main) {
                        onTransferItemDone("opus")
                        setTransferDetail("Downloaded ${binding.progressTransfer.progress}/${binding.progressTransfer.max}")
                    }

                    delay(500)
                } catch (e: Exception) {
                    opusFail++
                    syncError("DataDownload", "Error downloading audio $fileName: ${e.message}", e)

                    withContext(Dispatchers.Main) {
                        onTransferItemDone("opus")
                        setTransferDetail("Downloaded ${binding.progressTransfer.progress}/${binding.progressTransfer.max} (with errors)")
                    }
                }
            }
            
            // Show final result
            val totalSuccess = jpgSuccess + mp4Success + opusSuccess
            val totalFail = jpgFail + mp4Fail + opusFail
            syncInfo(
                "DataDownload",
                "Download completed: jpg=$jpgSuccess/${jpgFiles.size} ok, mp4=$mp4Success/${mp4Files.size} ok, opus=$opusSuccess/${opusFiles.size} ok, failed=$totalFail"
            )
            
            withContext(Dispatchers.Main) {
                if (totalFail == 0) {
                    showDownloadSuccess("All $totalSuccess files downloaded successfully!")
                } else {
                    showDownloadError("Download completed with errors: $totalSuccess successful, $totalFail failed")
                }
            }
        }
    }

    private fun mediaFileUrls(deviceIp: String, fileName: String): List<URL> = listOf(
        URL("http://$deviceIp/files/$fileName"),
        URL("http://$deviceIp/$fileName"),
    )

    private fun httpGetMediaFile(
        deviceIp: String,
        fileName: String,
        connectTimeoutMs: Int,
        readTimeoutMs: Int,
        onStream: (InputStream, Long) -> Unit,
    ): Boolean {
        for (url in mediaFileUrls(deviceIp, fileName)) {
            syncInfo("DataDownload", "HTTP GET file URL: $url")
            val ok = httpGet(url, connectTimeoutMs, readTimeoutMs, onStream)
            if (ok) {
                syncInfo("DataDownload", "File HTTP success: $url")
                return true
            }
            syncWarn("DataDownload", "File HTTP failed: $url")
        }
        return false
    }

    private suspend fun awaitDownloadRouteForItem(fileName: String, previousIp: String): String? {
        val preferred = downloadBleIp ?: bleIpBridge.ip.value ?: downloadResolvedHttpIp ?: previousIp
        if (!downloadPausedForReconnect && downloadP2pConnected && !preferred.isNullOrBlank()) {
            return preferred
        }

        downloadPausedForReconnect = true
        syncWarn("DataDownload", "P2P disconnected during download, pausing and reconnecting before $fileName")
        withContext(Dispatchers.Main) {
            setTransferDetail("P2P disconnected; reconnecting before $fileName...")
        }
        downloadWifiP2pManager?.discoverPeersStable()
        downloadWifiP2pManager?.startPeerDiscovery()

        val deadline = System.currentTimeMillis() + 60_000L
        var loggedResume = false
        while (coroutineContext.isActive && !downloadCancelledByUser && System.currentTimeMillis() < deadline) {
            if (downloadP2pConnected) {
                for (candidate in buildCandidateIps()) {
                    if (candidate.isBlank()) continue
                    if (mediaConfigOk(candidate, 2_000, logFailures = candidate == downloadBleIp)) {
                        downloadResolvedHttpIp = candidate
                        downloadPausedForReconnect = false
                        syncInfo("DataDownload", "Resuming downloads from remaining queue using IP=$candidate")
                        if (!loggedResume) {
                            recordTechnicalSyncRow(
                                action = "Resume download",
                                uuid = "HTTP, not BLE",
                                command = "N/A",
                                response = "verified route before $fileName",
                                status = "Resumed",
                            )
                            loggedResume = true
                        }
                        withContext(Dispatchers.Main) {
                            setTransferDetail("Reconnected; resuming downloads...")
                        }
                        return candidate
                    }
                }
            }
            delay(1_000)
        }

        recordTechnicalSyncRow(
            action = "Resume download",
            uuid = "HTTP, not BLE",
            command = "N/A",
            response = "timeout waiting for P2P route before $fileName",
            status = "Failed",
        )
        syncError("DataDownload", "Failed to resume download route before $fileName")
        return null
    }

    private suspend fun downloadSingleJpgFileWithReconnect(fileName: String, deviceIp: String): Boolean =
        downloadSingleFileWithReconnect(
            action = "Download Photo",
            fileName = fileName,
            deviceIp = deviceIp,
        ) { ip -> downloadSingleJpgFile(fileName, ip) }

    private suspend fun downloadSingleMp4FileWithReconnect(fileName: String, deviceIp: String): Boolean =
        downloadSingleFileWithReconnect(
            action = "Download Video",
            fileName = fileName,
            deviceIp = deviceIp,
        ) { ip -> downloadSingleMp4File(fileName, ip) }

    private suspend fun downloadSingleOpusFileWithReconnect(fileName: String, deviceIp: String): Boolean =
        downloadSingleFileWithReconnect(
            action = "Download Audio",
            fileName = fileName,
            deviceIp = deviceIp,
        ) { ip -> downloadSingleOpusFile(fileName, ip) }

    private suspend fun downloadSingleFileWithReconnect(
        action: String,
        fileName: String,
        deviceIp: String,
        downloader: suspend (String) -> Boolean,
    ): Boolean {
        val firstIp = awaitDownloadRouteForItem(fileName, deviceIp) ?: return false
        var success = downloader(firstIp)
        if (!success && (downloadPausedForReconnect || !downloadP2pConnected)) {
            syncWarn("DataDownload", "Retrying $fileName after P2P reconnect")
            val retryIp = awaitDownloadRouteForItem(fileName, firstIp) ?: return false
            success = downloader(retryIp)
        }
        if (success) {
            markTransferProgressDetected("file download succeeds")
        }
        return success
    }
    
    private suspend fun downloadSingleJpgFile(fileName: String, deviceIp: String): Boolean {
        return try {
            var saved: GallerySaveResult? = null
            httpGetMediaFile(deviceIp, fileName, 10000, 30000) { stream, _ ->
                val takenMs = parseTakenTimeMillisFromFilename(fileName) ?: System.currentTimeMillis()
                saved = saveJpegToGallery(stream, fileName, takenMs)
            }

            if (saved != null && saved!!.bytes > 0) {
                Log.i("DataDownload", "File downloaded: $fileName (${saved!!.bytes} bytes)")
            }
            if (saved != null && saved!!.success) {
                syncInfo("DataDownload", "Saved JPG to gallery: name=$fileName uri=${saved!!.uri} bytes=${saved!!.bytes}")
                recordTechnicalSyncRow(
                    action = "Download Photo",
                    uuid = "HTTP, not BLE",
                    command = "N/A",
                    response = "file=$fileName, ip=$deviceIp, bytes=${saved!!.bytes}, uri=${saved!!.uri}",
                    status = "Success",
                )
                true
            } else {
                syncError("DataDownload", "Failed to download/save JPG: $fileName")
                recordTechnicalSyncRow(
                    action = "Download Photo",
                    uuid = "HTTP, not BLE",
                    command = "N/A",
                    response = "file=$fileName, ip=$deviceIp",
                    status = "Failed",
                )
                false
            }
        } catch (e: Exception) {
            syncError("DataDownload", "Error downloading JPG $fileName: ${e.message}", e)
            false
        }
    }

    private suspend fun downloadSingleMp4File(fileName: String, deviceIp: String): Boolean {
        return try {
            var saved: GallerySaveResult? = null
            httpGetMediaFile(deviceIp, fileName, 15000, 180000) { stream, _ ->
                val takenMs = parseTakenTimeMillisFromFilename(fileName) ?: System.currentTimeMillis()
                saved = saveMp4ToGallery(stream, fileName, takenMs)
            }

            if (saved != null && saved!!.bytes > 0) {
                Log.i("DataDownload", "File downloaded: $fileName (${saved!!.bytes} bytes)")
            }
            if (saved != null && saved!!.success) {
                syncInfo("DataDownload", "Saved MP4 to gallery: name=$fileName uri=${saved!!.uri} bytes=${saved!!.bytes}")
                recordTechnicalSyncRow(
                    action = "Download Video",
                    uuid = "HTTP, not BLE",
                    command = "N/A",
                    response = "file=$fileName, ip=$deviceIp, bytes=${saved!!.bytes}, uri=${saved!!.uri}",
                    status = "Success",
                )
                true
            } else {
                syncError("DataDownload", "Failed to download/save MP4: $fileName")
                recordTechnicalSyncRow(
                    action = "Download Video",
                    uuid = "HTTP, not BLE",
                    command = "N/A",
                    response = "file=$fileName, ip=$deviceIp",
                    status = "Failed",
                )
                false
            }
        } catch (e: Exception) {
            syncError("DataDownload", "Error downloading MP4 $fileName: ${e.message}", e)
            false
        }
    }

    private suspend fun downloadSingleOpusFile(fileName: String, deviceIp: String): Boolean {
        return try {
            var saved: GallerySaveResult? = null
            var payloadBytes: ByteArray? = null
            var rawBytesSize = 0
            var payloadNote = "raw"
            val takenMs = parseTakenTimeMillisFromFilename(fileName) ?: System.currentTimeMillis()
            httpGetMediaFile(deviceIp, fileName, 15000, 120000) { stream, _ ->
                val rawBytes = readAllBytes(stream)
                rawBytesSize = rawBytes.size
                val wrapped = wrapOpusIfNeeded(rawBytes)
                payloadBytes = wrapped.first
                payloadNote = wrapped.second
                saved = saveOpusToLibrary(
                    payloadBytes = wrapped.first,
                    rawBytesSize = rawBytes.size,
                    payloadNote = wrapped.second,
                    displayName = fileName,
                    takenTimeMs = takenMs,
                )
            }

            if (saved != null && saved!!.bytes > 0) {
                Log.i("DataDownload", "File downloaded: $fileName (${saved!!.bytes} bytes)")
            }
            if (saved != null && saved!!.success) {
                payloadBytes?.let { bytes ->
                    runCatching {
                        val persisted = GlassesSyncedAudioIngestor.persistDownloadedAudio(
                            context = applicationContext,
                            displayName = fileName,
                            payloadBytes = bytes,
                            takenTimeMs = takenMs,
                        )
                        if (persisted.createdSessionId != null) {
                            Log.i(
                                "DataDownload",
                                "Synced audio persisted for recordings/transcription: sessionId=${persisted.createdSessionId} path=${persisted.localPath}"
                            )
                        }
                    }.onFailure {
                        Log.e("DataDownload", "Failed to persist synced audio session for $fileName: ${it.message}", it)
                    }
                }
                Log.i("DataDownload", "Saved to library: name=$fileName uri=${saved!!.uri}")
                recordTechnicalSyncRow(
                    action = "Download Audio",
                    uuid = "HTTP, not BLE",
                    command = "N/A",
                    response = "file=$fileName, ip=$deviceIp, rawBytes=$rawBytesSize, savedBytes=${saved!!.bytes}, uri=${saved!!.uri}, mode=$payloadNote",
                    status = "Success",
                )
                true
            } else {
                Log.e("DataDownload", "Failed to download/save: $fileName (raw=$rawBytesSize mode=$payloadNote)")
                recordTechnicalSyncRow(
                    action = "Download Audio",
                    uuid = "HTTP, not BLE",
                    command = "N/A",
                    response = "file=$fileName, ip=$deviceIp, rawBytes=$rawBytesSize, mode=$payloadNote",
                    status = "Failed",
                )
                false
            }
        } catch (e: Exception) {
            Log.e("DataDownload", "Error downloading $fileName: ${e.message}", e)
            false
        }
    }
    
    private data class GallerySaveResult(
        val success: Boolean,
        val uri: String?,
        val bytes: Long,
    )

    private fun parseTakenTimeMillisFromFilename(fileName: String): Long? {
        // The glasses filenames look like: yyyyMMddHHmmssSSS?.jpg
        // Example: 20260127095159018.jpg
        val digits = fileName.takeWhile { it.isDigit() }
        if (digits.length < 14) return null

        return try {
            val base = digits.substring(0, 14)
            val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
            val baseDate = sdf.parse(base) ?: return null
            val msPart = digits.substring(14).take(3)
            val extraMs = msPart.toIntOrNull() ?: 0
            baseDate.time + extraMs
        } catch (_: Exception) {
            null
        }
    }

    private fun saveJpegToGallery(input: InputStream, displayName: String, takenTimeMs: Long): GallerySaveResult {
        return try {
            val resolver = contentResolver

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_TAKEN, takenTimeMs)
                put(MediaStore.Images.Media.DATE_ADDED, takenTimeMs / 1000)
                put(MediaStore.Images.Media.DATE_MODIFIED, takenTimeMs / 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, SyncedMediaFolder.relativePath)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return GallerySaveResult(false, null, 0)

            var bytes = 0L
            try {
                resolver.openOutputStream(uri, "w")?.use { out ->
                    val buffer = ByteArray(8 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        out.write(buffer, 0, read)
                        bytes += read
                    }
                    out.flush()
                } ?: run {
                    resolver.delete(uri, null, null)
                    return GallerySaveResult(false, null, bytes)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                    resolver.update(uri, done, null, null)
                } else {
                    // Pre-Android 10: some galleries need an explicit media scan.
                    MediaScannerConnection.scanFile(
                        this,
                        arrayOf(uri.toString()),
                        arrayOf("image/jpeg"),
                        null
                    )
                }

                GallerySaveResult(true, uri.toString(), bytes)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                Log.e("DataDownload", "Gallery write failed for $displayName: ${e.message}", e)
                GallerySaveResult(false, uri.toString(), bytes)
            }
        } catch (e: Exception) {
            Log.e("DataDownload", "saveJpegToGallery failed for $displayName: ${e.message}", e)
            GallerySaveResult(false, null, 0)
        }
    }

    private fun saveMp4ToGallery(input: InputStream, displayName: String, takenTimeMs: Long): GallerySaveResult {
        return try {
            val resolver = contentResolver

            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.DATE_TAKEN, takenTimeMs)
                put(MediaStore.Video.Media.DATE_ADDED, takenTimeMs / 1000)
                put(MediaStore.Video.Media.DATE_MODIFIED, takenTimeMs / 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Keep videos in the same DCIM/CyanBridge folder as photos.
                    put(MediaStore.Video.Media.RELATIVE_PATH, SyncedMediaFolder.relativePath)
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: return GallerySaveResult(false, null, 0)

            var bytes = 0L
            try {
                resolver.openOutputStream(uri, "w")?.use { out ->
                    val buffer = ByteArray(128 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        out.write(buffer, 0, read)
                        bytes += read
                    }
                    out.flush()
                } ?: run {
                    resolver.delete(uri, null, null)
                    return GallerySaveResult(false, null, bytes)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val done = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
                    resolver.update(uri, done, null, null)
                }

                GallerySaveResult(true, uri.toString(), bytes)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                Log.e("DataDownload", "Gallery video write failed for $displayName: ${e.message}", e)
                GallerySaveResult(false, uri.toString(), bytes)
            }
        } catch (e: Exception) {
            Log.e("DataDownload", "saveMp4ToGallery failed for $displayName: ${e.message}", e)
            GallerySaveResult(false, null, 0)
        }
    }

    private fun saveOpusToLibrary(
        payloadBytes: ByteArray,
        rawBytesSize: Int,
        payloadNote: String,
        displayName: String,
        takenTimeMs: Long,
    ): GallerySaveResult {
        return try {
            val resolver = contentResolver

            val headHex = bytesToHex(payloadBytes, 24)
            Log.i(
                "DataDownload",
                "OPUS save: name=$displayName, raw=$rawBytesSize bytes, out=${payloadBytes.size} bytes, mode=$payloadNote, head=$headHex"
            )

            val title = displayName.substringBeforeLast('.', displayName)
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
                // Use Ogg/Opus container when possible.
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/ogg")
                put(MediaStore.Audio.Media.TITLE, title)
                put(MediaStore.Audio.Media.IS_MUSIC, 0)
                put(MediaStore.MediaColumns.DATE_ADDED, takenTimeMs / 1000)
                put(MediaStore.MediaColumns.DATE_MODIFIED, takenTimeMs / 1000)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Keep alongside photos/videos per your preference (DCIM/CyanBridge).
                    put(MediaStore.MediaColumns.RELATIVE_PATH, SyncedMediaFolder.relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                ?: return GallerySaveResult(false, null, 0)

            var bytes = 0L
            try {
                resolver.openOutputStream(uri, "w")?.use { out ->
                    out.write(payloadBytes)
                    bytes = payloadBytes.size.toLong()
                    out.flush()
                } ?: run {
                    resolver.delete(uri, null, null)
                    return GallerySaveResult(false, null, bytes)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                    resolver.update(uri, done, null, null)
                }

                GallerySaveResult(true, uri.toString(), bytes)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                Log.e("DataDownload", "Gallery audio write failed for $displayName: ${e.message}", e)
                GallerySaveResult(false, uri.toString(), bytes)
            }
        } catch (e: Exception) {
            Log.e("DataDownload", "saveOpusToLibrary failed for $displayName: ${e.message}", e)
            GallerySaveResult(false, null, 0)
        }
    }

    private fun readAllBytes(input: InputStream): ByteArray {
        val bos = ByteArrayOutputStream()
        val buffer = ByteArray(32 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            bos.write(buffer, 0, read)
        }
        return bos.toByteArray()
    }

    private fun bytesToHex(bytes: ByteArray, max: Int): String {
        val n = minOf(bytes.size, max)
        val sb = StringBuilder(n * 2)
        for (i in 0 until n) {
            sb.append(String.format("%02x", bytes[i]))
        }
        if (bytes.size > max) sb.append("...")
        return sb.toString()
    }

    private fun wrapOpusIfNeeded(raw: ByteArray): Pair<ByteArray, String> {
        if (raw.size >= 4 && raw[0].toInt() == 'O'.code && raw[1].toInt() == 'g'.code && raw[2].toInt() == 'g'.code && raw[3].toInt() == 'S'.code) {
            return raw to "ogg-already"
        }

        // Try to interpret the file as a sequence of length-prefixed Opus packets and wrap
        // them into a proper Ogg/Opus container so standard players (VLC) can open it.
        val packets = parseLengthPrefixedPackets(raw, littleEndian = true)
            ?: parseLengthPrefixedPackets(raw, littleEndian = false)
            ?: parseLengthPrefixedPackets1B(raw)
            ?: guessFixedSizePackets(raw)

        if (packets == null || packets.isEmpty()) {
            // Unknown/proprietary layout (the official app decodes these with jl_opus).
            return raw to "raw-unwrapped"
        }

        return try {
            val ogg = buildOggOpusFromPackets(packets, packetDurationMs = 40)
            ogg to "wrapped packets=${packets.size}"
        } catch (e: Exception) {
            Log.w("DataDownload", "Failed to wrap opus into ogg: ${e.message}")
            raw to "raw-unwrapped"
        }
    }

    private fun parseLengthPrefixedPackets(raw: ByteArray, littleEndian: Boolean): List<ByteArray>? {
        // Heuristic: repeated [u16 len][len bytes]...
        var i = 0
        val out = ArrayList<ByteArray>()
        while (i + 2 <= raw.size) {
            val b0 = raw[i].toInt() and 0xFF
            val b1 = raw[i + 1].toInt() and 0xFF
            val len = if (littleEndian) (b0 or (b1 shl 8)) else ((b0 shl 8) or b1)
            i += 2
            if (len <= 0 || len > 2000) return null
            if (i + len > raw.size) return null
            out.add(raw.copyOfRange(i, i + len))
            i += len
        }
        if (i != raw.size) return null
        // Require a few packets so we don't mis-detect.
        return if (out.size >= 3) out else null
    }

    private fun parseLengthPrefixedPackets1B(raw: ByteArray): List<ByteArray>? {
        // Heuristic: repeated [u8 len][len bytes]...
        var i = 0
        val out = ArrayList<ByteArray>()
        while (i + 1 <= raw.size) {
            val len = raw[i].toInt() and 0xFF
            i += 1
            if (len <= 0 || len > 255) return null
            if (i + len > raw.size) return null
            out.add(raw.copyOfRange(i, i + len))
            i += len
        }
        if (i != raw.size) return null
        return if (out.size >= 3) out else null
    }

    private fun guessFixedSizePackets(raw: ByteArray): List<ByteArray>? {
        // Last-resort heuristic: some devices store raw Opus packets back-to-back with a
        // fixed packet byte size. Try a few common sizes.
        if (raw.isEmpty()) return null
        // 40 bytes is especially common for these glasses (official app uses packetSize=40).
        val candidates = intArrayOf(40, 60, 80, 100, 120, 160, 200, 240, 320)
        for (size in candidates) {
            if (size <= 0) continue
            if (raw.size % size != 0) continue
            val count = raw.size / size
            if (count < 5) continue
            val out = ArrayList<ByteArray>(count)
            var i = 0
            while (i < raw.size) {
                out.add(raw.copyOfRange(i, i + size))
                i += size
            }
            return out
        }
        return null
    }

    private fun buildOggOpusFromPackets(packets: List<ByteArray>, packetDurationMs: Int): ByteArray {
        // Ogg/Opus expects OpusHead + OpusTags packets before audio packets.
        val serial = SecureRandom().nextInt()
        var seq = 0
        var granulePos: Long = 0

        val out = ByteArrayOutputStream()

        val opusHead = buildOpusHead(channels = 1, preSkip = 0)
        val opusTags = buildOpusTags(vendor = "CyanBridge")

        // Header pages
        writeOggPage(out, serial, seq++, granulePosition = 0, headerType = 0x02, packets = listOf(opusHead))
        writeOggPage(out, serial, seq++, granulePosition = 0, headerType = 0x00, packets = listOf(opusTags))

        // Audio pages
        val samplesPerPacket48k = (packetDurationMs * 48_000L) / 1000L
        val maxSegments = 255
        var idx = 0
        while (idx < packets.size) {
            val pagePackets = ArrayList<ByteArray>()
            var segCount = 0
            var localGranule = granulePos

            while (idx < packets.size) {
                val p = packets[idx]
                var neededSeg = (p.size + 254) / 255
                if (p.size % 255 == 0) neededSeg += 1
                if (segCount + neededSeg > maxSegments) break
                pagePackets.add(p)
                segCount += neededSeg
                localGranule += samplesPerPacket48k
                idx++
            }

            granulePos = localGranule
            val isLast = idx >= packets.size
            val headerType = if (isLast) 0x04 else 0x00
            writeOggPage(out, serial, seq++, granulePosition = granulePos, headerType = headerType, packets = pagePackets)
        }

        return out.toByteArray()
    }

    private fun buildOpusHead(channels: Int, preSkip: Int): ByteArray {
        // OpusHead (19 bytes)
        val b = ByteArrayOutputStream()
        b.write("OpusHead".toByteArray(Charsets.US_ASCII))
        b.write(1) // version
        b.write(channels and 0xFF)
        // pre-skip LE16
        b.write(preSkip and 0xFF)
        b.write((preSkip shr 8) and 0xFF)
        // input sample rate LE32 (Opus is coded at 48k internally)
        val sr = 48_000
        b.write(sr and 0xFF)
        b.write((sr shr 8) and 0xFF)
        b.write((sr shr 16) and 0xFF)
        b.write((sr shr 24) and 0xFF)
        // output gain LE16
        b.write(0)
        b.write(0)
        // channel mapping family (0 = mono/stereo)
        b.write(0)
        return b.toByteArray()
    }

    private fun buildOpusTags(vendor: String): ByteArray {
        val vendorBytes = vendor.toByteArray(Charsets.UTF_8)
        val b = ByteArrayOutputStream()
        b.write("OpusTags".toByteArray(Charsets.US_ASCII))
        writeLe32(b, vendorBytes.size)
        b.write(vendorBytes)
        // user comment list length = 0
        writeLe32(b, 0)
        return b.toByteArray()
    }

    private fun writeLe32(out: ByteArrayOutputStream, v: Int) {
        out.write(v and 0xFF)
        out.write((v shr 8) and 0xFF)
        out.write((v shr 16) and 0xFF)
        out.write((v shr 24) and 0xFF)
    }

    private fun writeOggPage(
        out: ByteArrayOutputStream,
        serial: Int,
        seq: Int,
        granulePosition: Long,
        headerType: Int,
        packets: List<ByteArray>,
    ) {
        val segmentTable = ByteArrayOutputStream()
        val payload = ByteArrayOutputStream()

        for (p in packets) {
            var remaining = p.size
            var offset = 0
            while (remaining > 0) {
                val seg = minOf(255, remaining)
                segmentTable.write(seg)
                payload.write(p, offset, seg)
                offset += seg
                remaining -= seg
            }
            if (p.size % 255 == 0) {
                // Lacing: 255 indicates continuation; add 0 to terminate packet exactly on boundary.
                segmentTable.write(0)
            }
        }

        val segBytes = segmentTable.toByteArray()
        if (segBytes.size > 255) {
            throw IllegalStateException("Ogg page has too many segments: ${segBytes.size}")
        }
        val payloadBytes = payload.toByteArray()

        val header = ByteArrayOutputStream()
        header.write("OggS".toByteArray(Charsets.US_ASCII))
        header.write(0) // version
        header.write(headerType and 0xFF)
        writeLe64(header, granulePosition)
        writeLe32(header, serial)
        writeLe32(header, seq)
        // checksum placeholder
        writeLe32(header, 0)
        header.write(segBytes.size)
        header.write(segBytes)

        val pageBytes = header.toByteArray() + payloadBytes
        val crc = oggCrc(pageBytes)

        // Patch checksum at byte offset 22 (from start of OggS)
        pageBytes[22] = (crc and 0xFF).toByte()
        pageBytes[23] = ((crc shr 8) and 0xFF).toByte()
        pageBytes[24] = ((crc shr 16) and 0xFF).toByte()
        pageBytes[25] = ((crc shr 24) and 0xFF).toByte()

        out.write(pageBytes)
    }

    private fun writeLe64(out: ByteArrayOutputStream, v: Long) {
        out.write((v and 0xFF).toInt())
        out.write(((v shr 8) and 0xFF).toInt())
        out.write(((v shr 16) and 0xFF).toInt())
        out.write(((v shr 24) and 0xFF).toInt())
        out.write(((v shr 32) and 0xFF).toInt())
        out.write(((v shr 40) and 0xFF).toInt())
        out.write(((v shr 48) and 0xFF).toInt())
        out.write(((v shr 56) and 0xFF).toInt())
    }

    private val oggCrcTable: IntArray = run {
        val table = IntArray(256)
        for (i in 0 until 256) {
            var r = i shl 24
            for (j in 0 until 8) {
                r = if ((r and 0x80000000.toInt()) != 0) {
                    (r shl 1) xor 0x04C11DB7
                } else {
                    r shl 1
                }
            }
            table[i] = r
        }
        table
    }

    private fun oggCrc(data: ByteArray): Int {
        var crc = 0
        for (b in data) {
            val idx = ((crc ushr 24) xor (b.toInt() and 0xFF)) and 0xFF
            crc = (crc shl 8) xor oggCrcTable[idx]
        }
        return crc
    }
    
    private fun teardownDownloadP2pSession(sendExitTransfer: Boolean, hideTransferUi: Boolean) {
        downloadAttemptJob?.cancel()
        downloadAttemptJob = null
        downloadPeerTimeoutJob?.cancel()
        downloadPeerTimeoutJob = null
        downloadBleIpTimeoutJob?.cancel()
        downloadBleIpTimeoutJob = null
        unbindProcessFromNetwork()

        if (hideTransferUi) {
            runOnUiThread {
                setTransferUiVisible(false)
                resetTransferUiState()
            }
        }

        // Stop receiving download-mode notify frames.
        if (downloadNotifyListenerRegistered) {
            try {
                LargeDataHandler.getInstance().removeOutDeviceListener(2)
                downloadNotifyListenerRegistered = false
                syncInfo("DataDownload", "Unregistered download notify listener (cmdType=2)")
            } catch (e: Exception) {
                syncWarn("DataDownload", "Failed to unregister download notify listener", e)
            }
        }

        if (sendExitTransfer) {
            // Tell the glasses to exit transfer mode (official app does this after downloads finish).
            try {
                LargeDataHandler.getInstance().glassesControl(
                    byteArrayOf(0x02, 0x01, 0x09)
                ) { _, resp ->
                    syncInfo(
                        "DataDownload",
                        "glassesControl[0x02,0x01,0x09] -> dataType=${resp.dataType}, error=${resp.errorCode}"
                    )
                }
            } catch (e: Exception) {
                syncWarn("DataDownload", "Failed to send exit-transfer command [0x02,0x01,0x09]", e)
            }
        }

        val manager = downloadWifiP2pManager
        val callback = downloadWifiP2pCallback
        if (manager != null && callback != null) {
            manager.removeCallback(callback)
        }

        // Mirror official app: cancel the P2P connection as part of cleanup.
        manager?.cancelP2pConnection()

        manager?.removeGroup { success ->
            syncInfo("DataDownload", "P2P group removed: $success")
        }
        manager?.unregisterReceiver()
        downloadWifiP2pManager = null
        downloadWifiP2pCallback = null
        downloadP2pConnected = false
        downloadInProgress = false
        downloadP2pNetwork = null
        downloadResolvedHttpIp = null
    }

    private fun cleanupP2pAfterDownload() {
        teardownDownloadP2pSession(
            sendExitTransfer = true,
            hideTransferUi = true,
        )
    }

    private fun cancelDataDownloadAttempt(reason: String, showToast: Boolean) {
        syncInfo("DataDownload", reason)
        downloadCancelledByUser = true
        setTransferDetail("Stopping sync...")
        teardownDownloadP2pSession(
            sendExitTransfer = true,
            hideTransferUi = true,
        )
        if (showToast) {
            Toast.makeText(this, reason, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDownloadSuccess(message: String) {
        cleanupP2pAfterDownload()
        syncInfo("DataDownload", "SUCCESS: $message")
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    private fun showDownloadError(message: String, cleanup: Boolean = true) {
        if (cleanup) {
            cleanupP2pAfterDownload()
        }
        syncError("DataDownload", "ERROR: $message")
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun isProbablyGroupOwnerIp(ip: String?): Boolean {
        if (ip.isNullOrBlank()) return false

        // If the phone is not the group owner, then we shouldn't block the group owner IP (.1)
        // because it belongs to the glasses.
        if (!downloadPhoneIsGroupOwner) return false

        // Typical Wi‑Fi Direct GO address when phone is GO.
        return ip == "192.168.49.1"
    }

    private fun ipv4Prefix24(ip: String?): String? {
        if (ip.isNullOrBlank()) return null
        val parts = ip.split(".")
        if (parts.size != 4) return null
        return "${parts[0]}.${parts[1]}.${parts[2]}."
    }

    private fun guessDownloadSubnetPrefix(): String? {
        // Prefer authoritative device IPs when available; otherwise fall back to
        // the group owner's subnet and finally the active Wi‑Fi/P2P interface subnet.
        ipv4Prefix24(downloadBleIp)?.let { return it }
        ipv4Prefix24(bleIpBridge.ip.value)?.let { return it }
        ipv4Prefix24(downloadWifiIp)?.let { return it }

        val network = downloadP2pNetwork ?: findLikelyP2pNetwork()
        if (network != null) {
            try {
                val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val lp = cm.getLinkProperties(network)
                val addr = lp?.linkAddresses
                    ?.mapNotNull { it.address.hostAddress }
                    ?.firstOrNull { it.count { ch -> ch == '.' } == 3 }
                ipv4Prefix24(addr)?.let { return it }
            } catch (_: Exception) {
                // ignore
            }
        }
        return null
    }

    private fun buildCandidateIps(): List<String> {
        val set = LinkedHashSet<String>()

        downloadBleIp?.let { set.add(it) }
        bleIpBridge.ip.value?.let { set.add(it) }

        if (!downloadPhoneIsGroupOwner && downloadWifiIp != null) {
            set.add(downloadWifiIp!!)
        } else {
            downloadWifiIp?.let { set.add(it) }
        }

        guessDownloadSubnetPrefix()?.let { prefix ->
            set.add("${prefix}1") // Glasses might be the group owner
            set.add("${prefix}79")
            set.add("${prefix}2")
            set.add("${prefix}3")
        }

        return set.toList()
    }

    private fun isPortOpen(ip: String, port: Int, timeoutMs: Int): Boolean {
        // Standard path: use P2P network's socket factory.
        try {
            val factory = downloadP2pNetwork?.socketFactory ?: javax.net.SocketFactory.getDefault()
            factory.createSocket().use { s ->
                s.connect(InetSocketAddress(ip, port), timeoutMs)
                return true
            }
        } catch (_: Exception) {}

        // VPN fallback: bind socket to P2P local address to bypass VPN routing.
        val p2pAddr = p2pLocalAddress() ?: return false
        return try {
            Socket().use { s ->
                s.bind(InetSocketAddress(p2pAddr, 0))
                s.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (_: Exception) { false }
    }

    private fun mediaConfigOk(ip: String, timeoutMs: Int, logFailures: Boolean = false): Boolean {
        if (!isPortOpen(ip, 80, (timeoutMs / 2).coerceAtLeast(400))) {
            if (logFailures) {
                syncWarn("DataDownload", "media.config probe skipped for $ip (port 80 closed/unreachable)")
            }
            return false
        }
        for (url in mediaListUrls(ip)) {
            if (logFailures) {
                syncInfo("DataDownload", "Probing media list endpoint: $url")
            }
            if (httpGet(url, timeoutMs, timeoutMs)) {
                return true
            }
        }
        if (logFailures) {
            syncWarn("DataDownload", "media.config probe failed for all endpoints on $ip")
        }
        return false
    }

    private suspend fun discoverGlassesIpByScan(prefix: String = "192.168.49."): String? {
        // Fast scan for an HTTP server on port 80 in the P2P subnet.
        // Concurrency is limited to avoid overwhelming the device/network stack.
        return supervisorScope {
            val sem = Semaphore(32)
            val connectTimeoutMs = 300
            val verifyTimeoutMs = 1200
            val found = CompletableDeferred<String?>()
            val firstOpenPortIp = AtomicReference<String?>(null)

            for (host in 1..254) {
                val ip = "$prefix$host"
                if (downloadPhoneIsGroupOwner && ip == "192.168.49.1") continue
                launch(Dispatchers.IO) {
                    sem.withPermit {
                        if (found.isCompleted) return@withPermit
                        if (isPortOpen(ip, 80, connectTimeoutMs)) {
                            firstOpenPortIp.compareAndSet(null, ip)
                            // Prefer an IP that actually serves media.config.
                            if (mediaConfigOk(ip, verifyTimeoutMs)) {
                                found.complete(ip)
                            }
                        }
                    }
                }
            }

            val res = withTimeoutOrNull(20_000L) { found.await() } ?: firstOpenPortIp.get()
            coroutineContext.cancelChildren()
            res
        }
    }

    /**
     * Debug helper: log all methods on LargeDataHandler so we can
     * discover additional SDK capabilities (such as WiFi transfer APIs)
     * without needing decompiled sources.
     */
    private fun logLargeDataHandlerMethodsOnce() {
        if (loggedLargeDataHandlerMethods) return
        loggedLargeDataHandlerMethods = true
        try {
            val clazz = LargeDataHandler.getInstance()::class.java
            val methods = clazz.declaredMethods
            for (m in methods) {
                val params = m.parameterTypes.joinToString(",") { it.simpleName ?: it.name }
                val ret = m.returnType.simpleName ?: m.returnType.name
                Log.i("LDHMethods", "method=${m.name}, params=($params), return=$ret")
            }
        } catch (e: Exception) {
            Log.e("LDHMethods", "Failed to introspect LargeDataHandler methods", e)
        }
    }

    private fun testConnection(deviceIp: String): Boolean {
        Log.i("DataDownload", "Testing connection to $deviceIp...")
        val url = URL("http://$deviceIp/files/media.config")
        var bytesRead = 0
        val ok = httpGet(url, 5000, 5000) { stream, _ ->
            val buffer = ByteArray(1024)
            bytesRead = stream.read(buffer)
            stream.close()
        }
        if (ok) {
            Log.i("DataDownload", "Connection test successful - read $bytesRead bytes")
        } else {
            Log.e("DataDownload", "Connection test failed for $deviceIp")
        }
        return ok
    }

    private fun onDownloadBleIp(ip: String) {
        val now = System.currentTimeMillis()
        if (ip == downloadBleIp && (now - lastDownloadBleIpAtMs) < 1200L) {
            syncInfo("DataDownload", "Ignoring duplicate BLE IP report: $ip")
            return
        }
        lastDownloadBleIpAtMs = now
        downloadBleIpTimeoutJob?.cancel()
        markTransferProgressDetected("BLE Wi-Fi IP received")
        syncInfo("DataDownload", "Using BLE-reported dynamic IP: $ip")
        recordTechnicalSyncRow(
            action = "Receive glasses Wi-Fi IP",
            uuid = bleNotifyUuidLabel(),
            command = "N/A",
            response = ip,
            status = "Confirmed",
        )
        downloadBleIp = ip

        // If we're stuck scanning/probing without a good route, restart the resolver now that
        // we have the authoritative device IP from BLE.
        if (downloadAttemptJob?.isActive == true && !downloadInProgress) {
            syncInfo("DataDownload", "New BLE IP arrived; restarting HTTP resolver")
            downloadAttemptJob?.cancel()
            downloadAttemptJob = null
        }
        maybeStartHttpDownload("BLE")
    }

    private fun onDownloadP2pConnected(info: WifiP2pInfo) {
        downloadP2pConnected = info.groupFormed
        downloadWifiIp = info.groupOwnerAddress?.hostAddress
        downloadPhoneIsGroupOwner = info.isGroupOwner
        downloadP2pNetwork = findLikelyP2pNetwork()
        bindProcessToNetwork(downloadP2pNetwork)
        downloadPeerTimeoutJob?.cancel()
        markTransferProgressDetected("P2P connected")
        recordTechnicalSyncRow(
            action = "P2P connected",
            uuid = "Wi-Fi Direct, not BLE",
            command = "N/A",
            response = "groupFormed=${info.groupFormed}, isGroupOwner=${info.isGroupOwner}, groupOwnerIp=$downloadWifiIp",
            status = if (info.groupFormed) "Confirmed" else "Failed",
        )
        if (downloadBleIp.isNullOrBlank() && bleIpBridge.ip.value.isNullOrBlank()) {
            scheduleBleIpTimeout()
        }
        syncInfo(
            "DataDownload",
            "onDownloadP2pConnected: p2pConnected=$downloadP2pConnected, isGroupOwner=${info.isGroupOwner}, groupOwnerIp=$downloadWifiIp"
        )
        maybeStartHttpDownload("P2P")
    }

    private fun maybeStartHttpDownload(source: String) {
        if (downloadCancelledByUser) {
            syncInfo("DataDownload", "Ignoring HTTP start trigger from $source after user stop")
            return
        }
        if (downloadInProgress || downloadAttemptJob?.isActive == true) {
            syncInfo("DataDownload", "Download already in progress, ignoring trigger from $source")
            return
        }

        if (!downloadP2pConnected) {
            syncInfo("DataDownload", "Ignoring HTTP start trigger from $source; P2P not connected yet")
            return
        }

        val hasDeviceIp = !downloadBleIp.isNullOrBlank() || !bleIpBridge.ip.value.isNullOrBlank()
        if (!hasDeviceIp) {
            setTransferDetail("Waiting for BLE-reported glasses IP...")
            syncInfo("DataDownload", "Ignoring HTTP start trigger from $source; waiting for device IP notify")
            return
        }

        val bridgeIp = bleIpBridge.ip.value
        syncInfo(
            "DataDownload",
            "HTTP start trigger from $source. p2p=$downloadP2pConnected, bleIp=$downloadBleIp, groupOwnerIp=$downloadWifiIp, bleBridgeIp=$bridgeIp"
        )

        downloadAttemptJob = CoroutineScope(Dispatchers.IO).launch {
            // Official app waits briefly after both P2P+BLE-IP signals before fetching media.config.
            delay(1000)

            val startMs = System.currentTimeMillis()
            val overallTimeoutMs = 45_000L
            var lastStatusLogMs = 0L
            var didSubnetScan = false

            while (isActive && System.currentTimeMillis() - startMs < overallTimeoutMs) {
                val now = System.currentTimeMillis()
                if (now - lastStatusLogMs > 5000) {
                    lastStatusLogMs = now
                    syncInfo(
                        "DataDownload",
                        "Resolving glasses HTTP IP... p2p=$downloadP2pConnected, bleIp=$downloadBleIp, groupOwnerIp=$downloadWifiIp"
                    )
                }

                // 1) Try known candidates first.
                for (candidate in buildCandidateIps()) {
                    if (!isActive) return@launch
                    if (candidate.isBlank()) continue
                    val shouldLog = candidate == downloadBleIp
                    if (mediaConfigOk(candidate, 2000, logFailures = shouldLog)) {
                        downloadResolvedHttpIp = candidate
                        downloadInProgress = true
                        downloadPausedForReconnect = false
                        markTransferProgressDetected("media.config reachable")
                        syncInfo("DataDownload", "Resolved glasses HTTP IP via candidate list: $candidate")
                        downloadMediaList(candidate)
                        return@launch
                    }
                }

                // 2) If we still don't have a device IP, scan the local /24 derived from
                // the best available hint (BLE IP, bridge IP, GO subnet, or interface subnet).
                if (!didSubnetScan &&
                    downloadP2pConnected &&
                    downloadResolvedHttpIp == null &&
                    downloadBleIp == null &&
                    bleIpBridge.ip.value == null
                ) {
                    val prefix = guessDownloadSubnetPrefix()
                    if (!prefix.isNullOrBlank()) {
                        didSubnetScan = true
                        syncInfo("DataDownload", "Candidate IPs failed; scanning ${prefix}0/24 for HTTP server...")
                        val found = discoverGlassesIpByScan(prefix)
                        if (!found.isNullOrBlank()) {
                            downloadResolvedHttpIp = found
                            downloadInProgress = true
                            downloadPausedForReconnect = false
                            markTransferProgressDetected("reachable candidate IP from HTTP probing")
                            syncInfo("DataDownload", "Resolved glasses HTTP IP via scan: $found")
                            downloadMediaList(found)
                            return@launch
                        }
                    }
                }

                delay(1500)
            }

            withContext(Dispatchers.Main) {
                showDownloadError(
                    "Could not resolve glasses HTTP IP (bleIp=$downloadBleIp, groupOwnerIp=$downloadWifiIp, p2p=$downloadP2pConnected)",
                    cleanup = true
                )
            }
        }
    }

    private inner class DownloadNotifyListener : GlassesDeviceNotifyListener() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun parseData(cmdType: Int, response: GlassesDeviceNotifyRsp) {
            // Only handle download-relevant notifications here to avoid duplicating
            // other flows already handled by MyDeviceNotifyListener.
            val load = response.loadData
            if (load.size < 7) return
            val rawPayload = load.joinToString(separator = ",") { ByteUtil.byteToInt(it).toString() }
            syncInfo("DeviceNotify", "(download) cmdType=$cmdType event=0x${load[6].toInt().and(0xFF).toString(16)} raw=$rawPayload")
            recordTechnicalSyncRow(
                action = "BLE notification",
                uuid = bleNotifyUuidLabel(),
                command = "N/A",
                response = "cmdType=$cmdType, event=0x${load[6].toInt().and(0xFF).toString(16)}, raw=$rawPayload",
                status = "Received",
            )
            when (load[6].toInt()) {
                0x08 -> {
                    if (load.size >= 11) {
                        val ip = "${ByteUtil.byteToInt(load[7])}." +
                                "${ByteUtil.byteToInt(load[8])}." +
                                "${ByteUtil.byteToInt(load[9])}." +
                                "${ByteUtil.byteToInt(load[10])}"
                        syncInfo("DeviceNotify", "(download) BLE reported WiFi IP: $ip")
                        onDownloadBleIp(ip)
                    }
                }

                0x09 -> {
                    val raw = load.getOrNull(7) ?: 0
                    val errorCode = ByteUtil.byteToInt(raw)
                    syncError("DeviceNotify", "(download) P2P/WiFi error from device: $errorCode (raw=$raw)")
                    if (errorCode == 255) {
                        maybeResetP2pAfterError255("download")
                    }
                }
            }
        }
    }

    private inner class WifiCommandTestNotifyListener : GlassesDeviceNotifyListener() {
        override fun parseData(cmdType: Int, response: GlassesDeviceNotifyRsp) {
            val load = response.loadData
            val command = wifiCommandTestCurrentCommand ?: "none"
            val rawHex = commandToHex(load)
            val rawDec = load.joinToString(separator = ",") { ByteUtil.byteToInt(it).toString() }
            val event = load.getOrNull(6)?.let { "0x%02X".format(it.toInt() and 0xff) } ?: "n/a"
            syncInfo(
                "WiFiCmdTest",
                "notify after command=$command cmdType=$cmdType event=$event rawHex=$rawHex rawDec=$rawDec"
            )

            if (load.size >= 11 && load[6].toInt() == 0x08) {
                val ip = "${ByteUtil.byteToInt(load[7])}." +
                        "${ByteUtil.byteToInt(load[8])}." +
                        "${ByteUtil.byteToInt(load[9])}." +
                        "${ByteUtil.byteToInt(load[10])}"
                syncInfo("WiFiCmdTest", "notify after command=$command reported WiFi IP=$ip")
            }
        }
    }

    private fun openHttpConnection(url: URL): HttpURLConnection? {
        val network = downloadP2pNetwork ?: findLikelyP2pNetwork()?.also { downloadP2pNetwork = it }
        if (network != null) {
            try {
                val conn = network.openConnection(url) as HttpURLConnection
                conn.instanceFollowRedirects = true
                return conn
            } catch (_: Exception) {}
        }
        return try {
            val conn = url.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true
            conn
        } catch (_: Exception) { null }
    }

    /** Build an OkHttp client whose sockets bind to the P2P local address (VPN-proof). */
    private fun vpnSafeHttpClient(connectTimeoutMs: Int, readTimeoutMs: Int): okhttp3.OkHttpClient? {
        val p2pAddr = p2pLocalAddress() ?: return null
        val factory = object : javax.net.SocketFactory() {
            override fun createSocket(): Socket {
                val s = Socket()
                s.bind(InetSocketAddress(p2pAddr, 0))
                return s
            }
            override fun createSocket(host: String, port: Int) = throw UnsupportedOperationException()
            override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int) = throw UnsupportedOperationException()
            override fun createSocket(host: InetAddress, port: Int) = throw UnsupportedOperationException()
            override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int) = throw UnsupportedOperationException()
        }
        return try {
            okhttp3.OkHttpClient.Builder()
                .socketFactory(factory)
                .connectTimeout(connectTimeoutMs.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMs.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        } catch (_: Exception) { null }
    }

    /**
     * HTTP GET using P2P-bound sockets (VPN-safe).
     * Tries Network.openConnection() first, then OkHttp with P2P local-address binding.
     */
    private fun httpGet(
        url: URL,
        connectTimeoutMs: Int,
        readTimeoutMs: Int,
        onStream: ((InputStream, Long) -> Unit)? = null
    ): Boolean {
        try {
            val conn = openHttpConnection(url) ?: return false
            conn.requestMethod = "GET"
            conn.connectTimeout = connectTimeoutMs
            conn.readTimeout = readTimeoutMs
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                onStream?.invoke(conn.inputStream, conn.contentLengthLong)
                conn.disconnect()
                return true
            }
            conn.disconnect()
        } catch (e: Exception) {
            syncWarn("DataDownload", "httpGet default path failed for $url: ${e.message}")
        }

        val client = vpnSafeHttpClient(connectTimeoutMs, readTimeoutMs) ?: return false
        return try {
            val request = okhttp3.Request.Builder().url(url).build()
            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful && resp.body != null) {
                    onStream?.invoke(resp.body!!.byteStream(), resp.body!!.contentLength())
                    true
                } else false
            }
        } catch (e: Exception) {
            syncWarn("DataDownload", "P2P-bound httpGet fallback failed for $url: ${e.message}")
            false
        }
    }

    private fun findLikelyP2pNetwork(): Network? {
        // We want a network whose sockets route to the Wi‑Fi Direct group even when a VPN is active.
        // Wi‑Fi Direct networks still show up as TRANSPORT_WIFI; the VPN itself shows up as TRANSPORT_VPN.
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val prefixHints = listOfNotNull(
                ipv4Prefix24(downloadBleIp),
                ipv4Prefix24(bleIpBridge.ip.value),
                ipv4Prefix24(downloadWifiIp)
            ).distinct()

            var p2pCandidate: Network? = null
            var fallbackWifi: Network? = null

            for (n in cm.allNetworks) {
                val caps = cm.getNetworkCapabilities(n) ?: continue
                if (!caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) continue
                if (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)) continue

                val lp = cm.getLinkProperties(n)
                val ifName = lp?.interfaceName ?: ""
                val addrs = lp?.linkAddresses?.mapNotNull { it.address.hostAddress } ?: emptyList()

                val matchesHint = prefixHints.any { p -> addrs.any { it.startsWith(p) } }
                val looksLikeP2p = ifName.contains("p2p", ignoreCase = true) ||
                    ifName.contains("wfd", ignoreCase = true) ||
                    addrs.any { it.startsWith("192.168.49.") } ||
                    matchesHint

                if (looksLikeP2p) {
                    Log.i("DataDownload", "Selected P2P/WFD network candidate: if=$ifName addrs=$addrs (matchesHint=$matchesHint)")
                    p2pCandidate = n
                    // Strong match -> return early.
                    if (ifName.contains("p2p", ignoreCase = true) || ifName.contains("wfd", ignoreCase = true) || matchesHint) {
                        return n
                    }
                }

                // Keep a Wi‑Fi fallback so VPN doesn't steal routing if we fail to detect P2P.
                if (fallbackWifi == null) {
                    Log.i("DataDownload", "Keeping Wi‑Fi fallback network: if=$ifName addrs=$addrs")
                    fallbackWifi = n
                }
            }

            p2pCandidate ?: fallbackWifi
        } catch (e: Exception) {
            Log.w("DataDownload", "Failed to locate P2P network: ${e.message}")
            null
        }
    }

    private fun bindProcessToNetwork(network: Network?) {
        if (network == null) return
        if (boundNetwork == network) return

        // When a VPN is active, Android blocks bindProcessToNetwork (EPERM).
        // We skip it and rely on per-socket binding via socket.bind(p2pLocalAddress) instead.
        if (isVpnActive()) {
            Log.i("DataDownload", "VPN active — skipping bindProcessToNetwork, will bind sockets to P2P local address")
            return
        }

        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val ok = cm.bindProcessToNetwork(network)
            if (ok) {
                boundNetwork = network
                Log.i("DataDownload", "Bound process to P2P network")
            } else {
                Log.w("DataDownload", "bindProcessToNetwork returned false")
            }
        } catch (e: Exception) {
            Log.w("DataDownload", "bindProcessToNetwork failed: ${e.message}")
        }
    }

    private fun unbindProcessFromNetwork() {
        if (boundNetwork == null) return
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.bindProcessToNetwork(null)
        } catch (_: Exception) {
            // ignore
        } finally {
            boundNetwork = null
        }
    }

    private fun isVpnActive(): Boolean {
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.allNetworks.any { n ->
                cm.getNetworkCapabilities(n)
                    ?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) == true
            }
        } catch (_: Exception) { false }
    }

    /** Return the P2P network's first IPv4 local address (e.g. "192.168.49.1"). */
    private fun p2pLocalAddress(): InetAddress? {
        val network = downloadP2pNetwork ?: return null
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val lp = cm.getLinkProperties(network)
            lp?.linkAddresses
                ?.mapNotNull { it.address }
                ?.firstOrNull { it is java.net.Inet4Address }
        } catch (_: Exception) { null }
    }

    private fun maybeResetP2pAfterError255(source: String) {
        val now = System.currentTimeMillis()
        val haveDeviceIp = !downloadBleIp.isNullOrBlank() || !bleIpBridge.ip.value.isNullOrBlank()

        // Only attempt P2P resets when we're actually in (or attempting) a download session.
        // Otherwise these resets can interfere with normal camera/recording usage.
        val sessionActive = downloadInProgress ||
            downloadAttemptJob?.isActive == true ||
            downloadP2pConnected ||
            downloadWifiP2pManager != null ||
            downloadNotifyListenerRegistered
        if (!sessionActive) {
            syncInfo("DataDownload", "Ignoring error=255 reset (source=$source) outside download session")
            return
        }

        // On some devices (notably Samsung), sending the reset command while we are actively
        // trying to talk to the glasses can drop the P2P link and kill the HTTP session.
        if (downloadInProgress || (downloadAttemptJob?.isActive == true && haveDeviceIp)) {
            syncInfo("DataDownload", "Suppressing resetDeviceP2p on error=255 (source=$source) during active download/resolve")
            return
        }

        if (now - lastP2pResetAtMs < 10_000) {
            syncInfo("DataDownload", "Suppressing resetDeviceP2p on error=255 (source=$source); reset was too recent")
            return
        }
        lastP2pResetAtMs = now
        syncWarn("DataDownload", "P2P/WiFi error=255 during sync; resetting device P2P and restarting discovery")
        val manager = downloadWifiP2pManager ?: WifiP2pManagerSingleton.getInstance(this)
        manager.resetDeviceP2p()
        CoroutineScope(Dispatchers.Main).launch {
            delay(1_500)
            if (!downloadCancelledByUser && !downloadP2pConnected) {
                manager.discoverPeersStable()
                manager.startPeerDiscovery()
            }
        }
    }

    inner class MyDeviceNotifyListener : GlassesDeviceNotifyListener() {

        @RequiresApi(Build.VERSION_CODES.O)
        override fun parseData(cmdType: Int, response: GlassesDeviceNotifyRsp) {
            Log.i(
                "DeviceNotify",
                "cmdType=$cmdType, loadData=${response.loadData.joinToString(separator = ",") { it.toInt().toString() }}"
            )
            when (response.loadData[6].toInt()) {
                //Glasses battery report
                0x05 -> {
                    //Current battery
                    val battery = response.loadData[7].toInt()
                    //Is it charging
                    val changing = response.loadData[8].toInt()
                    handleBatteryReport(battery, changing == 1)
                }
                //Glasses pass quick recognition / AI Photo
                0x02 -> {
                    Log.i("DeviceNotify", "AI Photo Button Pressed")
                    if (isAiHijackEnabled) {
                        runOnUiThread {
                            val unsupportedReason = imageQueryUnsupportedReasonForCurrentSelection()
                            if (unsupportedReason != null) {
                                Toast.makeText(this@MainActivity, unsupportedReason, Toast.LENGTH_SHORT).show()
                                speak(unsupportedReason)
                                return@runOnUiThread
                            }
                            if (maybeShowGeminiChatGptImageRequirementsWarning()) {
                                return@runOnUiThread
                            }
                            handleGlassesImageButtonPressed(
                                triggerCapture = false,
                                sourceTag = "glasses_signal",
                            )
                        }
                    }
                }

                //Glasses activate microphone / AI button
                0x03 -> {
                    if (response.loadData[7].toInt() == 1) {
                        Log.i("DeviceNotify", "AI Button Pressed - Hijacking to Phone Assistant")
                        if (isAiHijackEnabled) {
                            triggerAssistantVoiceQuery()
                        } else {
                            //The glasses activate the microphone to start speaking
                            runOnUiThread {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Glasses microphone activated (Original Path)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
                //ota upgrade
                0x04 -> {
                    try {
                        val download = response.loadData[7].toInt()
                        val soc = response.loadData[8].toInt()
                        val nor = response.loadData[9].toInt()
                        //download firmware download progress soc download progress nor upgrade progress
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                0x0c -> {
                    //The glasses trigger a pause event, voice broadcast
                    if (response.loadData[7].toInt() == 1) {
                        //to do
                    }
                }

                0x0d -> {
                    //Unbind APP event
                    if (response.loadData[7].toInt() == 1) {
                        //to do
                    }
                }
                //Glasses memory low event
                0x0e -> {

                }
                //Translation pause event
                0x10 -> {

                }
                //Glasses volume change event
                0x12 -> {
                    //Music volume
                    //Minimum volume
                    response.loadData[8].toInt()
                    //Maximum volume
                    response.loadData[9].toInt()
                    //Current volume
                    response.loadData[10].toInt()

                    //Incoming call volume
                    //Minimum volume
                    response.loadData[12].toInt()
                    //Maximum volume
                    response.loadData[13].toInt()
                    //Current volume
                    response.loadData[14].toInt()

                    //Glasses system volume
                    //Minimum volume
                    response.loadData[16].toInt()
                    //Maximum volume
                    response.loadData[17].toInt()
                    //Current volume
                    response.loadData[18].toInt()

                    //Current volume mode
                    val mode = response.loadData[19].toInt()

                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Volume changed (mode=$mode)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                }
                // Glasses report WiFi IP for data download
                0x08 -> {
                    if (response.loadData.size >= 11) {
                        val ip = "${ByteUtil.byteToInt(response.loadData[7])}." +
                                "${ByteUtil.byteToInt(response.loadData[8])}." +
                                "${ByteUtil.byteToInt(response.loadData[9])}." +
                                "${ByteUtil.byteToInt(response.loadData[10])}"
                        Log.i("DeviceNotify", "BLE reported WiFi IP: $ip")
                        onDownloadBleIp(ip)
                    } else {
                        Log.w(
                            "DeviceNotify",
                            "0x08 notify with too-short payload, size=${response.loadData.size}"
                        )
                    }
                }
                // Glasses report P2P / WiFi error during data download
                0x09 -> {
                    val raw = response.loadData.getOrNull(7) ?: 0
                    val errorCode = ByteUtil.byteToInt(raw)
                    Log.e("DeviceNotify", "P2P/WiFi error from device: $errorCode (raw=$raw)")
                    if (errorCode == 255) {
                        // Mirror the official app: ask the glasses/phone P2P
                        // layer to reset, but do NOT treat this as a fatal
                        // error for the whole download flow. The official app
                        // still proceeds to receive an IP and download.
                        maybeResetP2pAfterError255("main")
                    }
                }
            }
        }
    }
}
