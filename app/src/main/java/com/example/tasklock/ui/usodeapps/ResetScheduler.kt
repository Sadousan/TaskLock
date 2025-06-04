package com.example.tasklock

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ResetScheduler {
    fun scheduleDailyReset(context: Context) {
        val now = Calendar.getInstance()
        val due = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val initialDelay = due.timeInMillis - now.timeInMillis

        Log.d("ResetScheduler", "Reset agendado para: ${due.time}")


        val workRequest = PeriodicWorkRequestBuilder<ResetWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "ResetDailyWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
