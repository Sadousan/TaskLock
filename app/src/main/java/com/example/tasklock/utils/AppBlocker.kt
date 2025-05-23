package com.example.tasklock.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.concurrent.TimeUnit

/**
 * Classe responsável por gerenciar o bloqueio de aplicativos
 * Implementa o padrão Singleton para garantir uma única instância
 */
class AppBlocker private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AppBlocker"
        private const val PREFS_NAME = "AppBlockerPrefs"
        private const val KEY_BLOCKED_APPS = "blocked_apps"
        private const val KEY_BLOCK_UNTIL = "block_until_"

        @Volatile
        private var instance: AppBlocker? = null

        fun getInstance(context: Context): AppBlocker {
            return instance ?: synchronized(this) {
                instance ?: AppBlocker(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Bloqueia um aplicativo por um determinado tempo
     * @param packageName Nome do pacote do aplicativo
     * @param durationMinutes Duração do bloqueio em minutos
     */
    fun blockApp(packageName: String, durationMinutes: Int) {
        val blockUntil = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(durationMinutes.toLong())
        prefs.edit().apply {
            putLong(KEY_BLOCK_UNTIL + packageName, blockUntil)
            putStringSet(KEY_BLOCKED_APPS, getBlockedApps() + packageName)
            apply()
        }
        Log.d(TAG, "App $packageName bloqueado até ${blockUntil}")
    }

    /**
     * Verifica se um aplicativo está bloqueado
     * @param packageName Nome do pacote do aplicativo
     * @return true se o app estiver bloqueado, false caso contrário
     */
    fun isAppBlocked(packageName: String): Boolean {
        val blockUntil = prefs.getLong(KEY_BLOCK_UNTIL + packageName, 0)
        if (blockUntil > 0 && System.currentTimeMillis() > blockUntil) {
            // Remove o bloqueio se o tempo expirou
            unblockApp(packageName)
            return false
        }
        return blockUntil > 0
    }

    /**
     * Remove o bloqueio de um aplicativo
     * @param packageName Nome do pacote do aplicativo
     */
    fun unblockApp(packageName: String) {
        prefs.edit().apply {
            remove(KEY_BLOCK_UNTIL + packageName)
            putStringSet(KEY_BLOCKED_APPS, getBlockedApps() - packageName)
            apply()
        }
        Log.d(TAG, "App $packageName desbloqueado")
    }

    /**
     * Obtém a lista de aplicativos bloqueados
     * @return Set contendo os nomes dos pacotes bloqueados
     */
    fun getBlockedApps(): Set<String> {
        return prefs.getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()
    }

    /**
     * Obtém o tempo restante de bloqueio de um aplicativo
     * @param packageName Nome do pacote do aplicativo
     * @return Tempo restante em minutos, ou 0 se não estiver bloqueado
     */
    fun getRemainingBlockTime(packageName: String): Int {
        val blockUntil = prefs.getLong(KEY_BLOCK_UNTIL + packageName, 0)
        if (blockUntil <= 0) return 0
        
        val remaining = blockUntil - System.currentTimeMillis()
        return if (remaining > 0) {
            TimeUnit.MILLISECONDS.toMinutes(remaining).toInt()
        } else {
            unblockApp(packageName)
            0
        }
    }
} 