package com.example.tasklock

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tasklock.data.db.AppUsageDatabase
import com.example.tasklock.data.model.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

        btnEntrar.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val senha = senhaInput.text.toString().trim()

            val db = AppUsageDatabase.getInstance(this)
            CoroutineScope(Dispatchers.IO).launch {
                val user = db.usuarioDao().buscarPorEmail(email)
                if (user != null && user.senha == senha) {
                    UserPreferences(this@telaloginusuario).setUsuarioLogado(email)

                    val intent = Intent(this@telaloginusuario, TelaPrincipalMenu::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    runOnUiThread {
                        Toast.makeText(this@telaloginusuario, "Email ou senha incorretos", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        txtEsqueceuSenha.setOnClickListener {
            startActivity(Intent(this, TrocarSenhaUsuario::class.java))
        }

        txtVoltarCadastro.setOnClickListener {
            irParaCadastro()
        }

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