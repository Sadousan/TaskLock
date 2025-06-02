package com.example.tasklock

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tasklock.data.model.UserPreferences

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)

        // Verifica se o usuário já está logado ANTES de carregar a interface
        val prefs = UserPreferences(this)
        if (prefs.isLoggedIn()) {
            // Redireciona direto para a tela principal e finaliza a MainActivity
            val intent = Intent(this, TelaPrincipalMenu::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnComecar = findViewById<Button>(R.id.btn_comecarPrincipal)
        btnComecar.setOnClickListener {
            // Vai para tela de cadastro (fluxo inicial)
            val intent = Intent(this, Cadastro::class.java)
            startActivity(intent)
            finish() // finaliza a MainActivity para não voltar nela ao pressionar "voltar"
        }
    }
}
