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
//import com.example.tasklock.ui.usodeapps.UsoApp

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        //desabilitar modo noturno
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        //bloquear rotacao da tela
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val btnMenu = findViewById<Button>(R.id.btnMenu)

        btnMenu.setOnClickListener {
            goTomenuScreen()
        }

        val btnComecar = findViewById<Button>(R.id.btn_comecarPrincipal)

        btnComecar.setOnClickListener {
            goToScreen()
        }
    }
    private fun goTomenuScreen() {
        startActivity(Intent(this, UsoApp::class.java))
    }
    private fun goToScreen() {
        startActivity(Intent(this, TelaPrincipalMenu::class.java))
    }
}