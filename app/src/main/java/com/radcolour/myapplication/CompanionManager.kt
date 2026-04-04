package com.radcolour.myapplication

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.File

object CompanionManager {

    interface ConnectionListener {
        fun onConnectionChanged(connected: Boolean)
    }

    private const val PING_FILENAME = "companion_ping"
    private const val POLL_INTERVAL_MS = 3000L
    private const val STALE_THRESHOLD_MS = 6000L

    private val handler = Handler(Looper.getMainLooper())
    private var listener: ConnectionListener? = null
    private var isConnected = false
    private var pingFile: File? = null

    private val pollRunnable = object : Runnable {
        override fun run() {
            checkPing()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    fun init(context: Context) {
        val dir = context.getExternalFilesDir(null)
        pingFile = if (dir != null) File(dir, PING_FILENAME) else null
    }

    fun start(listener: ConnectionListener) {
        this.listener = listener
        handler.post(pollRunnable)
    }

    fun stop() {
        handler.removeCallbacks(pollRunnable)
        listener = null
    }

    private fun checkPing() {
        val file = pingFile ?: return
        val connected = file.exists() &&
                (System.currentTimeMillis() - file.lastModified()) < STALE_THRESHOLD_MS
        if (connected != isConnected) {
            isConnected = connected
            listener?.onConnectionChanged(connected)
        }
    }
}