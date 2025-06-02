

package com.example.tasklock

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.tasklock.data.db.AppUsageDatabase
import com.example.tasklock.utils.AppInfoProvider.appInfoManual
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.net.Uri
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.widget.Toolbar


class UsoApp : BaseActivity() {

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


        findViewById<Button>(R.id.btnBlock).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                AlertDialog.Builder(this)
                    .setTitle("Permissão necessária")
                    .setMessage("Para que o TaskLock funcione corretamente e bloqueie apps com sobreposição, conceda permissão de sobreposição.")
                    .setPositiveButton("Conceder") { _, _ ->
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } else {
                bloquearAppsSelecionados()
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

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false) // Oculta o título padrão herdado
        findViewById<TextView>(R.id.toolbar_title)?.text = "Uso de aplicativos"

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close

        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val navView = findViewById<NavigationView>(R.id.nav_view)
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, TelaPrincipalMenu::class.java).apply {
                        putExtra("navigate_to", R.id.nav_home)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    finish()
                    true
                }

                R.id.nav_usoapp -> {
                    if (this !is UsoApp) {
                        startActivity(Intent(this, UsoApp::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_appsbloqueados -> {
                    if (this !is BlockedAppsActivity) {
                        startActivity(Intent(this, BlockedAppsActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_adicionartarefa -> {
                    if (this !is AdicionarTarefaActivity) {
                        startActivity(Intent(this, AdicionarTarefaActivity::class.java))
                        finish()
                    }
                    true
                }
                else -> false
            }.also {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }




    }

    override fun onResume() {
        super.onResume()

        if (todasPermissoesConcedidas()) {
            carregarAppsDoBanco()
        }
    }

    private fun bloquearAppsSelecionados() {
        val container = findViewById<LinearLayout>(R.id.appListContainer)
        val dao = AppUsageDatabase.getInstance(this).blockedAppsDao()

        val appsParaBloquear = mutableListOf<String>()

        for (i in 0 until container.childCount) {
            val itemView = container.getChildAt(i)
            val checkBox = itemView.findViewById<CheckBox>(R.id.appCheckBox)

            val packageName = itemView.tag as String

            if (checkBox.isChecked) {
                appsParaBloquear.add(packageName)
            }
        }

        if (appsParaBloquear.isEmpty()) {
            Toast.makeText(this, "Nenhum app selecionado para bloqueio.", Toast.LENGTH_SHORT).show()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Confirmar bloqueio")
                .setMessage(
                    "Você deseja bloquear os apps?\n\n" +
                            appsParaBloquear.joinToString("\n- ", "- ") { pkg ->
                                appInfoManual[pkg]?.first ?: pkg
                            }
                )
                .setPositiveButton("Sim") { dialog, _ ->
                    lifecycleScope.launch {
                        appsParaBloquear.forEach { packageName ->
                            val currentLimit = getLimitFromPreferences(packageName)
                            val appName = appInfoManual[packageName]?.first ?: packageName
                            val iconResId = appInfoManual[packageName]?.second

                            val iconBase64 = iconResId?.let { resId ->
                                val drawable = getDrawable(resId)
                                (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap?.let { bitmap ->
                                    val stream = java.io.ByteArrayOutputStream()
                                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                                    android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.DEFAULT)
                                }
                            } ?: ""

                            dao.insertOrUpdate(
                                com.example.tasklock.data.model.BlockedAppEntity(
                                    packageName = packageName,
                                    appName = appName,
                                    iconBase64 = iconBase64,
                                    dailyLimitMs = currentLimit,
                                    usedTodayMs = 0L,
                                    bonusMs = 0L
                                )
                            )
                        }

                        carregarAppsDoBanco()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
                .show()
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
        val daoBlocked = AppUsageDatabase.getInstance(this).blockedAppsDao()

        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                dao.getAllAgrupado()
            }

            val appsBloqueados = withContext(Dispatchers.IO) {
                daoBlocked.getAll().map { it.packageName }
            }

            val appsFiltrados = apps
                .filter { appInfoManual.containsKey(it.packageName) }
                .filterNot { deveOcultarApp(it.packageName) }
                .filterNot { appsBloqueados.contains(it.packageName) } // Ocultar apps bloqueados
                .distinctBy { it.packageName }
                .sortedByDescending { it.totalTimeMs }

            if (appsFiltrados.isEmpty()) {
                Toast.makeText(this@UsoApp, "Nenhum app encontrado no banco ainda.", Toast.LENGTH_LONG).show()
                return@launch
            }

            chart.setData(appsFiltrados, appInfoManual)

            appsFiltrados.forEach { app ->
                val view = layoutInflater.inflate(R.layout.activity_item_app_block, container, false)

                view.tag = app.packageName

                val manual = appInfoManual[app.packageName]
                if (manual != null) {
                    view.findViewById<ImageView>(R.id.appIcon).setImageResource(manual.second)
                    view.findViewById<TextView>(R.id.appName).text =
                        "${manual.first} (${formatTime(app.totalTimeMs)})"
                }

                setupTimerButton(view, app.packageName, manual?.first ?: app.packageName)

                val checkBox = view.findViewById<CheckBox>(R.id.appCheckBox)

                checkBox.isChecked = false // Nenhum vem marcado

                container.addView(view)
            }
        }
    }


    private fun deveOcultarApp(pkg: String): Boolean {
        return prefixosSistema.any { pkg.startsWith(it) } ||
                pkg.contains("launcher") ||
                pkg.contains("setupwizard")
    }

    private fun setupTimerButton(view: android.view.View, pkg: String, name: String) {
        val btn = view.findViewById<Button>(R.id.appTimerButton)
        val options = arrayOf("15 min", "30 min", "1 h", "2 h")

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

                    // Atualizar no banco, se estiver bloqueado
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
                val db = AppUsageDatabase.getInstance(this@UsoApp)

                withContext(Dispatchers.IO) {
                    // Reset de uso de apps
                    db.appUsageDao().deleteAll()

                    // Reset de tarefas recorrentes marcadas como concluídas
                    db.tarefaDao().resetarTarefasRecorrentes()
                }

                Toast.makeText(
                    this@UsoApp,
                    "Registros e tarefas diárias reiniciados para o novo dia ᕙ(▿´)ᕗ.",
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
