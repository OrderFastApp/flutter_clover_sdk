package ar.com.orderfast.services

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import com.clover.sdk.Lockscreen
import com.clover.sdk.util.CustomerMode

/**
 * Servicio para manejar el modo kiosco
 * Encapsula la lógica de bloqueo del sistema y Screen Pinning
 */
class KioskService(private val context: Context) {

    companion object {
        private const val TAG = "KioskService"
    }

    /**
     * Activa el modo kiosco
     * @param activity La actividad actual
     */
    fun enable(activity: Activity) {
        try {
            CustomerMode.enable(activity)

            Log.d(TAG, "Modo kiosco activado")
        } catch (e: Exception) {
            Log.e(TAG, "Error al activar modo kiosco", e)
            throw e
        }
    }

    /**
     * Desactiva el modo kiosco
     * @param activity La actividad actual
     * @return true si se desactivó correctamente
     */
    fun disable(activity: Activity): Boolean {
        CustomerMode.disable(activity)
        return true
    }

    /**
     * Verifica si el modo kiosco está activo
     */
    fun isActive(activity: Activity): Boolean {
        return CustomerMode.getState(activity) == CustomerMode.State.ENABLED
    }

    /**
     * Limpia los recursos del servicio
     */
    fun dispose() {
        Log.d(TAG, "KioskService liberado")
    }
}
