package com.iptvtv.player.util

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Keeps the stack trace of the last crash on disk so it can be read back inside the app.
 *
 * A sideloaded TV app has no practical way to show a user its logcat: there is no cable, no
 * developer tools on the screen, and by the time the app is restarted the log is gone. Writing
 * the trace to a file and showing it in the settings screen turns "it froze and closed" into
 * something diagnosable.
 */
class CrashReporter(context: Context) {

    private val file = File(context.filesDir, "last-crash.txt")

    fun install() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                file.writeText(
                    buildString {
                        appendLine(TIMESTAMP_FORMAT.format(Date()))
                        appendLine("Thread: ${thread.name}")
                        appendLine()
                        append(Log.getStackTraceString(error))
                    },
                )
            }
            // Still let the platform do its normal thing, so the crash is not swallowed.
            previousHandler?.uncaughtException(thread, error)
        }
    }

    fun lastCrash(): String? = runCatching {
        file.takeIf { it.exists() && it.length() > 0 }?.readText()
    }.getOrNull()

    fun clear() {
        runCatching { file.delete() }
    }

    private companion object {
        val TIMESTAMP_FORMAT = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    }
}
