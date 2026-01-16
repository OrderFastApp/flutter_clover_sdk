package ar.com.orderfast.services

import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.clover.sdk.util.Screensaver

/**
 * Servicio para manejar el screensaver/standby
 * Encapsula la lógica de configuración y activación del screensaver
 */
class ScreensaverService(private val context: Context) {

    companion object {
        private const val TAG = "ScreensaverService"
    }

    private val screensaver: Screensaver = Screensaver(context)

    /**
     * Verifica si el screensaver está soportado en este dispositivo
     */
    fun isSupported(): Boolean {
        return screensaver.supported
    }

    /**
     * Habilita o deshabilita el screensaver
     * @param enabled true para habilitar, false para deshabilitar
     */
    fun setEnabled(enabled: Boolean) {
        try {
            if (!isSupported()) {
                Log.w(TAG, "Screensaver no está soportado en este dispositivo")
                return
            }

            screensaver.setScreensaverSetting(
                Screensaver.ScreensaverSetting.screensaver_enabled,
                enabled
            )
            Log.d(TAG, "Screensaver ${if (enabled) "habilitado" else "deshabilitado"}")
        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar screensaver", e)
            throw e
        }
    }

    /**
     * Configura si el screensaver se activa cuando el dispositivo duerme
     * @param activateOnSleep true para activar cuando duerme, false en caso contrario
     */
    fun setActivateOnSleep(activateOnSleep: Boolean) {
        try {
            if (!isSupported()) {
                Log.w(TAG, "Screensaver no está soportado en este dispositivo")
                return
            }

            screensaver.setScreensaverSetting(
                Screensaver.ScreensaverSetting.screensaver_activate_on_sleep,
                activateOnSleep
            )
            Log.d(TAG, "Activar en sleep: $activateOnSleep")
        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar activate on sleep", e)
            throw e
        }
    }

    /**
     * Establece el componente DreamService a usar como screensaver
     * @param componentName Nombre del componente DreamService
     */
    fun setDreamComponent(componentName: ComponentName) {
        try {
            if (!isSupported()) {
                Log.w(TAG, "Screensaver no está soportado en este dispositivo")
                return
            }

            screensaver.setDreamComponents(listOf(componentName))
            Log.d(TAG, "Dream component establecido: $componentName")
        } catch (e: Exception) {
            Log.e(TAG, "Error al establecer dream component", e)
            throw e
        }
    }

    /**
     * Obtiene los componentes DreamService activos
     */
    fun getDreamComponents(): List<ComponentName> {
        return try {
            if (!isSupported()) {
                Log.w(TAG, "Screensaver no está soportado en este dispositivo")
                return emptyList()
            }
            screensaver.getDreamComponents()
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener dream components", e)
            emptyList()
        }
    }

    /**
     * Inicia el screensaver manualmente
     */
    fun start() {
        try {
            if (!isSupported()) {
                Log.w(TAG, "Screensaver no está soportado en este dispositivo")
                return
            }

            screensaver.gotoSleep()
            Log.d(TAG, "Screensaver iniciado manualmente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar screensaver", e)
            throw e
        }
    }

    /**
     * Verifica si el screensaver está habilitado
     */
    fun isEnabled(): Boolean {
        return try {
            if (!isSupported()) {
                return false
            }
            screensaver.getScreensaverSetting(
                Screensaver.ScreensaverSetting.screensaver_enabled
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar estado del screensaver", e)
            false
        }
    }

    /**
     * Verifica si se activa cuando el dispositivo duerme
     */
    fun isActivateOnSleep(): Boolean {
        return try {
            if (!isSupported()) {
                return false
            }
            screensaver.getScreensaverSetting(
                Screensaver.ScreensaverSetting.screensaver_activate_on_sleep
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar activate on sleep", e)
            false
        }
    }
}
