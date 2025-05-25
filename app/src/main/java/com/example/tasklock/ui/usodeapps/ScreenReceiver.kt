package com.example.tasklock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.tasklock.data.db.AppUsageDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScreenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) {
            Log.d("ScreenReceiver", "Tela desligada detectada.")

            TrackerState.lastApp?.let { currentApp ->
                val now = System.currentTimeMillis()
                val duration = now - TrackerState.lastTimestamp
                if (duration > 0) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val dao = AppUsageDatabase.getInstance(context).appUsageDao()
                        dao.insertOrUpdate(currentApp, now, duration)
                        Log.d("ScreenReceiver", "Tempo salvo na tela desligada: $currentApp - $duration ms")
                    }
                    TrackerState.lastTimestamp = now
                }
            }
        }
    }
}
