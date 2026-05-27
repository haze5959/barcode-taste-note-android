package com.oq.barnote.core.oqcore.utils

import android.util.Log

object OQLog {
    private const val TAG = "OQLog"

    fun i(message: String, prefix: String = "ℹ️ INFO", saveLog: Boolean = true) {
        Log.i(TAG, "[$prefix] $message")
        // TODO: Toast or file logging for DEBUG if necessary
    }

    fun d(message: String, prefix: String = "DEBUG") {
        // In Android, typically we check BuildConfig.DEBUG or Timber.
        Log.d(TAG, "[$prefix] $message")
    }

    fun e(message: String, prefix: String = "❌ ERR", saveLog: Boolean = true) {
        Log.e(TAG, "[$prefix] $message")
        // TODO: Crashlytics reporting for Release
    }
}
