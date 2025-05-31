package com.example.tasklock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tasklock.data.db.AppUsageDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ResetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        withContext(Dispatchers.IO) {
            val db = AppUsageDatabase.getInstance(applicationContext)
            db.appUsageDao().deleteAll()                       // Reseta uso geral
            db.blockedAppsDao().resetDailyUsage()              // Reseta tempo de uso dos apps bloqueados

            Log.d("TaskLock", "Reset diário executado pelo WorkManager.")
            mostrarNotificacao()
        }
        return Result.success()
    }


    private fun mostrarNotificacao() {
        val channelId = "ResetChannel"
        val channelName = "Reset Diário TaskLock"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("TaskLock")
            .setContentText("Registros de uso foram resetados para o novo dia.")
            .setSmallIcon(R.drawable.main_icon_tasklock)
            .setAutoCancel(true)
            .build()

        manager.notify(1001, notification)
    }
}
