package com.example.tasklock

import android.animation.ObjectAnimator
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
    private lateinit var minuteAnimator: ObjectAnimator
    private lateinit var hourAnimator: ObjectAnimator

    private lateinit var minuteHand: ImageView
    private lateinit var hourHand: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SplashScreen", "onCreate executado")
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)
        //desabilitar modo noturno
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        // Bloquear rotação da tela
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        minuteHand = findViewById(R.id.minuteHand)
        hourHand = findViewById(R.id.hourHand)
        animateClockHandsPrecisely()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

    }

    // Garante que o ângulo fique sempre entre 0° e 359°
    private fun normalizeAngle(angle: Float): Float {
        return ((angle % 360) + 360) % 360
    }

    // Calcula a menor distância positiva (para os ponteiros nao seguirem um sentido inverso ate a posicao que foi definida) de A até B
    private fun getPositiveRotationDistance(start: Float, end: Float): Float {
        val startNorm = normalizeAngle(start)
        val endNorm = normalizeAngle(end)
        return if (endNorm >= startNorm) {
            endNorm - startNorm
        } else {
            360 - (startNorm - endNorm)
        }
    }


    private fun animateClockHandsPrecisely() {

        // Ângulos iniciais
        val minuteStart = 120f
        val hourStart = 240f

        // Ângulos finais
        val minuteTarget = normalizeAngle(-50f) // 313°
        val hourTarget = 30f

        // Aplica ângulos iniciais
        minuteHand.rotation = minuteStart
        hourHand.rotation = hourStart

        //calculo de distancia angular entre ponteiros

        val minuteDistance = getPositiveRotationDistance(minuteStart, minuteTarget)
        val hourDistance = getPositiveRotationDistance(hourStart, hourTarget)

        val minuteDuration = 1800L  // 1.8s
        val hourDuration = 2100L    // 2.1s

        // Ponteiro dos minutos
        val minuteAnimator = ValueAnimator.ofFloat(0f, minuteDistance).apply {
            duration = minuteDuration
            interpolator = LinearInterpolator()
            addUpdateListener {
                val offset = it.animatedValue as Float
                minuteHand.rotation = (minuteStart + offset) % 360
            }
            start()
        }

        // Ponteiro das horas
        val hourAnimator = ValueAnimator.ofFloat(0f, hourDistance).apply {
            duration = hourDuration
            interpolator = LinearInterpolator()
            addUpdateListener {
                val offset = it.animatedValue as Float
                hourHand.rotation = (hourStart + offset) % 360
            }
            start()
        }
        //coisa (definição de tempo de splash screen a partir da soma do tempo de animação dos ponteiros e do 1 segundo em que eles permanecem parados)
        val maxDuration = maxOf(minuteDuration, hourDuration) + 1000L // animacao + 1s pausa
        Handler(Looper.getMainLooper()).postDelayed({
            goToMainScreen()
        }, maxDuration)
    }
        private fun goToMainScreen() {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }        //1 segundo após a animação parar (3s total)
}