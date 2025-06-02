package com.example.tasklock

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tasklock.data.model.UserPreferences

class telaloginusuario : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_telaloginusuario)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val emailInput = findViewById<EditText>(R.id.campLoginEmail)
        val senhaInput = findViewById<EditText>(R.id.campLoginSenha)
        val btnEntrar = findViewById<Button>(R.id.btnLogin)
        val txtEsqueceuSenha = findViewById<TextView>(R.id.textViewAnchor)
        val txtVoltarCadastro = findViewById<TextView>(R.id.textView6)
        val btnVoltar = findViewById<Button?>(R.id.setavoltarCad)

        // Botão de login
        btnEntrar.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val senha = senhaInput.text.toString().trim()

            val prefs = UserPreferences(this)
            val user = prefs.getUser()

            if (user != null && user.email == email && user.senha == senha) {
                prefs.setLoggedIn(true)

                val intent = Intent(this, TelaPrincipalMenu::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Email ou senha incorretos", Toast.LENGTH_SHORT).show()
            }
        }

        // Redirecionar para troca de senha
        txtEsqueceuSenha.setOnClickListener {
            startActivity(Intent(this, TrocarSenhaUsuario::class.java))
        }

        // Redirecionar para tela de cadastro via TextView
        txtVoltarCadastro.setOnClickListener {
            irParaCadastro()
        }

        // Redirecionar para tela de cadastro via botão (se estiver presente no layout)
        btnVoltar?.setOnClickListener {
            irParaCadastro()
        }
    }

    private fun irParaCadastro() {
        val intent = Intent(this, Cadastro::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
