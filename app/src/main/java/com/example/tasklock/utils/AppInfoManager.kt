package com.example.tasklock.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.util.Log
import java.util.concurrent.TimeUnit

/**
 * Classe utilitária para obter informações sobre aplicativos instalados
 * usando ContentProviders e Cursor
 */
class AppInfoManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AppInfoManager"
        
        // URIs dos ContentProviders
        private val PACKAGE_URI = Uri.parse("content://com.android.launcher3.settings/favorites")
        private val MEDIA_STORE_URI = Uri.parse("content://media/external/file")
        private val INSTALLED_APPS_URI = Uri.parse("content://applications")
        
        @Volatile
        private var instance: AppInfoManager? = null

        fun getInstance(context: Context): AppInfoManager {
            return instance ?: synchronized(this) {
                instance ?: AppInfoManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Obtém informações básicas de todos os apps instalados
     * @return Lista de pares (nome do pacote, nome do app)
     */
    fun getInstalledApps(): List<Pair<String, String>> {
        val pm = context.packageManager
        val installedApps = mutableListOf<Pair<String, String>>()
        
        try {
            // Primeiro tenta obter via ContentProvider
            val apps = queryInstalledApps()
            if (apps.isNotEmpty()) {
                Log.d(TAG, "Apps obtidos via ContentProvider: ${apps.size}")
                return apps
            }
            
            // Se falhar, usa o PackageManager
            Log.d(TAG, "Usando PackageManager como fallback")
            val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in allApps) {
                // Ignora apps do sistema e o próprio app
                if ((app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && 
                    app.packageName != context.packageName) {
                    val appName = pm.getApplicationLabel(app).toString()
                    installedApps.add(app.packageName to appName)
                    Log.v(TAG, "App encontrado: $appName (${app.packageName})")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter apps instalados", e)
        }
        
        return installedApps
    }

    /**
     * Tenta obter informações adicionais dos apps via ContentProviders
     * @return Mapa com informações adicionais dos apps
     */
    fun getAdditionalAppInfo(): Map<String, AppInfo> {
        val appInfoMap = mutableMapOf<String, AppInfo>()
        
        try {
            // Tenta obter informações do launcher
            queryLauncherInfo(appInfoMap)
            
            // Tenta obter informações do MediaStore
            queryMediaStoreInfo(appInfoMap)
            
            // Tenta obter informações do ContentProvider de apps instalados
            queryInstalledAppsInfo(appInfoMap)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter informações adicionais", e)
        }
        
        return appInfoMap
    }

    /**
     * Consulta informações do launcher
     */
    private fun queryLauncherInfo(appInfoMap: MutableMap<String, AppInfo>) {
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                PACKAGE_URI,
                null,
                null,
                null,
                null
            )
            
            cursor?.let {
                while (it.moveToNext()) {
                    try {
                        val packageName = it.getString(it.getColumnIndexOrThrow("packageName"))
                        val lastUpdateTime = it.getLong(it.getColumnIndexOrThrow("lastUpdateTime"))
                        
                        appInfoMap[packageName] = AppInfo(
                            packageName = packageName,
                            lastUpdateTime = lastUpdateTime,
                            source = "Launcher"
                        )
                        Log.v(TAG, "Informações do launcher obtidas para: $packageName")
                    } catch (e: Exception) {
                        Log.w(TAG, "Erro ao processar linha do launcher", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Não foi possível acessar informações do launcher", e)
        } finally {
            cursor?.close()
        }
    }

    /**
     * Consulta informações do MediaStore
     */
    private fun queryMediaStoreInfo(appInfoMap: MutableMap<String, AppInfo>) {
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                MEDIA_STORE_URI,
                null,
                null,
                null,
                null
            )
            
            cursor?.let {
                while (it.moveToNext()) {
                    try {
                        val path = it.getString(it.getColumnIndexOrThrow("_data"))
                        val packageName = path?.substringAfterLast("/")?.substringBeforeLast(".")
                        
                        if (packageName != null && !appInfoMap.containsKey(packageName)) {
                            val lastModified = it.getLong(it.getColumnIndexOrThrow("date_modified"))
                            appInfoMap[packageName] = AppInfo(
                                packageName = packageName,
                                lastUpdateTime = lastModified,
                                source = "MediaStore"
                            )
                            Log.v(TAG, "Informações do MediaStore obtidas para: $packageName")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Erro ao processar linha do MediaStore", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Não foi possível acessar informações do MediaStore", e)
        } finally {
            cursor?.close()
        }
    }

    /**
     * Consulta informações dos apps instalados
     */
    private fun queryInstalledApps(): List<Pair<String, String>> {
        var cursor: Cursor? = null
        val apps = mutableListOf<Pair<String, String>>()
        
        try {
            cursor = context.contentResolver.query(
                INSTALLED_APPS_URI,
                null,
                null,
                null,
                null
            )
            
            cursor?.let {
                while (it.moveToNext()) {
                    try {
                        val packageName = it.getString(it.getColumnIndexOrThrow("package_name"))
                        val appName = it.getString(it.getColumnIndexOrThrow("app_name"))
                        apps.add(packageName to appName)
                        Log.v(TAG, "App encontrado via ContentProvider: $appName ($packageName)")
                    } catch (e: Exception) {
                        Log.w(TAG, "Erro ao processar linha do ContentProvider", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Não foi possível acessar ContentProvider de apps instalados", e)
        } finally {
            cursor?.close()
        }
        
        return apps
    }

    /**
     * Consulta informações adicionais dos apps instalados
     */
    private fun queryInstalledAppsInfo(appInfoMap: MutableMap<String, AppInfo>) {
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                INSTALLED_APPS_URI,
                null,
                null,
                null,
                null
            )
            
            cursor?.let {
                while (it.moveToNext()) {
                    try {
                        val packageName = it.getString(it.getColumnIndexOrThrow("package_name"))
                        val lastUpdateTime = it.getLong(it.getColumnIndexOrThrow("last_update_time"))
                        
                        if (!appInfoMap.containsKey(packageName)) {
                            appInfoMap[packageName] = AppInfo(
                                packageName = packageName,
                                lastUpdateTime = lastUpdateTime,
                                source = "InstalledApps"
                            )
                            Log.v(TAG, "Informações adicionais obtidas para: $packageName")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Erro ao processar linha do ContentProvider", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Não foi possível acessar informações adicionais dos apps", e)
        } finally {
            cursor?.close()
        }
    }

    /**
     * Classe de dados para armazenar informações dos apps
     */
    data class AppInfo(
        val packageName: String,
        val lastUpdateTime: Long,
        val source: String
    )
} 