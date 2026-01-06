package ar.com.orderfast.services

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import com.clover.sdk.Lockscreen

/**
 * Servicio para manejar el modo kiosco
 * Encapsula la lógica de bloqueo del sistema y Screen Pinning
 */
class KioskService(private val context: Context) {

    companion object {
        private const val TAG = "KioskService"
    }

    private var unlockCode: String? = null
    private var isActive = false
    private var lockScreen: Lockscreen? = Lockscreen(context)

    /**
     * Activa el modo kiosco
     * @param activity La actividad actual
     * @param unlockCode Código opcional para desbloquear
     * @param enableScreenPinning Si se debe activar Screen Pinning
     */
    fun enable(activity: Activity, unlockCode: String?, enableScreenPinning: Boolean = true) {
        try {
            this.unlockCode = unlockCode

            activity.runOnUiThread {
                try {
                    if (enableScreenPinning) {
                        // Intentar activar lock task mode
                        try {
                            activity.startLockTask()
                            isActive = true
                            Log.d(TAG, "Modo kiosco activado (Lock Task Mode)")
                        } catch (e: SecurityException) {
                            Log.e(TAG, "No se pudo activar Lock Task Mode. Puede requerir configuración adicional.", e)
                            // Continuar con otras medidas de bloqueo
                        }
                    }

                    // Bloquear botones del sistema
                    activity.window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
                    activity.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)

                    isActive = true
                    Log.d(TAG, "Modo kiosco activado")
                } catch (e: Exception) {
                    Log.e(TAG, "Error al activar modo kiosco", e)
                    throw e
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al activar modo kiosco", e)
            throw e
        }
    }

    /**
     * Desactiva el modo kiosco
     * @param activity La actividad actual
     * @param providedCode Código de desbloqueo (requerido si se configuró)
     * @return true si se desactivó correctamente, false si el código es incorrecto
     */
    fun disable(activity: Activity, providedCode: String?): Boolean {
        // Verificar código de desbloqueo si fue configurado
        if (unlockCode != null && unlockCode != providedCode) {
            Log.w(TAG, "Código de desbloqueo incorrecto")
            return false
        }

        activity.runOnUiThread {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // Desactivar Lock Task Mode
                    try {
                        activity.stopLockTask()
                        Log.d(TAG, "Lock Task Mode desactivado")
                    } catch (e: Exception) {
                        Log.w(TAG, "Error al desactivar Lock Task Mode", e)
                    }
                }

                // Restaurar flags de ventana
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)

                isActive = false
                unlockCode = null
                Log.d(TAG, "Modo kiosco desactivado")
            } catch (e: Exception) {
                Log.e(TAG, "Error al desactivar modo kiosco", e)
                throw e
            }
        }

        return true
    }

    /**
     * Verifica si el modo kiosco está activo
     */
    fun isActive(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.isInLockTaskMode
            } catch (e: Exception) {
                Log.w(TAG, "Error al verificar Lock Task Mode", e)
                isActive
            }
        } else {
            isActive
        }
    }

    /**
     * Verifica si se debe bloquear una tecla en modo kiosco
     * @param keyCode Código de la tecla
     * @return true si se debe bloquear, false en caso contrario
     */
    fun shouldBlockKey(keyCode: Int): Boolean {
        if (!isActive()) return false

        // Bloquear botones del sistema en modo kiosco
        return when (keyCode) {
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_MENU -> true
            else -> false
        }
    }

    /**
     * Limpia los recursos del servicio
     */
    fun dispose() {
        unlockCode = null
        isActive = false
        Log.d(TAG, "KioskService liberado")
    }
}
