package com.example.tasklock

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tasklock.data.db.AppUsageDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrocarSenhaUsuario : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_trocarsenhausuario)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val emailInput = findViewById<EditText>(R.id.emailparatrocardesenha)
        val novaSenhaInput = findViewById<EditText>(R.id.editTextNovaSenha)
        val confirmarSenhaInput = findViewById<EditText>(R.id.editTextConfirmarSenha)
        val btnTrocarSenha = findViewById<Button>(R.id.salvarnovasenha)

        btnTrocarSenha.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val novaSenha = novaSenhaInput.text.toString().trim()
            val confirmarSenha = confirmarSenhaInput.text.toString().trim()

            if (email.isEmpty() || novaSenha.isEmpty() || confirmarSenha.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (novaSenha.length < 6) {
                Toast.makeText(this, "A senha deve ter no mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val regexSenha = Regex("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$")
            if (!regexSenha.matches(novaSenha)) {
                Toast.makeText(this, "A senha deve conter letras e números", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (novaSenha != confirmarSenha) {
                Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                val db = AppUsageDatabase.getInstance(this@TrocarSenhaUsuario)
                val usuarioDao = db.usuarioDao()
                val usuario = usuarioDao.buscarPorEmail(email)

                if (usuario != null) {
                    val usuarioAtualizado = usuario.copy(senha = novaSenha)
                    usuarioDao.atualizar(usuarioAtualizado)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@TrocarSenhaUsuario, "Senha atualizada com sucesso", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@TrocarSenhaUsuario, "E-mail não encontrado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
