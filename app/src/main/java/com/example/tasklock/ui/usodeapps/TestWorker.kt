package com.example.tasklock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class TestWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        mostrarNotificacaoTeste()
        return Result.success()
    }

    private fun mostrarNotificacaoTeste() {
        val channelId = "TestChannel"
        val channelName = "Test Notificações"
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
            .setContentTitle("TaskLock (Teste)")
            .setContentText("Notificação de teste a cada 2 horas.")
            .setSmallIcon(R.drawable.main_icon_tasklock)
            .setAutoCancel(true)
            .build()

        manager.notify((1000..9999).random(), notification)
    }
}
