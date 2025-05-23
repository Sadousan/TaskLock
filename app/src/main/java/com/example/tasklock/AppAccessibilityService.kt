package com.example.tasklock

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.tasklock.data.db.AppUsageDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object TrackerState {
    var lastApp: String? = null
    var lastTimestamp: Long = 0L
}

class AppAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var periodicJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
        Log.d("Accessibility", "AccessibilityService conectado com sucesso.")

        iniciarPersistenciaContinua()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val currentApp = event.packageName?.toString() ?: return
        val now = System.currentTimeMillis()

        if (currentApp.startsWith("com.android.systemui") ||
            currentApp.startsWith("com.miui.home") ||
            currentApp == packageName) {
            return
        }

        if (TrackerState.lastTimestamp == 0L) {
            TrackerState.lastApp = currentApp
            TrackerState.lastTimestamp = now
            Log.d("Accessibility", "Primeira inicialização: $currentApp")
            return
        }

        if (TrackerState.lastApp != null && TrackerState.lastApp != currentApp) {
            val duration = now - TrackerState.lastTimestamp
            if (duration in 1000..1000L * 60 * 60 * 6) {
                Log.d("Accessibility", "Salvando troca: ${TrackerState.lastApp} - ${duration}ms")
                scope.launch {
                    val dao = AppUsageDatabase.getInstance(applicationContext).appUsageDao()
                    dao.insertOrUpdate(TrackerState.lastApp!!, now, duration)
                }
            }
            TrackerState.lastApp = currentApp
            TrackerState.lastTimestamp = now
        } else if (TrackerState.lastApp == currentApp) {
            val duration = now - TrackerState.lastTimestamp
            if (duration > 10000) {
                Log.d("Accessibility", "App contínuo gravado: $currentApp - ${duration}ms")
                scope.launch {
                    val dao = AppUsageDatabase.getInstance(applicationContext).appUsageDao()
                    dao.insertOrUpdate(currentApp, now, duration)
                }
                TrackerState.lastTimestamp = now
            }
        } else {
            TrackerState.lastApp = currentApp
            TrackerState.lastTimestamp = now
            Log.d("Accessibility", "Inicializando tracker com $currentApp")
        }
    }

    private fun iniciarPersistenciaContinua() {
        periodicJob?.cancel()
        periodicJob = scope.launch {
            while (true) {
                delay(10_000L)
                val currentApp = TrackerState.lastApp ?: continue
                val now = System.currentTimeMillis()
                val duration = now - TrackerState.lastTimestamp
                if (duration > 1000) {
                    val dao = AppUsageDatabase.getInstance(applicationContext).appUsageDao()
                    dao.insertOrUpdate(currentApp, now, duration)
                    TrackerState.lastTimestamp = now
                    Log.d("AccessibilityLoop", "Atualização contínua: $currentApp -> ${duration}ms")
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d("Accessibility", "Serviço interrompido.")
        periodicJob?.cancel()
    }
}
