package com.example.tasklock

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tasklock.data.model.UserPreferences

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
            //            regex
            val regexSenha = Regex("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}\$")
            if (!regexSenha.matches(novaSenha)) {
                Toast.makeText(this, "A senha deve conter letras e números", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (novaSenha.length < 6) {
                Toast.makeText(this, "A senha deve ter no mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (novaSenha != confirmarSenha) {
                Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefs = UserPreferences(this)
            val user = prefs.getUser()

            if (user != null && user.email == email) {
                val novoUser = user.copy(senha = novaSenha)
                prefs.saveUser(novoUser)
                Toast.makeText(this, "Senha atualizada com sucesso", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "E-mail não corresponde a nenhum usuário cadastrado", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
