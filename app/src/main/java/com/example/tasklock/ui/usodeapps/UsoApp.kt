package com.example.tasklock

import android.accessibilityservice.AccessibilityServiceInfo
import com.example.tasklock.utils.AppInfoProvider.appInfoManual
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.accessibility.AccessibilityManager
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.tasklock.data.db.AppUsageDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UsoApp : AppCompatActivity() {

    private lateinit var permissionLauncher: ActivityResultLauncher<Intent>
    private lateinit var chart: AppUsageChart



    private val prefixosSistema = listOf(
        "com.google.android.gms", "com.android.systemui", "com.android.phone", "com.android.settings",
        "com.miui.", "com.samsung.", "com.sec.android", "com.motorola.",
        "com.google.android.inputmethod", "com.xiaomi.", "com.oppo.", "com.coloros.",
        "com.realme.", "com.vivo.", "com.huawei.", "android"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uso_app)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        chart = findViewById(R.id.chart)
        inicializarPermissionLauncher()
        verificarPermissoes()

        findViewById<Button>(R.id.btnBlock)?.setOnClickListener {
            val container = findViewById<LinearLayout>(R.id.appListContainer)
            val dao = AppUsageDatabase.getInstance(this).blockedAppsDao()

            val appsMarcados = mutableListOf<String>()

            for (i in 0 until container.childCount) {
                val itemView = container.getChildAt(i)
                val checkBox = itemView.findViewById<CheckBox>(R.id.appCheckBox)
                val appName = itemView.findViewById<TextView>(R.id.appName).text.toString()

                if (checkBox.isChecked) {
                    appsMarcados.add(appName)
                }
            }

            if (appsMarcados.isEmpty()) {
                Toast.makeText(this, "Nenhum app selecionado para bloqueio.", Toast.LENGTH_SHORT).show()
            } else {
                val nomes = appsMarcados.joinToString("\n") { "- $it" }

                AlertDialog.Builder(this)
                    .setTitle("Apps bloqueados")
                    .setMessage("Os seguintes apps estão bloqueados:\n\n$nomes")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }


        findViewById<ImageButton>(R.id.btnHelp)?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Algo de errado? ✍(◔◡◔)")
                .setMessage(
                    "O TaskLock começará a registrar seu uso de aplicativos (considerados distrativos) a partir do momento em que foi iniciado.\n\n" +
                            "Os dados se tornarão mais precisos ao longo do dia.\n\n" +
                            "Retorne após alguns instantes, para acompanhar suas estatísticas!"
                )
                .setPositiveButton("Entendido") { dialog, _ ->
                    dialog.dismiss()
                    carregarAppsDoBanco()
                }
                .show()
        }
        TestScheduler.scheduleTestNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

    }

    override fun onResume() {
        super.onResume()
        checarEDefinirResetDiario()

        if (todasPermissoesConcedidas()) {
            carregarAppsDoBanco()
        }
    }


    private fun inicializarPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            verificarPermissoes()
        }
    }

    private fun verificarPermissoes() {
        when {
            !hasUsageStatsPermission() -> {
                solicitarPermissao(
                    "Permissão de uso necessária",
                    "Ative o acesso de uso para monitorar os aplicativos.",
                    Settings.ACTION_USAGE_ACCESS_SETTINGS
                )
            }
            !isAccessibilityEnabled() -> {
                solicitarPermissao(
                    "Permissão de acessibilidade necessária",
                    "Ative o TaskLock na acessibilidade para monitoramento correto.",
                    Settings.ACTION_ACCESSIBILITY_SETTINGS
                )
            }
            !hasBackgroundPermission() && !jaSolicitouBackground() -> {
                solicitarPermissaoDeBackground()
            }
            else -> {
                if (!boasVindasJaMostrada()) {
                    mostrarTelaBoasVindas()
                }
                carregarAppsDoBanco()
            }
        }
    }

    private fun solicitarPermissao(titulo: String, mensagem: String, intentAction: String) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensagem)
            .setCancelable(false)
            .setPositiveButton("Permitir") { _, _ ->
                try {
                    permissionLauncher.launch(Intent(intentAction))
                } catch (e: Exception) {
                    permissionLauncher.launch(Intent(Settings.ACTION_SETTINGS))
                }
            }
            .show()
    }

    private fun solicitarPermissaoDeBackground() {
        AlertDialog.Builder(this)
            .setTitle("Permissão de segundo plano")
            .setMessage("Ative o início automático e funcionamento em segundo plano para garantir funcionamento contínuo do TaskLock.")
            .setCancelable(false)
            .setPositiveButton("Permitir") { _, _ ->
                try {
                    val intent = Intent().apply {
                        component = ComponentName(
                            "com.miui.securitycenter",
                            "com.miui.permcenter.autostart.AutoStartManagementActivity"
                        )
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }

                getSharedPreferences("prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("background_permissao_solicitada", true).apply()
            }
            .show()
    }

    private fun jaSolicitouBackground(): Boolean {
        return getSharedPreferences("prefs", Context.MODE_PRIVATE)
            .getBoolean("background_permissao_solicitada", false)
    }

    private fun mostrarTelaBoasVindas() {
        AlertDialog.Builder(this)
            .setTitle("Tudo pronto!")
            .setMessage(
                "O TaskLock começará a registrar seu uso de aplicativos.\n\n" +
                        "Os dados se tornarão mais precisos ao longo do dia.\n\n" +
                        "Volte depois para acompanhar suas estatísticas!"
            )
            .setPositiveButton("Ok") { dialog, _ ->
                dialog.dismiss()
                getSharedPreferences("prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("boas_vindas_mostrada", true).apply()
            }
            .show()
    }

    private fun boasVindasJaMostrada(): Boolean {
        return getSharedPreferences("prefs", Context.MODE_PRIVATE)
            .getBoolean("boas_vindas_mostrada", false)
    }

    private fun todasPermissoesConcedidas(): Boolean {
        return hasUsageStatsPermission() && isAccessibilityEnabled() && hasBackgroundPermission()
    }

    private fun carregarAppsDoBanco() {
        val container = findViewById<LinearLayout>(R.id.appListContainer)
        container.removeAllViews()

        val dao = AppUsageDatabase.getInstance(this).appUsageDao()
        val pm = packageManager

        Thread {
            val apps = dao.getAllAgrupado()

            runOnUiThread {
                if (apps.isEmpty()) {
                    Toast.makeText(this, "Nenhum app encontrado no banco ainda.", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                val appsFiltrados = apps
                    .filter { appInfoManual.containsKey(it.packageName) } // Filtro por Map
                    .filterNot { deveOcultarApp(it.packageName) }
                    .distinctBy { it.packageName }
                    .sortedByDescending { it.totalTimeMs }

                chart.setData(appsFiltrados, appInfoManual)

                appsFiltrados.forEach { app ->
                    val view = layoutInflater.inflate(R.layout.activity_item_app_block, container, false)

                    val manual = appInfoManual[app.packageName]
                    if (manual != null) {
                        view.findViewById<ImageView>(R.id.appIcon).setImageResource(manual.second)
                        view.findViewById<TextView>(R.id.appName).text =
                            "${manual.first} (${formatTime(app.totalTimeMs)})"
                    }

                    setupTimerButton(view, app.packageName, manual?.first ?: app.packageName)
                    container.addView(view)
                    val daoBlocked = AppUsageDatabase.getInstance(this).blockedAppsDao()

                    val checkBox = view.findViewById<CheckBox>(R.id.appCheckBox)

// Verificar se já está bloqueado
                    lifecycleScope.launch {
                        val isBlocked = withContext(Dispatchers.IO) {
                            daoBlocked.getByPackage(app.packageName) != null
                        }
                        checkBox.isChecked = isBlocked
                    }

                    checkBox.setOnCheckedChangeListener { _, isChecked ->
                        lifecycleScope.launch {
                            if (isChecked) {
                                val currentLimit = getLimitFromPreferences(app.packageName)
                                daoBlocked.insertOrUpdate(
                                    com.example.tasklock.data.model.BlockedAppEntity(
                                        packageName = app.packageName,
                                        dailyLimitMs = currentLimit,
                                        usedTodayMs = 0L
                                    )
                                )
                            } else {
                                daoBlocked.remove(app.packageName)
                            }
                        }
                    }

                }
            }
        }.start()
    }


    private fun deveOcultarApp(pkg: String): Boolean {
        return prefixosSistema.any { pkg.startsWith(it) } ||
                pkg.contains("launcher") ||
                pkg.contains("setupwizard")
    }

    private fun setupTimerButton(view: android.view.View, pkg: String, name: String) {
        val btn = view.findViewById<Button>(R.id.appTimerButton)
        val options = arrayOf("15 min", "30 min", "1 h", "2 h")

        // Mostrar valor salvo
        val label = getSharedPreferences("AppTempos", Context.MODE_PRIVATE).getString(pkg, "30 min")
        btn.text = label

        btn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Definir tempo de uso para $name")
                .setItems(options) { _, i ->
                    val chosen = options[i]
                    btn.text = chosen

                    getSharedPreferences("AppTempos", Context.MODE_PRIVATE)
                        .edit().putString(pkg, chosen).apply()

                    // Atualizar também no banco, se já estiver bloqueado
                    lifecycleScope.launch {
                        val dao = AppUsageDatabase.getInstance(this@UsoApp).blockedAppsDao()
                        val blocked = withContext(Dispatchers.IO) {
                            dao.getByPackage(pkg)
                        }
                        if (blocked != null) {
                            dao.insertOrUpdate(
                                blocked.copy(
                                    dailyLimitMs = getLimitFromPreferences(pkg)
                                )
                            )
                        }
                    }
                }
                .show()
        }
    }


    private fun formatTime(ms: Long): String {
        val sec = ms / 1000
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return "%dh %02dm %02ds".format(h, m, s)
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

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        return enabledServices.any { it.resolveInfo.serviceInfo.packageName == packageName }
    }

    private fun hasBackgroundPermission(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun checarEDefinirResetDiario() {
        val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val ultimaData = prefs.getString("ultima_data", null)

        val hoje = java.text.SimpleDateFormat("yyyyMMdd").format(System.currentTimeMillis())

        if (ultimaData == null || ultimaData != hoje) {
            lifecycleScope.launch {
                val dao = AppUsageDatabase.getInstance(this@UsoApp).appUsageDao()

                withContext(Dispatchers.IO) {
                    dao.deleteAll()
                }

                Toast.makeText(
                    this@UsoApp,
                    "Registros reiniciados para o novo dia ᕙ(`▿´)ᕗ.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            prefs.edit().putString("ultima_data", hoje).apply()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Permissão de notificação não concedida. Algumas notificações não aparecerão.", Toast.LENGTH_LONG).show()
        }
    }

    private fun getLimitFromPreferences(pkg: String): Long {
        val label = getSharedPreferences("AppTempos", Context.MODE_PRIVATE).getString(pkg, "30 min")
        return when (label) {
            "15 min" -> 15 * 60 * 1000L
            "30 min" -> 30 * 60 * 1000L
            "1 h" -> 60 * 60 * 1000L
            "2 h" -> 120 * 60 * 1000L
            else -> 30 * 60 * 1000L
        }
    }


}