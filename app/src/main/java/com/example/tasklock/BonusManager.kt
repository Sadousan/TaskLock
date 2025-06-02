package com.example.tasklock.bonus

import android.content.Context
import com.example.tasklock.data.db.AppUsageDatabase
import com.example.tasklock.data.model.BonusDiarioEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object BonusManager {

    // Limite diário total de bônus em milissegundos (ex: 1h = 3600000ms)
    private const val LIMITE_BONUS_DIARIO_MS = 60 * 60 * 1000L

    // Formato de data para chave de controle (yyyy-MM-dd)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun processarConclusaoTarefa(context: Context, bonusTotalTarefa: Long): Boolean {
        return withContext(Dispatchers.IO) {
            val db = AppUsageDatabase.getInstance(context)
            val bonusDao = db.bonusDiarioDao() // Correção no nome do método
            val blockedDao = db.blockedAppsDao()

            val hoje = dateFormat.format(Calendar.getInstance().time)
            val usadoHoje = bonusDao.getBonusDoDia(hoje)?.usadoMs ?: 0L


            val bonusDisponivel = LIMITE_BONUS_DIARIO_MS - usadoHoje
            if (bonusDisponivel <= 0) return@withContext false

            val bonusDistribuir = bonusTotalTarefa.coerceAtMost(bonusDisponivel)
            val appsBloqueados = blockedDao.getAll()
            if (appsBloqueados.isEmpty()) return@withContext false

            val bonusPorApp = bonusDistribuir / appsBloqueados.size

            appsBloqueados.forEach { app ->
                val novoBonus = app.bonusMs + bonusPorApp
                blockedDao.atualizarBonusApp(app.packageName, novoBonus)
            }

            val novoTotalUsado = usadoHoje + bonusDistribuir
            bonusDao.inserirOuAtualizar(BonusDiarioEntity(data = hoje, usadoMs = novoTotalUsado))


            true
        }
    }

    suspend fun getBonusUsadoHoje(context: Context): Long {
        return withContext(Dispatchers.IO) {
            val hoje = dateFormat.format(Calendar.getInstance().time)
            AppUsageDatabase.getInstance(context).bonusDiarioDao().getBonusDoDia(hoje)?.usadoMs ?: 0L
        }
    }

    fun getLimiteMaximo(): Long = LIMITE_BONUS_DIARIO_MS
}
