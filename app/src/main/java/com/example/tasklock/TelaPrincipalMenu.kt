package com.example.tasklock

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.tasklock.data.db.AppUsageDatabase
import com.example.tasklock.data.model.UserPreferences
import com.example.tasklock.databinding.ActivityTelaprincipalmenuBinding
import kotlinx.coroutines.*

class TelaPrincipalMenu : BaseActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityTelaprincipalmenuBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTelaprincipalmenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = UserPreferences(this)
        val emailLogado = prefs.getEmailUsuarioLogado()
        if (emailLogado == null) {
            redirecionarParaLogin()
            return
        }

        setSupportActionBar(binding.appBarTelaprincipalmenu.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_telaprincipalmenu)

        drawerToggle = ActionBarDrawerToggle(
            this, drawerLayout, binding.appBarTelaprincipalmenu.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {
                atualizarCabecalhoUsuario(emailLogado)
            }
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_usoapp,
                R.id.nav_appsbloqueados
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

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
                            prefs.logout()
                            redirecionarParaLogin()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
            drawerLayout.closeDrawers()
            true
        }

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

        atualizarCabecalhoUsuario(emailLogado)
    }

    private fun redirecionarParaLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun atualizarCabecalhoUsuario(email: String) {
        val navigationView = binding.navView
        navigationView.removeHeaderView(navigationView.getHeaderView(0))
        val headerView = layoutInflater.inflate(R.layout.nav_header_telaprincipalmenu, navigationView, false)
        navigationView.addHeaderView(headerView)

        val txtNome = headerView.findViewById<TextView>(R.id.nomeusuario)
        val txtEmail = headerView.findViewById<TextView>(R.id.email)
        val txtNomeHome = findViewById<TextView?>(R.id.txtNomeUsuario)

        CoroutineScope(Dispatchers.IO).launch {
            val usuario = AppUsageDatabase.getInstance(this@TelaPrincipalMenu).usuarioDao().buscarPorEmail(email)
            withContext(Dispatchers.Main) {
                txtNome.text = usuario?.nome ?: "Nome Usuário"
                txtEmail.text = usuario?.email ?: "Email"
                txtNomeHome?.text = usuario?.nome ?: "Usuário"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val email = UserPreferences(this).getEmailUsuarioLogado()
        if (email != null) atualizarCabecalhoUsuario(email)
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
                        redirecionarParaLogin()
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
