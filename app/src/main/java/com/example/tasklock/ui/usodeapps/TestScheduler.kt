package com.example.tasklock

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object TestScheduler {
    fun scheduleTestNotification(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<TestWorker>(2, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "TestNotificationWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}
