package com.example.tasklock

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.example.tasklock.data.db.AppUsageDatabase
import com.example.tasklock.utils.AppInfoProvider.appInfoManual
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.appcompat.widget.Toolbar
import com.example.tasklock.data.model.UserPreferences


class BlockedAppsActivity : BaseActivity() {

    private val motivationalQuotes = listOf(
        "A disciplina pesa gramas, o arrependimento pesa toneladas.",
        "O tempo que poderia ser investido em suas metas está prestes a ser desperdiçado.",
        "O conforto de hoje é o atraso de amanhã.",
        "Seu tempo é limitado! Não desista de suas metas.",
        "Nunca se é homem enquanto se não encontra alguma coisa pela qual se estaria disposto a morrer"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked_apps)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        carregarAppsBloqueados()

        findViewById<Button>(R.id.btnUnblock).setOnClickListener {
            desbloquearAppsSelecionados()
        }
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false) // Oculta o título padrão herdado
        findViewById<TextView>(R.id.toolbar_title)?.text = "Aplicativos bloqueados"

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
        atualizarCabecalhoUsuario(navView, this)
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
                }R.id.nav_usoapp -> {
                    if (this !is UsoApp) {
                        startActivity(Intent(this, UsoApp::class.java))
                        finish()
                    }
                    true
                }R.id.nav_appsbloqueados -> {
                    if (this !is BlockedAppsActivity) {
                        startActivity(Intent(this, BlockedAppsActivity::class.java))
                        finish()
                    }
                    true
                }R.id.nav_adicionartarefa -> {
                    if (this !is AdicionarTarefaActivity) {
                        startActivity(Intent(this, AdicionarTarefaActivity::class.java))
                        finish()
                    }
                    true
                }R.id.action_logout -> {
                    AlertDialog.Builder(this)
                        .setTitle("Sair")
                        .setMessage("Deseja realmente sair?")
                        .setPositiveButton("Sim") { _, _ ->
                            UserPreferences(this).logout()
                            startActivity(Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                            finish()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                    return@setNavigationItemSelectedListener true
                }
                else -> false
            }.also {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }


    }

    private fun desbloquearAppsSelecionados() {
        val container = findViewById<LinearLayout>(R.id.blockedAppListContainer)
        val dao = AppUsageDatabase.getInstance(this).blockedAppsDao()

        val appsParaDesbloquear = mutableListOf<String>()

        for (i in 0 until container.childCount) {
            val itemView = container.getChildAt(i)
            val checkBox = itemView.findViewById<CheckBox>(R.id.appCheckBox)
            val packageName = itemView.tag as String

            if (checkBox.isChecked) {
                appsParaDesbloquear.add(packageName)
            }
        }

        if (appsParaDesbloquear.isEmpty()) {
            Toast.makeText(this, "Nenhum app selecionado.", Toast.LENGTH_SHORT).show()
        } else {
            val randomMessage = motivationalQuotes.random()

            AlertDialog.Builder(this)
                .setTitle("Deseja realmente desbloquear?")
                .setMessage(
                    "$randomMessage\n\nApps selecionados:\n" +
                            appsParaDesbloquear.joinToString("\n- ", "- ") { pkg ->
                                appInfoManual[pkg]?.first ?: pkg
                            }
                )
                .setPositiveButton("Sim") { dialog, _ ->
                    lifecycleScope.launch {
                        appsParaDesbloquear.forEach {
                            dao.remove(it)
                        }
                        carregarAppsBloqueados()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
                .show()
        }
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

                view.tag = app.packageName

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
                    val options = arrayOf("1 min", "15 min", "30 min", "1 h", "2 h")

                    AlertDialog.Builder(this@BlockedAppsActivity)
                        .setTitle("Definir limite de uso")
                        .setItems(options) { _, i ->
                            val chosen = options[i]
                            val newLimit = when (chosen) {
                                "1 min" -> 1 * 60 * 1000L
                                "15 min" -> 15 * 60 * 1000L
                                "30 min" -> 30 * 60 * 1000L
                                "1 h" -> 60 * 60 * 1000L
                                "2 h" -> 120 * 60 * 1000L
                                else -> 30 * 60 * 1000L
                            }


                            AlertDialog.Builder(this@BlockedAppsActivity)
                                .setTitle("Confirmar alteração")
                                .setMessage(
                                    "Você realmente deseja definir o limite de uso para ${manual?.first ?: app.packageName} como $chosen?"
                                )
                                .setPositiveButton("Confirmar") { confirmDialog, _ ->
                                    lifecycleScope.launch {
                                        dao.insertOrUpdate(
                                            app.copy(dailyLimitMs = newLimit)
                                        )
                                        btn.text = chosen
                                        Toast.makeText(
                                            this@BlockedAppsActivity,
                                            "Limite atualizado com sucesso!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    confirmDialog.dismiss()
                                }
                                .setNegativeButton("Cancelar") { confirmDialog, _ ->
                                    confirmDialog.dismiss()
                                }
                                .show()
                        }
                        .show()
                }

                val checkBox = view.findViewById<CheckBox>(R.id.appCheckBox)
                checkBox.isChecked = false

                container.addView(view)
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
