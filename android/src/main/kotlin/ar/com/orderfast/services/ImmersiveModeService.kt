package ar.com.orderfast.services

import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Servicio para manejar el modo inmersivo
 * Encapsula la lógica de ocultar barras del sistema
 */
class ImmersiveModeService {

    companion object {
        private const val TAG = "ImmersiveModeService"
    }

    private var isActive = false

    /**
     * Activa el modo inmersivo
     * @param activity La actividad actual
     * @param hideStatusBar Si se debe ocultar la barra de estado
     * @param hideNavigationBar Si se debe ocultar la barra de navegación
     */
    fun enable(activity: Activity, hideStatusBar: Boolean = true, hideNavigationBar: Boolean = true) {
        activity.runOnUiThread {
            val window = activity.window
            val decorView = window.decorView

            // Habilitar edge-to-edge
            WindowCompat.setDecorFitsSystemWindows(window, false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ (API 30+)
                val controller = window.insetsController
                if (controller != null) {
                    var typesToHide = 0

                    if (hideStatusBar) {
                        typesToHide = typesToHide or WindowInsetsCompat.Type.statusBars()
                    }

                    if (hideNavigationBar) {
                        typesToHide = typesToHide or WindowInsetsCompat.Type.navigationBars()
                    }

                    if (typesToHide != 0) {
                        controller.hide(typesToHide)
                        // Mantener el modo inmersivo sticky
                        controller.systemBarsBehavior =
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }
            } else {
                // Android 10 y anteriores
                @Suppress("DEPRECATION")
                var flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

                if (hideStatusBar) {
                    @Suppress("DEPRECATION")
                    flags = flags or View.SYSTEM_UI_FLAG_FULLSCREEN
                }

                if (hideNavigationBar) {
                    @Suppress("DEPRECATION")
                    flags = flags or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                }

                // Modo inmersivo sticky
                @Suppress("DEPRECATION")
                flags = flags or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

                @Suppress("DEPRECATION")
                decorView.systemUiVisibility = flags
            }

            isActive = true
            Log.d(TAG, "Modo inmersivo activado: statusBar=$hideStatusBar, navigationBar=$hideNavigationBar")
        }
    }

    /**
     * Desactiva el modo inmersivo
     * @param activity La actividad actual
     */
    fun disable(activity: Activity) {
        activity.runOnUiThread {
            val window = activity.window
            val decorView = window.decorView

            // Deshabilitar edge-to-edge
            WindowCompat.setDecorFitsSystemWindows(window, true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ (API 30+)
                val controller = window.insetsController
                if (controller != null) {
                    controller.show(WindowInsetsCompat.Type.statusBars())
                    controller.show(WindowInsetsCompat.Type.navigationBars())
                }
            } else {
                // Android 10 y anteriores
                @Suppress("DEPRECATION")
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }

            isActive = false
            Log.d(TAG, "Modo inmersivo desactivado")
        }
    }

    /**
     * Verifica si el modo inmersivo está activo
     */
    fun isActive(): Boolean {
        return isActive
    }

    /**
     * Limpia los recursos del servicio
     */
    fun dispose() {
        isActive = false
        Log.d(TAG, "ImmersiveModeService liberado")
    }
}
