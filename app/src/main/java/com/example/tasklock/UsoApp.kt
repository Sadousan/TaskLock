package com.example.tasklock

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tasklock.utils.AppBlocker
import com.example.tasklock.utils.AppInfoManager
import com.example.tasklock.utils.AppUsageManager
import com.example.tasklock.utils.PermissionManager
import java.text.SimpleDateFormat
import java.util.*

class UsoApp : AppCompatActivity() {
    private lateinit var appUsageManager: AppUsageManager
    private lateinit var appInfoManager: AppInfoManager
    private lateinit var appBlocker: AppBlocker
    private lateinit var permissionManager: PermissionManager
    private val selectedApps = mutableSetOf<String>()
    private var hasShownPermissionDialog = false
    private var isInitialized = false
    private var lastToastTime = 0L
    private val TOAST_COOLDOWN = 2000L // 2 segundos entre toasts

    companion object {
        private const val TAG = "UsoApp"
        private const val PERMISSION_REQUEST_CODE = 1234
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Iniciando UsoApp")
        
        // Configure theme and orientation before setContentView
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_uso_app)

        // Initialize managers
        appUsageManager = AppUsageManager.getInstance(this)
        appInfoManager = AppInfoManager.getInstance(this)
        appBlocker = AppBlocker.getInstance(this)
        permissionManager = PermissionManager.getInstance(this)

        // Setup window insets once
        val mainScroll = findViewById<ScrollView>(R.id.Main)
        ViewCompat.setOnApplyWindowInsetsListener(mainScroll) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup UI elements
        setupUI()
        
        // Check permissions
        if (!isInitialized) {
            verificarPermissaoEExecutar()
            isInitialized = true
        }
    }

    private fun setupUI() {
        // Refresh button
        findViewById<ImageButton>(R.id.btnRefresh)?.setOnClickListener {
            Log.d(TAG, "onClick: Botão de atualização pressionado")
            carregarApps()
        }

        // Block button
        findViewById<Button>(R.id.btnBlock)?.setOnClickListener {
            Log.d(TAG, "onClick: Botão de bloqueio pressionado. Apps selecionados: ${selectedApps.size}")
            if (selectedApps.isEmpty()) {
                showError("Selecione pelo menos um aplicativo para bloquear")
                return@setOnClickListener
            }
            showBlockDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Verificando estado das permissões")
        if (!hasShownPermissionDialog && !hasUsageStatsPermission()) {
            Log.d(TAG, "onResume: Permissões não concedidas, mostrando diálogo")
            verificarPermissaoEExecutar()
        } else {
            Log.d(TAG, "onResume: Carregando apps")
            carregarApps()
        }
    }

    private fun verificarPermissaoEExecutar() {
        Log.d(TAG, "verificarPermissaoEExecutar: Iniciando verificação")
        if (!appUsageManager.isUsageStatsSupported()) {
            Log.e(TAG, "verificarPermissaoEExecutar: Dispositivo não suporta monitoramento de uso")
            showError("Seu dispositivo não suporta o monitoramento de uso de apps")
            return
        }

        if (!hasUsageStatsPermission()) {
            Log.d(TAG, "verificarPermissaoEExecutar: Permissão de uso não concedida")
            hasShownPermissionDialog = true
            showPermissionDialog()
        } else {
            Log.d(TAG, "verificarPermissaoEExecutar: Permissão de uso já concedida")
            showSuccess("Permissões ativas! Carregando apps...")
            carregarApps()
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun showPermissionDialog() {
        Log.d(TAG, "showPermissionDialog: Exibindo diálogo de permissões")
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Permissões Necessárias")
        builder.setMessage("Para monitorar o uso de apps, precisamos de algumas permissões. Deseja configurá-las agora?")
        builder.setPositiveButton("Configurar") { _, _ ->
            Log.d(TAG, "showPermissionDialog: Usuário escolheu configurar permissões")
            startActivityForResult(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), PERMISSION_REQUEST_CODE)
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            Log.d(TAG, "showPermissionDialog: Usuário cancelou configuração de permissões")
            dialog.dismiss()
            hasShownPermissionDialog = false
            showError("Sem as permissões necessárias, o app não funcionará corretamente")
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun showError(message: String) {
        Log.e(TAG, "showError: $message")
        showToast(message, Toast.LENGTH_LONG)
    }

    private fun showSuccess(message: String) {
        Log.d(TAG, "showSuccess: $message")
        showToast(message, Toast.LENGTH_SHORT)
    }

    private fun showToast(message: String, duration: Int) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastToastTime >= TOAST_COOLDOWN) {
            Toast.makeText(this, message, duration).show()
            lastToastTime = currentTime
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (hasUsageStatsPermission()) {
                Log.d(TAG, "onActivityResult: Permissões concedidas após configuração")
                showSuccess("Permissões concedidas com sucesso! Carregando apps...")
                carregarApps()
            } else {
                Log.d(TAG, "onActivityResult: Permissões ainda não concedidas após configuração")
                showPermissionDialog()
            }
        }
    }

    private fun carregarApps() {
        Log.d(TAG, "carregarApps: Iniciando carregamento")
        val container = findViewById<LinearLayout>(R.id.appListContainer)
        container.removeAllViews()
        selectedApps.clear()

        try {
            // Obtém apps instalados
            val installedApps = appInfoManager.getInstalledApps()
            Log.d(TAG, "carregarApps: ${installedApps.size} apps instalados encontrados")

            if (installedApps.isEmpty()) {
                Log.w(TAG, "carregarApps: Nenhum app instalado detectado")
                showError("Nenhum app instalado detectado")
                return
            }

            // Obtém informações adicionais
            val additionalInfo = appInfoManager.getAdditionalAppInfo()
            Log.d(TAG, "carregarApps: ${additionalInfo.size} apps com informações adicionais")

            // Formata a data
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

            installedApps.forEach { (packageName, appName) ->
                try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    val appIcon = packageManager.getApplicationIcon(appInfo)

                    val view = layoutInflater.inflate(R.layout.activity_item_app_block, container, false)
                    view.findViewById<ImageView>(R.id.appIcon).setImageDrawable(appIcon)
                    
                    // Adiciona informações adicionais se disponíveis
                    val additional = additionalInfo[packageName]
                    val lastUpdate = additional?.lastUpdateTime?.let { dateFormat.format(Date(it)) } ?: "Desconhecido"
                    view.findViewById<TextView>(R.id.appName).text = "$appName\nÚltima atualização: $lastUpdate"

                    // Configura o checkbox
                    val checkBox = view.findViewById<CheckBox>(R.id.appCheckBox)
                    checkBox.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            selectedApps.add(packageName)
                            Log.d(TAG, "App selecionado: $appName")
                        } else {
                            selectedApps.remove(packageName)
                            Log.d(TAG, "App desselecionado: $appName")
                        }
                    }

                    // Configura o botão de bloqueio
                    val blockButton = view.findViewById<Button>(R.id.appTimerButton)
                    val remainingTime = appBlocker.getRemainingBlockTime(packageName)
                    
                    if (remainingTime > 0) {
                        blockButton.text = "Bloqueado: ${remainingTime}m"
                        blockButton.isEnabled = false
                        Log.d(TAG, "App $appName está bloqueado por mais $remainingTime minutos")
                    } else {
                        blockButton.text = "Bloquear"
                        blockButton.setOnClickListener {
                            showBlockDialog(packageName)
                        }
                    }

                    container.addView(view)
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao processar app $packageName", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao carregar apps", e)
            showError("Erro ao carregar lista de apps")
        }
    }

    private fun showBlockDialog(packageName: String? = null) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Bloquear Apps")
        builder.setMessage("Por quanto tempo deseja bloquear ${if (packageName != null) "este app" else "os apps selecionados"}?")
        
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.hint = "Tempo em minutos"
        builder.setView(input)

        builder.setPositiveButton("Bloquear") { _, _ ->
            val timeStr = input.text.toString()
            if (timeStr.isNotEmpty()) {
                val time = timeStr.toIntOrNull()
                if (time != null && time > 0) {
                    if (packageName != null) {
                        appBlocker.blockApp(packageName, time)
                    } else {
                        selectedApps.forEach { pkg ->
                            appBlocker.blockApp(pkg, time)
                        }
                    }
                    showSuccess("Apps bloqueados com sucesso!")
                    carregarApps()
                } else {
                    showError("Tempo inválido")
                }
            } else {
                showError("Digite um tempo válido")
            }
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }
}
