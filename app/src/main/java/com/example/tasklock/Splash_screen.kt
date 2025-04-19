package com.example.tasklock

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Splash_screen : AppCompatActivity() {
    private lateinit var minuteAnimator: ObjectAnimator
    private lateinit var hourAnimator: ObjectAnimator

    private lateinit var minuteHand: ImageView
    private lateinit var hourHand: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)
        //desabilitar modo noturno
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        // Bloquear rotação da tela
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        minuteHand = findViewById(R.id.minuteHand)
        hourHand = findViewById(R.id.hourHand)
        animateClockHands()

        // carregamento do app
        Handler(Looper.getMainLooper()).postDelayed({
            stopAnimations()
            goToMainScreen()
        }, 6000) // 6 segundos de splash


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun animateClockHands() {
        // Rotação contínua do ponteiro dos minutos (mais rápido)
        minuteAnimator = ObjectAnimator.ofFloat(minuteHand, "rotation", 0f, 360f).apply {
            duration = 2000 // 2 segundos para um giro completo
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            start()
        }

        // Rotação contínua do ponteiro das horas (mais lento)
        hourAnimator = ObjectAnimator.ofFloat(hourHand, "rotation", 0f, 360f).apply {
            duration = 6000 // 6 segundos para um giro completo
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            start()
        }
    }
    private fun stopAnimations() {
        minuteAnimator.cancel()
        hourAnimator.cancel()
    }
    private fun goToMainScreen() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}