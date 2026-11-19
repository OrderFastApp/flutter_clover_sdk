package ar.com.orderfast.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import android.util.Log

/**
 * Foreground service que mantiene viva la app mientras Pagos QR está al frente.
 *
 * Sin esto, el proceso de la app queda cacheado (`adj 900`) y Android lo mata en
 * el C305 (1,8 GB de RAM), con lo que se pierde el estado del cobro en curso.
 */
class QrPaymentForegroundService : Service() {

    companion object {
        private const val TAG = "QrPaymentFgService"
        private const val CHANNEL_ID = "orderfast_qr_payment"
        private const val NOTIFICATION_ID = 0x5152

        /** Red de seguridad: si el resultado nunca llega, el servicio no queda eterno. */
        private const val MAX_LIFETIME_MS = 10 * 60 * 1000L

        fun start(context: Context) {
            try {
                val intent = Intent(context, QrPaymentForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // No crítico: el cobro sigue, solo sin la protección.
                Log.w(TAG, "No se pudo iniciar el foreground service", e)
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, QrPaymentForegroundService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo detener el foreground service", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Pago con QR",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
            .setContentTitle("Pago con QR en curso")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ stopSelf() }, MAX_LIFETIME_MS)
        return START_NOT_STICKY
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
