package com.example.tasklock.data.model

import android.content.Context

class UserPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun setUsuarioLogado(email: String) {
        prefs.edit().putString("email_logado", email).putBoolean("is_logged_in", true).apply()
    }

    fun getEmailUsuarioLogado(): String? {
        return prefs.getString("email_logado", null)
    }

    fun logout() {
        prefs.edit().putBoolean("is_logged_in", false).remove("email_logado").apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }
}
