package com.example.tasklock

import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Splash_screen : AppCompatActivity() {
    private lateinit var minuteAnimator: ValueAnimator
    private lateinit var hourAnimator: ValueAnimator
    private lateinit var minuteHand: ImageView
    private lateinit var hourHand: ImageView
    private var isAnimationComplete = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SplashScreen", "onCreate executado")
        
        // Configure theme and orientation before setContentView
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)

        // Initialize views
        minuteHand = findViewById(R.id.minuteHand)
        hourHand = findViewById(R.id.hourHand)

        // Setup window insets once
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Start animation
        animateClockHandsPrecisely()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up animators
        if (::minuteAnimator.isInitialized) {
            minuteAnimator.cancel()
        }
        if (::hourAnimator.isInitialized) {
            hourAnimator.cancel()
        }
    }

    private fun animateClockHandsPrecisely() {
        if (isAnimationComplete) return

        // Ponteiro dos minutos
        val minuteStart = -50f
        val minuteEnd = 310f
        val minuteDistance = minuteEnd - minuteStart
        val minuteDuration = 2000L // 2 segundos

        minuteAnimator = ValueAnimator.ofFloat(0f, minuteDistance).apply {
            duration = minuteDuration
            interpolator = LinearInterpolator()
            addUpdateListener {
                val offset = it.animatedValue as Float
                minuteHand.rotation = normalizeAngle(minuteStart + offset)
            }
            start()
        }

        // Ponteiro das horas
        val hourStart = 30f
        val hourEnd = 390f
        val hourDistance = hourEnd - hourStart
        val hourDuration = 2000L // 2 segundos

        hourAnimator = ValueAnimator.ofFloat(0f, hourDistance).apply {
            duration = hourDuration
            interpolator = LinearInterpolator()
            addUpdateListener {
                val offset = it.animatedValue as Float
                hourHand.rotation = normalizeAngle(hourStart + offset)
            }
            start()
        }

        // Schedule navigation after animation
        val maxDuration = maxOf(minuteDuration, hourDuration) + 1000L
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing) {
                isAnimationComplete = true
                goToMainScreen()
            }
        }, maxDuration)
    }

    private fun normalizeAngle(angle: Float): Float {
        return ((angle % 360) + 360) % 360
    }

    private fun goToMainScreen() {
        if (!isFinishing) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}