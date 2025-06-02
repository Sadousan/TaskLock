package com.example.tasklock.data.model

import android.content.Context
class UserPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun saveUser(user: User) {
        prefs.edit().apply {
            putString("nome", user.nome)
            putString("email", user.email)
            putString("senha", user.senha)
            putBoolean("is_logged_in", true) // indica login
            apply()
        }
    }

    fun getUser(): User? {
        val nome = prefs.getString("nome", null)
        val email = prefs.getString("email", null)
        val senha = prefs.getString("senha", null)
        return if (nome != null && email != null && senha != null) {
            User(nome, email, senha)
        } else null
    }

    fun logout() {
        prefs.edit()
            .putBoolean("is_logged_in", false) // agora só desloga, sem apagar usuário
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    fun setLoggedIn(loggedIn: Boolean) {
        prefs.edit().putBoolean("is_logged_in", loggedIn).apply()
    }

}