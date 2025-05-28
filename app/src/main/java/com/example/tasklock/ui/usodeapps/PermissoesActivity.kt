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
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PermissoesActivity : AppCompatActivity() {

    private lateinit var checkAcessibilidade: CheckBox
    private lateinit var checkUsoApps: CheckBox
    private lateinit var checkSegundoPlano: CheckBox
    private lateinit var btnContinuar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissoes)

        checkAcessibilidade = findViewById(R.id.checkAcessibilidade)
        checkUsoApps = findViewById(R.id.checkUso)
        checkSegundoPlano = findViewById(R.id.checkSegundoPlano)
        btnContinuar = findViewById(R.id.btnContinuar)

        atualizarChecks()

        checkAcessibilidade.setOnClickListener { abrirAcessibilidade() }
        checkUsoApps.setOnClickListener { abrirUsoApps() }
        checkSegundoPlano.setOnClickListener { abrirSegundoPlano() }

        btnContinuar.setOnClickListener {
            if (todasPermissoesConcedidas()) {
                startActivity(Intent(this, UsoApp::class.java))
                finish()
            } else {
                Toast.makeText(this, "Conceda todas as permissões!", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onResume() {
        super.onResume()
        atualizarChecks()
    }

    private fun atualizarChecks() {
        checkAcessibilidade.isChecked = isAccessibilityEnabled()
        checkUsoApps.isChecked = hasUsageStatsPermission()
        checkSegundoPlano.isChecked = hasBackgroundPermission()
    }

    private fun abrirAcessibilidade() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun abrirUsoApps() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    private fun abrirSegundoPlano() {
        try {
            val intent = when {
                Build.MANUFACTURER.equals("xiaomi", true) -> Intent().apply {
                    component = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                }
                Build.MANUFACTURER.equals("samsung", true) -> Intent(Settings.ACTION_APPLICATION_SETTINGS)
                else -> Intent(Settings.ACTION_APPLICATION_SETTINGS)
            }
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun todasPermissoesConcedidas(): Boolean {
        return isAccessibilityEnabled() && hasUsageStatsPermission() && hasBackgroundPermission()
    }

    //Utilidades para checar permissões

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        return enabledServices.any { it.resolveInfo.serviceInfo.packageName == packageName }
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

    private fun hasBackgroundPermission(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }
}
