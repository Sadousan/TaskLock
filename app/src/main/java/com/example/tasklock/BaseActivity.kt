package com.example.tasklock

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.tasklock.data.db.AppUsageDatabase
import com.example.tasklock.data.model.UserPreferences
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class BaseActivity : AppCompatActivity() {

    override fun startActivity(intent: Intent?) {
        super.startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    fun atualizarCabecalhoUsuario(drawerView: NavigationView, context: Context) {
        val prefs = UserPreferences(context)
        val emailLogado = prefs.getEmailUsuarioLogado() ?: return

        drawerView.removeHeaderView(drawerView.getHeaderView(0))
        val headerView = LayoutInflater.from(context).inflate(R.layout.nav_header_telaprincipalmenu, drawerView, false)
        drawerView.addHeaderView(headerView)

        val txtNome = headerView.findViewById<TextView>(R.id.nomeusuario)
        val txtEmail = headerView.findViewById<TextView>(R.id.email)

        CoroutineScope(Dispatchers.IO).launch {
            val usuario = AppUsageDatabase.getInstance(context).usuarioDao().buscarPorEmail(emailLogado)
            withContext(Dispatchers.Main) {
                txtNome.text = usuario?.nome ?: "Nome Usuário"
                txtEmail.text = usuario?.email ?: "Email"
            }
        }
    }


    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
