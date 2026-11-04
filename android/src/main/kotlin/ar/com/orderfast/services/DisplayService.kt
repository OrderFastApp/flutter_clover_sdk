package ar.com.orderfast.services

import android.accounts.Account
import android.content.Context
import android.util.Log
import com.clover.connector.sdk.v3.DisplayConnector
import com.clover.sdk.util.CloverAccount
import com.clover.sdk.v3.connector.IDisplayConnectorListener

/**
 * Servicio para controlar la pantalla cliente de Clover (Kiosk / Station Duo)
 * Encapsula el DisplayConnector del SDK
 *
 * En el Clover Kiosk, la app de Clover de la pantalla cliente
 * (`com.clover.remote.terminal.kiosk`) la apaga con su propio screensaver a
 * los 10 min de quedar IDLE. Ese timeout no es configurable; cada mensaje de
 * display (p. ej. showWelcomeScreen) lo reinicia.
 */
class DisplayService(private val context: Context) : IDisplayConnectorListener {

    companion object {
        private const val TAG = "DisplayService"
    }

    private var displayConnector: DisplayConnector? = null

    /**
     * Muestra la pantalla de bienvenida de Clover en la pantalla cliente.
     * El DisplayConnector se crea la primera vez (en dispositivos sin pantalla
     * cliente nunca se conecta).
     */
    fun showWelcomeScreen() {
        val connector = displayConnector ?: run {
            val cloverAccount: Account = CloverAccount.getAccount(context)
                ?: throw IllegalStateException("No se pudo obtener la cuenta de Clover")
            DisplayConnector(context, cloverAccount, this).also { displayConnector = it }
        }
        connector.showWelcomeScreen()
        Log.d(TAG, "showWelcomeScreen enviado")
    }

    override fun onDeviceConnected() {
        Log.d(TAG, "DisplayConnector conectado")
    }

    override fun onDeviceDisconnected() {
        Log.d(TAG, "DisplayConnector desconectado")
    }

    /**
     * Libera el DisplayConnector
     */
    fun dispose() {
        displayConnector?.dispose()
        displayConnector = null
        Log.d(TAG, "DisplayService liberado")
    }
}
