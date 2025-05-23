package com.example.tasklock.utils

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.util.concurrent.TimeUnit

/**
 * Classe utilitária para gerenciar o monitoramento de uso de aplicativos
 * Implementa o padrão Singleton para garantir uma única instância
 */
class AppUsageManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AppUsageManager"
        private const val DEFAULT_TIME_RANGE = 24L // 24 horas em horas
        private const val MIN_TIME_THRESHOLD = 1000L // 1 segundo em milissegundos

        @Volatile
        private var instance: AppUsageManager? = null

        fun getInstance(context: Context): AppUsageManager {
            return instance ?: synchronized(this) {
                instance ?: AppUsageManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val permissionManager = PermissionManager.getInstance(context)

    /**
     * Verifica se o app tem permissão para acessar estatísticas de uso
     */
    fun hasUsageStatsPermission(): Boolean {
        return permissionManager.hasUsageStatsPermission()
    }

    /**
     * Obtém o tempo de uso dos aplicativos nas últimas 24 horas
     * @return Map contendo o nome do pacote e o tempo de uso em milissegundos
     */
    fun getAppUsageStats(): Map<String, Long> {
        if (!hasUsageStatsPermission()) {
            Log.w(TAG, "Sem permissão para acessar estatísticas de uso")
            return emptyMap()
        }

        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val startTime = endTime - TimeUnit.HOURS.toMillis(DEFAULT_TIME_RANGE)

            // Verifica se o serviço está disponível
            if (usageStatsManager == null) {
                Log.e(TAG, "UsageStatsManager não disponível")
                return emptyMap()
            }

            val events = usageStatsManager.queryEvents(startTime, endTime)
            if (events == null) {
                Log.e(TAG, "Não foi possível obter eventos de uso")
                return emptyMap()
            }

            val event = UsageEvents.Event()
            val usageMap = mutableMapOf<String, Long>()
            val foregroundTimestamps = mutableMapOf<String, Long>()

            while (events.getNextEvent(event)) {
                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND,
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        foregroundTimestamps[event.packageName] = event.timeStamp
                        Log.v(TAG, "App em primeiro plano: ${event.packageName}")
                    }
                    UsageEvents.Event.MOVE_TO_BACKGROUND,
                    UsageEvents.Event.ACTIVITY_PAUSED -> {
                        foregroundTimestamps[event.packageName]?.let { start ->
                            val duration = event.timeStamp - start
                            if (duration > MIN_TIME_THRESHOLD) {
                                usageMap[event.packageName] = (usageMap[event.packageName] ?: 0) + duration
                                Log.v(TAG, "App em segundo plano: ${event.packageName}, duração: ${duration}ms")
                            }
                        }
                        foregroundTimestamps.remove(event.packageName)
                    }
                }
            }

            // Adiciona o tempo dos apps que ainda estão em primeiro plano
            foregroundTimestamps.forEach { (pkg, start) ->
                val duration = endTime - start
                if (duration > MIN_TIME_THRESHOLD) {
                    usageMap[pkg] = (usageMap[pkg] ?: 0) + duration
                    Log.v(TAG, "App ainda em primeiro plano: $pkg, duração: ${duration}ms")
                }
            }

            Log.d(TAG, "Apps detectados: ${usageMap.size}")
            usageMap.forEach { (pkg, time) ->
                Log.v(TAG, "App: $pkg, Tempo total: ${time}ms")
            }
            return usageMap

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter estatísticas de uso", e)
            return emptyMap()
        }
    }

    /**
     * Filtra apenas aplicativos de terceiros
     * @param usageMap Mapa com estatísticas de uso
     * @return Mapa filtrado apenas com apps de terceiros
     */
    fun filterThirdPartyApps(usageMap: Map<String, Long>): Map<String, Long> {
        val pm = context.packageManager
        val filteredMap = usageMap.filterKeys { packageName ->
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isOwnApp = packageName == context.packageName
                val isEssential = isEssentialSystemApp(packageName)
                
                Log.v(TAG, "Verificando app: $packageName")
                Log.v(TAG, "  - É app do sistema: $isSystemApp")
                Log.v(TAG, "  - É o próprio app: $isOwnApp")
                Log.v(TAG, "  - É app essencial: $isEssential")
                
                !isSystemApp && !isOwnApp && !isEssential
            } catch (e: PackageManager.NameNotFoundException) {
                Log.w(TAG, "App não encontrado: $packageName")
                false
            }
        }
        
        Log.d(TAG, "Apps de terceiros filtrados: ${filteredMap.size}")
        filteredMap.forEach { (pkg, time) ->
            Log.v(TAG, "App de terceiros: $pkg, Tempo: ${time}ms")
        }
        
        return filteredMap
    }

    /**
     * Verifica se um app é um app do sistema essencial
     */
    private fun isEssentialSystemApp(packageName: String): Boolean {
        val essentialApps = setOf(
            "com.android.systemui",
            "com.android.settings",
            "com.android.launcher",
            "com.android.phone",
            "com.android.providers.settings",
            "com.android.vending",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.providers.calendar",
            "com.android.providers.contacts",
            "com.android.providers.downloads",
            "com.android.providers.media",
            "com.android.providers.telephony",
            "com.android.providers.userdictionary",
            "com.android.providers.downloads.ui",
            "com.android.providers.appwidget",
            "com.android.providers.blockednumber",
            "com.android.providers.partnerbookmarks",
            "com.android.providers.settings",
            "com.android.providers.telephony",
            "com.android.providers.userdictionary",
            "com.android.providers.media.module",
            "com.android.providers.media.module",
            "com.android.providers.media.module",
            "com.android.providers.media.module"
        )
        return essentialApps.contains(packageName)
    }

    /**
     * Formata o tempo de uso em uma string legível
     * @param milliseconds Tempo em milissegundos
     * @return String formatada (ex: "2h 30m 15s")
     */
    fun formatUsageTime(milliseconds: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
        return String.format("%dh %02dm %02ds", hours, minutes, seconds)
    }

    /**
     * Verifica se o dispositivo suporta o monitoramento de uso de apps
     */
    fun isUsageStatsSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    }
} 