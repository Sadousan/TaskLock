package com.example.tasklock

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.tasklock.data.db.AppUsageDatabase
import com.example.tasklock.ui.usodeapps.BlockedWarningActivity
import kotlinx.coroutines.*

object TrackerState {
    var lastApp: String? = null
    var lastTimestamp: Long = 0L
}

class AppAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var periodicJob: Job? = null
    private lateinit var screenReceiver: BroadcastReceiver

    override fun onServiceConnected() {
        super.onServiceConnected()

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }

        Log.d("Accessibility", "AccessibilityService iniciado corretamente.")

        iniciarPersistenciaContinua()

        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    finalizarUsoAtual()
                    Log.d("Accessibility", "Tela desligada, uso atual persistido.")
                }
            }
        }

        registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val currentApp = event.packageName?.toString() ?: return
        val now = System.currentTimeMillis()

        if (isSystemApp(currentApp)) return

        Log.d("Accessibility", "App detectado: $currentApp")

        scope.launch {
            val daoBlocked = AppUsageDatabase.getInstance(applicationContext).blockedAppsDao()
            val blockedApp = daoBlocked.getByPackage(currentApp)

            if (blockedApp != null) {
                val totalUsage = blockedApp.usedTodayMs
                val limit = blockedApp.dailyLimitMs

                if (totalUsage >= limit) {
                    Log.d("Accessibility", "APP BLOQUEADO PELO TASKLOCK: $currentApp")

                    withContext(Dispatchers.Main) {
                        val intent = Intent(this@AppAccessibilityService, BlockedWarningActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)

                        // Voltar para a tela inicial (Home)
                        performGlobalAction(GLOBAL_ACTION_HOME)
                    }

                    return@launch
                }
            }

            processarUsoNormal(currentApp, now)
        }
    }

    private suspend fun processarUsoNormal(currentApp: String, now: Long) {
        if (TrackerState.lastApp != null && TrackerState.lastApp != currentApp) {
            val duration = now - TrackerState.lastTimestamp
            if (duration in 1000..1000L * 60 * 60 * 6) {
                salvarUso(TrackerState.lastApp!!, now, duration)
            }
            TrackerState.lastApp = currentApp
            TrackerState.lastTimestamp = now
        } else if (TrackerState.lastApp == currentApp) {
            val duration = now - TrackerState.lastTimestamp
            if (duration > 10000) {
                salvarUso(currentApp, now, duration)
                TrackerState.lastTimestamp = now
            }
        } else {
            TrackerState.lastApp = currentApp
            TrackerState.lastTimestamp = now
        }
    }

    private fun salvarUso(pkg: String, timestamp: Long, duration: Long) {
        scope.launch {
            val daoUsage = AppUsageDatabase.getInstance(applicationContext).appUsageDao()
            val daoBlocked = AppUsageDatabase.getInstance(applicationContext).blockedAppsDao()

            daoUsage.insertOrUpdate(pkg, timestamp, duration)

            val blocked = daoBlocked.getByPackage(pkg)
            if (blocked != null) {
                val novoUso = blocked.usedTodayMs + duration
                daoBlocked.insertOrUpdate(blocked.copy(usedTodayMs = novoUso))
            }

            Log.d("Accessibility", "Salvou uso: $pkg - $duration ms")
        }
    }

    private fun finalizarUsoAtual() {
        val now = System.currentTimeMillis()
        val currentApp = TrackerState.lastApp ?: return
        val duration = now - TrackerState.lastTimestamp
        if (duration >= 1000) {
            salvarUso(currentApp, TrackerState.lastTimestamp, duration)
        }
        TrackerState.lastApp = null
        TrackerState.lastTimestamp = 0L
    }

    override fun onInterrupt() {
        finalizarUsoAtual()
        periodicJob?.cancel()
        Log.d("Accessibility", "Serviço interrompido")
    }

    override fun onDestroy() {
        super.onDestroy()
        finalizarUsoAtual()
        periodicJob?.cancel()
        unregisterReceiver(screenReceiver)
        Log.d("Accessibility", "AccessibilityService destruído")
    }

    private fun iniciarPersistenciaContinua() {
        periodicJob?.cancel()
        periodicJob = scope.launch {
            while (true) {
                delay(15000L)
                val currentApp = TrackerState.lastApp ?: continue
                val now = System.currentTimeMillis()
                val duration = now - TrackerState.lastTimestamp
                if (duration >= 10000) {
                    salvarUso(currentApp, TrackerState.lastTimestamp, duration)
                    TrackerState.lastTimestamp = now
                }
            }
        }
    }

    private fun isSystemApp(pkg: String): Boolean {
        return pkg.startsWith("com.android.") ||
                pkg.startsWith("com.miui.") ||
                pkg.startsWith("com.samsung.") ||
                pkg.startsWith("android") ||
                pkg.startsWith("miui.systemui.") ||
                pkg == packageName
    }
}
