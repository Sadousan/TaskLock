package com.example.tasklock

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.tasklock.data.db.AppUsageDatabase
import com.example.tasklock.utils.AppInfoProvider.appInfoManual
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BlockedAppsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked_apps)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        carregarAppsBloqueados()
    }

    private fun carregarAppsBloqueados() {
        val container = findViewById<LinearLayout>(R.id.blockedAppListContainer)
        container.removeAllViews()

        val dao = AppUsageDatabase.getInstance(this).blockedAppsDao()

        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                dao.getAll()
            }

            if (apps.isEmpty()) {
                Toast.makeText(this@BlockedAppsActivity, "Nenhum app bloqueado.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            apps.forEach { app ->
                val view = layoutInflater.inflate(R.layout.activity_item_app_block, container, false)

                val manual = appInfoManual[app.packageName]
                if (manual != null) {
                    view.findViewById<ImageView>(R.id.appIcon).setImageResource(manual.second)
                    view.findViewById<TextView>(R.id.appName).text = manual.first
                } else {
                    view.findViewById<TextView>(R.id.appName).text = app.packageName
                }

                val btn = view.findViewById<Button>(R.id.appTimerButton)
                btn.text = formatLimit(app.dailyLimitMs)

                btn.setOnClickListener {
                    Toast.makeText(this@BlockedAppsActivity, "Alterar tempo ainda não implementado.", Toast.LENGTH_SHORT).show()
                }

                val checkBox = view.findViewById<CheckBox>(R.id.appCheckBox)
                checkBox.isChecked = true
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    if (!isChecked) {
                        lifecycleScope.launch {
                            dao.remove(app.packageName)
                            carregarAppsBloqueados()
                        }
                    }
                }

                container.addView(view) // acho q o erro ta aqui
            }
        }
    }

    private fun formatLimit(ms: Long): String {
        val min = ms / 60000
        return when (min) {
            15L -> "15 min"
            30L -> "30 min"
            60L -> "1 h"
            120L -> "2 h"
            else -> "${min} min"
        }
    }
}
