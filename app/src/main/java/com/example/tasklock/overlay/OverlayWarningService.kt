package com.example.tasklock.overlay

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.Button
import com.example.tasklock.R

class OverlayWarningService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View

    companion object {
        // Flag para impedir múltiplas sobreposições ao mesmo tempo
        var isOverlayActive = false
    }

    override fun onCreate() {
        super.onCreate()

        // Se já estiver ativo, não cria outro
        if (isOverlayActive) {
            stopSelf()
            return
        }
        isOverlayActive = true

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_blocked_warning, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Permite foco para capturar eventos de toque, evitando inconsistência visual
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(overlayView, params)

        // Botão "Voltar" minimiza o app atual e fecha a sobreposição
        overlayView.findViewById<Button>(R.id.btnVoltar).setOnClickListener {
            // Envia o usuário para a tela inicial
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)

            // Remove a sobreposição
            try {
                windowManager.removeView(overlayView)
            } catch (_: Exception) {}
            stopSelf()
        }

        // Impede qualquer interação com a tela enquanto há sobreposição
        overlayView.setOnTouchListener { _, _ -> true }
    }

    override fun onDestroy() {
        super.onDestroy()
        isOverlayActive = false

        // Garante que a view seja removida corretamente
        try {
            if (::overlayView.isInitialized) {
                windowManager.removeView(overlayView)
            }
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}