package com.example.tasklock

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class UsoApp : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_uso_app)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val mainScroll = findViewById<ScrollView>(R.id.Main)
        ViewCompat.setOnApplyWindowInsetsListener(mainScroll) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Verifica permissão no início
        verificarPermissaoEExecutar()

        // Botão extra para abrir configurações específicas da MIUI (se desejar testar)
        findViewById<Button>(R.id.btnBlock)?.setOnClickListener {
            val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
            intent.putExtra("extra_pkgname", packageName)
            startActivity(intent)
        }
    }

    private fun verificarPermissaoEExecutar() {
        if (!hasUsageStatsPermission()) {
            Toast.makeText(this, "Permissão de uso não concedida. Redirecionando...", Toast.LENGTH_SHORT).show()
            // Inicia Activity de permissões e espera resultado
            startActivityForResult(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), 1234)
        } else {
            carregarAppsMaisUsados()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Quando retorna da tela de permissões, tenta recarregar automaticamente
        if (requestCode == 1234) {
            if (hasUsageStatsPermission()) {
                Toast.makeText(this, "Permissão concedida. Carregando apps...", Toast.LENGTH_SHORT).show()
                carregarAppsMaisUsados()
            } else {
                Toast.makeText(this, "Permissão ainda não concedida.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Coleta e exibe apps mais usados via UsageEvents
    private fun carregarAppsMaisUsados() {
        val container = findViewById<LinearLayout>(R.id.appListContainer)
        container.removeAllViews() // Limpa ao recarregar
        val pm = packageManager
        val installedPackages = pm.getInstalledApplications(0).associateBy { it.packageName }

        val usageEventsMap = getUsageEventsCalculated()

        val usageList = usageEventsMap
            .filterKeys { installedPackages.containsKey(it) }
            .toList()
            .sortedByDescending { it.second }
            .take(30)

        if (usageList.isEmpty()) {
            Toast.makeText(this, "Nenhum app detectado nas últimas 24h.", Toast.LENGTH_LONG).show()
            return
        }

        usageList.forEach { (pkg, tempoMs) ->
            try {
                val appInfo = installedPackages[pkg]!!
                val appName = pm.getApplicationLabel(appInfo).toString()
                val appIcon = pm.getApplicationIcon(appInfo)

                val view = layoutInflater.inflate(R.layout.activity_item_app_block, container, false)
                view.findViewById<ImageView>(R.id.appIcon).setImageDrawable(appIcon)
                view.findViewById<TextView>(R.id.appName).text = "$appName (${formatTime(tempoMs)})"

                // Restaurando funcionalidade do botão de tempo
                val timeBtn = view.findViewById<Button>(R.id.appTimerButton)
                val tempos = arrayOf("15 min", "30 min", "1 h", "2 h")
                timeBtn.setOnClickListener {
                    val builder = androidx.appcompat.app.AlertDialog.Builder(this)
                    builder.setTitle("Definir tempo de uso para $appName")
                    builder.setItems(tempos) { _, which ->
                        timeBtn.text = tempos[which]
                        getSharedPreferences("AppTempos", Context.MODE_PRIVATE)
                            .edit()
                            .putString(pkg, tempos[which])
                            .apply()
                    }
                    builder.show()
                }

                container.addView(view)
                Log.d("AppUsage", "Exibindo: $appName - ${formatTime(tempoMs)}")

            } catch (e: Exception) {
                Log.e("AppUsage", "Erro ao processar $pkg", e)
            }
        }

        Toast.makeText(this, "Apps carregados: ${container.childCount}", Toast.LENGTH_SHORT).show()
    }


    // Cálculo local robusto usando UsageEvents puro
    private fun getUsageEventsCalculated(): Map<String, Long> {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000L * 60 * 60 * 24

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        val usageMap = mutableMapOf<String, Long>()
        val foregroundTimestamps = mutableMapOf<String, Long>()

        while (events.getNextEvent(event)) {
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND, UsageEvents.Event.ACTIVITY_RESUMED -> {
                    foregroundTimestamps[event.packageName] = event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND, UsageEvents.Event.ACTIVITY_PAUSED -> {
                    foregroundTimestamps[event.packageName]?.let { start ->
                        val duration = event.timeStamp - start
                        if (duration > 0) {
                            usageMap[event.packageName] = (usageMap[event.packageName] ?: 0) + duration
                        }
                    }
                    foregroundTimestamps.remove(event.packageName)
                }
            }
        }

        foregroundTimestamps.forEach { (pkg, start) ->
            val duration = endTime - start
            if (duration > 0) {
                usageMap[pkg] = (usageMap[pkg] ?: 0) + duration
            }
        }

        Log.d("UsageEvents", "Apps detectados: ${usageMap.size}")
        return usageMap
    }

    private fun formatTime(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%dh %02dm %02ds", hours, minutes, secs)
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
