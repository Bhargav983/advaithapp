package com.fersaiyan.cyanbridge.ui

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SyncLogBuffer {
    private const val MAX_LINES = 1000
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val lines = ArrayDeque<String>(MAX_LINES)

    @Synchronized
    fun log(priority: Int, tag: String, message: String, throwable: Throwable? = null) {
        when (priority) {
            Log.ERROR -> Log.e(tag, message, throwable)
            Log.WARN -> Log.w(tag, message, throwable)
            Log.DEBUG -> Log.d(tag, message, throwable)
            else -> Log.i(tag, message, throwable)
        }

        val level = when (priority) {
            Log.ERROR -> "E"
            Log.WARN -> "W"
            Log.DEBUG -> "D"
            else -> "I"
        }
        appendLine("${timestampFormat.format(Date())} $level/$tag: $message")
        if (throwable != null) {
            appendLine("${timestampFormat.format(Date())} $level/$tag: ${Log.getStackTraceString(throwable)}")
        }
    }

    @Synchronized
    fun snapshot(): String = lines.joinToString(separator = "\n")

    @Synchronized
    fun clear() {
        lines.clear()
    }

    @Synchronized
    private fun appendLine(line: String) {
        while (lines.size >= MAX_LINES) {
            lines.removeFirst()
        }
        lines.addLast(line)
    }
}
