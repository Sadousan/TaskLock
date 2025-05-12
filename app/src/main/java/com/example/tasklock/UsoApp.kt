package com.example.tasklock

import android.app.usage.UsageStats
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
import com.example.tasklock.data.model.AppUsageData


class UsoApp : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_uso_app)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Verifica se o app tem permissão de acesso ao uso de apps
        if (!temPermissaoDeUso()) {
            // Redireciona o usuário para a tela onde ele pode ativar essa permissão manualmente
            Toast.makeText(this, "Conceda permissão de acesso ao uso para continuar.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            return
        }

// Se a permissão estiver ok, carrega os dados
        carregarAppsMaisUsados()

    }

    // Responsável por inflar os apps na tela
    private fun carregarAppsMaisUsados() {
        val container = findViewById<LinearLayout>(R.id.appListContainer)
        val usageList = getAppUsageStats(this)

        val pm = packageManager

        for (appData in usageList) {
            try {
                // Verifica se o pacote ainda é considerado instalado no dispositivo
                val installedPackages = pm.getInstalledPackages(0)
                val isInstalled = installedPackages.any { it.packageName == appData.packageName }

                if (!isInstalled) {
                    Log.w("AppUsage", "Ignorado pacote não instalado: ${appData.packageName}")
                    continue
                }

                // Obtém informações visuais do app (ícone, nome)
                val appInfo = pm.getApplicationInfo(appData.packageName, 0)
                val appName = pm.getApplicationLabel(appInfo).toString()
                val appIcon = pm.getApplicationIcon(appInfo)

                // Infla layout do item de lista (reutilizável)
                val view = layoutInflater.inflate(R.layout.activity_item_app_block, container, false)

                val iconView = view.findViewById<ImageView>(R.id.appIcon)
                val nameView = view.findViewById<TextView>(R.id.appName)
                val timeBtn = view.findViewById<Button>(R.id.appTimerButton)
                val checkBox = view.findViewById<CheckBox>(R.id.appCheckBox)

                iconView.setImageDrawable(appIcon)
                nameView.text = appName

                // Ação do botão de tempo: grava o tempo definido em SharedPreferences
                timeBtn.setOnClickListener {
                    val tempos = arrayOf("15 min", "30 min", "1 h", "2 h")
                    val builder = androidx.appcompat.app.AlertDialog.Builder(this)
                    builder.setTitle("Definir tempo de uso para $appName")
                    builder.setItems(tempos) { _, which ->
                        timeBtn.text = tempos[which]
                        getSharedPreferences("AppTempos", Context.MODE_PRIVATE)
                            .edit()
                            .putString(appData.packageName, tempos[which])
                            .apply()
                    }
                    builder.show()
                }

                // Adiciona o item visual ao container principal
                container.addView(view)
                Log.d("AppUsage", "$appName: ${appData.totalTime / 1000} segundos")

            } catch (e: PackageManager.NameNotFoundException) {
                Log.w("AppUsage", "App não encontrado ou acesso negado: ${appData.packageName}")
            } catch (e: Exception) {
                Log.e("AppUsage", "Erro ao processar ${appData.packageName}", e)
            }
        }

        Toast.makeText(this, "Apps carregados: ${container.childCount}", Toast.LENGTH_SHORT).show()
    }





    // Retorna lista crua de usage stats
    private fun getAppUsageStats(context: Context): List<AppUsageData> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000L * 60 * 60 * 24 * 3 // últimos 3 dias

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            startTime,
            endTime
        )

        val packageManager = context.packageManager
        val timeMap = mutableMapOf<String, Long>()

        for (stat in stats) {
            val pkg = stat.packageName
            if (stat.totalTimeInForeground > 0 &&
                pkg != "com.mi.android.globallauncher" &&
                pkg != packageName
            ) {
                try {
                    // Apenas conta se o app ainda estiver instalado
                    packageManager.getApplicationInfo(pkg, 0)
                    val current = timeMap[pkg] ?: 0L
                    timeMap[pkg] = current + stat.totalTimeInForeground
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.w("AppStatsFilter", "Ignorado (não instalado): $pkg")
                }
            }
        }

        // Log de apps filtrados
        for ((pkg, tempo) in timeMap) {
            Log.d("AppStatsFiltrado", "$pkg => ${tempo / 1000}s")
        }

        return timeMap.entries
            .sortedByDescending { it.value }
            .take(15)
            .map { AppUsageData(it.key, it.value) }
    }



    // Verifica permissão para acessar dados de uso
    private fun hasUsageAccessPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            "android:get_usage_stats",
            android.os.Process.myUid(),
            packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }
    // Verifica se a permissão de acesso ao uso está ativa
    private fun temPermissaoDeUso(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

}
