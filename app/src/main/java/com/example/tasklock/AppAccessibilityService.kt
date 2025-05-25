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

        // Ignore os malditos apps vampiros de foco de atividade (sistema e teclado)
        if (currentApp.startsWith("com.android.systemui") || // tela inicial
            currentApp.startsWith("com.miui.home") || // tela inicial
            currentApp.startsWith("com.google.android.inputmethod") || // teclado
            currentApp.startsWith("miui.systemui.plugin") || // recursos gráficos (aparecia em jogos, youtube, etc
            currentApp.startsWith("com.samsung.android.app.cocktailbarservice") || // Sidebar Samsung
            currentApp.startsWith("com.samsung.android.app.smartcapture") || // Captura Samsung
            currentApp.startsWith("com.samsung.android.app.taskedge") || // Painel Edge Samsung
            currentApp.startsWith("com.samsung.android.honeyboard") || // Teclado Samsung
            currentApp.startsWith("com.sec.android.app.launcher") || // Launcher Samsung
            currentApp.startsWith("com.motorola.launcher3") || // Launcher Motorola
            currentApp.startsWith("com.motorola.actions") || // Gestos Motorola
            currentApp.startsWith("com.motorola.motosignature.app") || // Personalizações Motorola
            currentApp.startsWith("com.google.android.apps.nexuslauncher") || // Launcher Pixel/Android puro
            currentApp.startsWith("com.google.android.setupwizard") || // Configurações iniciais Android
            currentApp.startsWith("com.android.launcher3") || // Launcher genérico Android puro
            currentApp.startsWith("android") || // Genérico para pacotes do sistema Android
            currentApp.startsWith("com.mi.android.globallauncher") || // Launcher Xiaomi
            currentApp.startsWith("com.miui.home.launcher") || // Launcher Xiaomi
            currentApp.startsWith("com.android.settings") ||
            currentApp == packageName) {
            return
        }

        Log.d("Accessibility", "App detectado: $currentApp")

        if (TrackerState.lastApp != null && TrackerState.lastApp != currentApp) {
            val duration = now - TrackerState.lastTimestamp
            if (duration in 1000..1000L * 60 * 60 * 6) {
                salvarUso(TrackerState.lastApp!!, now, duration)
            }
            TrackerState.lastApp = currentApp
            TrackerState.lastTimestamp = now
        } else if (TrackerState.lastApp == currentApp) {
            val duration = now - TrackerState.lastTimestamp
            if (duration > 10000) { // Persistência a cada 10s
                salvarUso(currentApp, now, duration)
                TrackerState.lastTimestamp = now
            }
        } else {
            TrackerState.lastApp = currentApp
            TrackerState.lastTimestamp = now
        }
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

    private fun salvarUso(pkg: String, timestamp: Long, duration: Long) {
        scope.launch {
            val dao = AppUsageDatabase.getInstance(applicationContext).appUsageDao()
            dao.insertOrUpdate(pkg, timestamp, duration)
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
}
