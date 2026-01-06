package ar.com.orderfast.clover_sdk_example

import android.view.KeyEvent
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

class MainActivity: FlutterActivity() {
    private val TAG = "MainActivity"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Bloquear botones del sistema en modo kiosco
        when (keyCode) {
            KeyEvent.KEYCODE_HOME -> {
                Log.d(TAG, "Botón HOME bloqueado")
                return true // Bloquear
            }
            KeyEvent.KEYCODE_APP_SWITCH -> {
                Log.d(TAG, "Botón RECENT APPS bloqueado")
                return true // Bloquear
            }
            KeyEvent.KEYCODE_MENU -> {
                Log.d(TAG, "Botón MENU bloqueado")
                return true // Bloquear
            }
            KeyEvent.KEYCODE_BACK -> {
                // El botón BACK se maneja en onBackPressed()
                return super.onKeyDown(keyCode, event)
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onBackPressed() {
        // En modo kiosco, bloquear el botón back
        // Para permitir salir, necesitas desactivar el modo kiosco primero
        Log.d(TAG, "Botón BACK presionado - bloqueado en modo kiosco")
        // No llamar super.onBackPressed() para bloquear
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            // Si la app pierde el foco, intentar recuperarlo (útil en modo kiosco)
            // Esto puede ayudar a prevenir que otras apps se abran
        }
    }
}
