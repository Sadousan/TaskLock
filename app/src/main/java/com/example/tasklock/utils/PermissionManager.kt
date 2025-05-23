package com.example.tasklock.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * Classe responsável por gerenciar permissões específicas de fabricantes
 * Implementa o padrão Singleton para garantir uma única instância
 */
class PermissionManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "PermissionManager"
        
        // Intent actions específicos de fabricantes
        private const val XIAOMI_PERMISSION_EDITOR = "miui.intent.action.APP_PERM_EDITOR"
        private const val XIAOMI_PACKAGE_NAME = "com.miui.securitycenter"
        private const val XIAOMI_ACTIVITY_NAME = "com.miui.permcenter.permissions.AppPermissionsEditorActivity"
        
        // Intent actions para outros fabricantes
        private const val OPPO_PERMISSION_EDITOR = "oppo.intent.action.APP_PERM_EDITOR"
        private const val VIVO_PERMISSION_EDITOR = "vivo.intent.action.APP_PERM_EDITOR"
        private const val HUAWEI_PERMISSION_EDITOR = "huawei.intent.action.APP_PERM_EDITOR"

        @Volatile
        private var instance: PermissionManager? = null

        fun getInstance(context: Context): PermissionManager {
            return instance ?: synchronized(this) {
                instance ?: PermissionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Verifica se o dispositivo é um Xiaomi
     */
    fun isXiaomiDevice(): Boolean {
        return Build.MANUFACTURER.lowercase().contains("xiaomi") ||
               Build.MANUFACTURER.lowercase().contains("redmi")
    }

    /**
     * Verifica se o app tem todas as permissões necessárias
     */
    fun hasAllRequiredPermissions(): Boolean {
        return hasUsageStatsPermission() && hasAutostartPermission()
    }

    /**
     * Verifica se o app tem permissão de uso de estatísticas
     */
    fun hasUsageStatsPermission(): Boolean {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 // últimos 60 segundos

        val stats = usageStatsManager.queryUsageStats(
            android.app.usage.UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        return stats != null && stats.isNotEmpty()
    }


    /**
     * Verifica se o app tem permissão de autostart (específico para Xiaomi)
     */
    fun hasAutostartPermission(): Boolean {
        if (!isXiaomiDevice()) return true

        return try {
            val packageManager = context.packageManager
            val intent = Intent(XIAOMI_PERMISSION_EDITOR)
            intent.setClassName(
                XIAOMI_PACKAGE_NAME,
                XIAOMI_ACTIVITY_NAME
            )
            intent.putExtra("extra_pkgname", context.packageName)
            packageManager.resolveActivity(intent, 0) != null
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar permissão de autostart", e)
            false
        }
    }

    /**
     * Abre as configurações de permissão apropriadas para o dispositivo
     */
    fun openPermissionSettings() {
        when {
            isXiaomiDevice() -> {
                try {
                    // Tenta abrir as configurações específicas do Xiaomi
                    val intent = Intent(XIAOMI_PERMISSION_EDITOR)
                    intent.setClassName(
                        XIAOMI_PACKAGE_NAME,
                        XIAOMI_ACTIVITY_NAME
                    )
                    intent.putExtra("extra_pkgname", context.packageName)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao abrir configurações Xiaomi", e)
                    // Fallback para configurações gerais
                    openGeneralUsageSettings()
                }
            }
            else -> openGeneralUsageSettings()
        }
    }

    /**
     * Abre as configurações gerais de uso de apps
     */
    private fun openGeneralUsageSettings() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao abrir configurações gerais", e)
            // Último recurso: abre configurações do app
            openAppSettings()
        }
    }

    /**
     * Abre as configurações do app
     */
    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", context.packageName, null)
            intent.data = uri
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao abrir configurações do app", e)
        }
    }

    /**
     * Verifica se o app está instalado como app do sistema
     */
    fun isSystemApp(): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val appInfo = packageInfo.applicationInfo
            appInfo?.let {
                it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
            } ?: false
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

} 