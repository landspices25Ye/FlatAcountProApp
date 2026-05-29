package com.example.core.logger

import android.util.Log

interface ILogger {
    fun d(tag: String, msg: String)
    fun i(tag: String, msg: String)
    fun w(tag: String, msg: String, tr: Throwable? = null)
    fun e(tag: String, msg: String, tr: Throwable? = null)
}

object ConsoleLogger : ILogger {
    override fun d(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    override fun i(tag: String, msg: String) {
        Log.i(tag, msg)
    }

    override fun w(tag: String, msg: String, tr: Throwable?) {
        Log.w(tag, msg, tr)
    }

    override fun e(tag: String, msg: String, tr: Throwable?) {
        Log.e(tag, msg, tr)
    }
}

object RemoteLogger : ILogger {
    override fun d(tag: String, msg: String) {
        // Safe mock for production Sentry or Firebase crashlytics log send
    }

    override fun i(tag: String, msg: String) {
        // Send critical logs to server
    }

    override fun w(tag: String, msg: String, tr: Throwable?) {
        // Send warning trace to server
    }

    override fun e(tag: String, msg: String, tr: Throwable?) {
        // Send exception dump to telemetry
    }
}

object LoggerFactory {
    fun getLogger(isDebug: Boolean = true): ILogger {
        return if (isDebug) ConsoleLogger else RemoteLogger
    }
}
