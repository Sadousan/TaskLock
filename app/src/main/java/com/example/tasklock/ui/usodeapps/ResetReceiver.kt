package com.example.tasklock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.tasklock.data.db.AppUsageDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.tasklock.ACTION_RESET_DAILY") {
            CoroutineScope(Dispatchers.IO).launch {
                AppUsageDatabase.getInstance(context).appUsageDao().deleteAll()
                Log.d("TaskLock", "Reset diário executado automaticamente.")
            }
        }
    }
}
