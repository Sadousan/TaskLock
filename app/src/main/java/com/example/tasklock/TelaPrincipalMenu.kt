package com.example.tasklock

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.tasklock.data.model.UserPreferences
import com.example.tasklock.databinding.ActivityTelaprincipalmenuBinding

class TelaPrincipalMenu : BaseActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityTelaprincipalmenuBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = UserPreferences(this).getUser()
        if (user == null) {
            // Usuário não está logado, redireciona para tela inicial (Main)
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        binding = ActivityTelaprincipalmenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Preenche campos do usuário
        val header = binding.navView.getHeaderView(0)
        val txtNome = header.findViewById<TextView>(R.id.nomeusuario)
        val txtEmail = header.findViewById<TextView>(R.id.email)
        val txtNomeHome = findViewById<TextView>(R.id.txtNomeUsuario)

        txtNome.text = user.nome
        txtEmail.text = user.email
        txtNomeHome.text = user.nome

        // Configura Toolbar como barra de ação
        setSupportActionBar(binding.appBarTelaprincipalmenu.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_telaprincipalmenu)

        // Cria botão do menu (≡) sincronizado com o Drawer
        drawerToggle = ActionBarDrawerToggle(
            this, drawerLayout, binding.appBarTelaprincipalmenu.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        // Define os destinos principais do menu (sem botão de voltar)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_usoapp,
                R.id.nav_appsbloqueados
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)

        // Define ações do menu lateral
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home, R.id.nav_usoapp, R.id.nav_appsbloqueados -> {
                    val currentDestination = navController.currentDestination?.id
                    val target = menuItem.itemId
                    if (currentDestination != target) {
                        navController.popBackStack()
                    }
                    Handler(Looper.getMainLooper()).post {
                        navController.navigate(target)
                    }
                }

                R.id.nav_adicionartarefa -> {
                    startActivity(Intent(this, AdicionarTarefaActivity::class.java))
                    finish()
                }

                R.id.action_logout -> {
                    AlertDialog.Builder(this)
                        .setTitle("Sair")
                        .setMessage("Deseja realmente sair?")
                        .setPositiveButton("Sim") { _, _ ->
                            UserPreferences(this).logout()
                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
            drawerLayout.closeDrawers()
            true
        }

        // Se for chamada com direcionamento para algum fragment
        if (intent.hasExtra("navigate_to") && savedInstanceState == null) {
            val navigateTo = intent.getIntExtra("navigate_to", -1)
            if (navigateTo != -1) {
                Handler(Looper.getMainLooper()).post {
                    val currentDestId = navController.currentDestination?.id
                    if (currentDestId == navigateTo) {
                        navController.popBackStack()
                    }
                    navController.navigate(navigateTo)
                    intent.removeExtra("navigate_to")
                }
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.telaprincipalmenu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                AlertDialog.Builder(this)
                    .setTitle("Sair")
                    .setMessage("Deseja realmente sair?")
                    .setPositiveButton("Sim") { _, _ ->
                        UserPreferences(this).logout()
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_telaprincipalmenu)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
