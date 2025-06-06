package com.example.tasklock

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tasklock.data.db.AppUsageDatabase
import com.example.tasklock.data.model.UserPreferences
import com.example.tasklock.data.model.UsuarioEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Cadastro : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cadastro)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val nomeInput = findViewById<EditText>(R.id.campcadastrarnome)
        val emailInput = findViewById<EditText>(R.id.campemailcadastadro)
        val senhaInput = findViewById<EditText>(R.id.campcadastrarsenha)
        val confirmarInput = findViewById<EditText>(R.id.campcadastrarconfirmarsenha)
        val btnCadastrar = findViewById<Button>(R.id.button)
        val loginText = findViewById<TextView>(R.id.textViewAnchor)

        btnCadastrar.setOnClickListener {
            val nome = nomeInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val senha = senhaInput.text.toString().trim()
            val confirmar = confirmarInput.text.toString().trim()

            if (nome.isEmpty() || email.isEmpty() || senha.isEmpty() || confirmar.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!email.contains("@") || !email.contains(".")) {
                Toast.makeText(this, "Email inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (senha.length < 6) {
                Toast.makeText(this, "A senha deve ter no mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val regexSenha = Regex("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$")
            if (!regexSenha.matches(senha)) {
                Toast.makeText(this, "A senha deve conter letras e números", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (senha != confirmar) {
                Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = AppUsageDatabase.getInstance(this)
            CoroutineScope(Dispatchers.IO).launch {
                val existente = db.usuarioDao().buscarPorEmail(email)
                if (existente != null) {
                    runOnUiThread {
                        Toast.makeText(this@Cadastro, "Este email já está cadastrado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    db.usuarioDao().inserir(UsuarioEntity(nome = nome, email = email, senha = senha))
                    UserPreferences(this@Cadastro).setUsuarioLogado(email)

                    val intent = Intent(this@Cadastro, TelaPrincipalMenu::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }

        loginText.setOnClickListener {
            startActivity(Intent(this, telaloginusuario::class.java))
            finish()
        }
    }
}
