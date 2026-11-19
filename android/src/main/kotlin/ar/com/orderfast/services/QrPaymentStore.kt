package ar.com.orderfast.services

import android.content.Context
import android.util.Log
import ar.com.orderfast.models.QrPaymentRequest
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistencia del pago QR en curso y de su resultado "huérfano".
 *
 * Mientras Pagos QR está al frente, Android puede matar el proceso de la app
 * (cacheado, poca RAM). El resultado del pago llega igual — el sistema relanza
 * `MainActivity` para entregarlo — pero a un proceso nuevo, sin el callback ni el
 * estado de Dart. Para no perder un cobro ya hecho, el pedido se guarda antes de
 * lanzar y, si el resultado llega sin callback, se guarda hasta que Dart lo
 * consuma (`consumeOrphanQrResult`).
 */
class QrPaymentStore(context: Context) {

    companion object {
        private const val TAG = "QrPaymentStore"
        private const val PREFS = "orderfast_clover_qr_payment"
        private const val KEY_PENDING = "pending"
        private const val KEY_ORPHAN = "orphan"
    }

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun savePending(request: QrPaymentRequest) {
        val json = JSONObject()
            .put("externalId", request.externalId)
            .put("reference", request.reference)
            .put("amount", request.amount)
            .put("startedAt", System.currentTimeMillis())
        prefs.edit().putString(KEY_PENDING, json.toString()).apply()
    }

    fun loadPending(): JSONObject? = read(KEY_PENDING)

    fun clearPending() {
        prefs.edit().remove(KEY_PENDING).apply()
    }

    /** Guarda el resultado que llegó sin callback (proceso reiniciado). */
    fun saveOrphan(responseMap: Map<String, Any?>, pending: JSONObject?) {
        val json = JSONObject(responseMap)
        json.put("reference", pending?.opt("reference"))
        json.put("externalId", pending?.opt("externalId"))
        json.put("receivedAt", System.currentTimeMillis())
        // `commit` (no `apply`): puede que el proceso muera enseguida.
        prefs.edit().putString(KEY_ORPHAN, json.toString()).commit()
    }

    /** Devuelve el resultado huérfano (una sola vez) o null. */
    fun consumeOrphan(): Map<String, Any?>? {
        val json = read(KEY_ORPHAN) ?: return null
        prefs.edit().remove(KEY_ORPHAN).commit()
        return toMap(json)
    }

    private fun read(key: String): JSONObject? {
        val raw = prefs.getString(key, null) ?: return null
        return try {
            JSONObject(raw)
        } catch (e: Exception) {
            Log.w(TAG, "JSON inválido en $key, se descarta", e)
            prefs.edit().remove(key).commit()
            null
        }
    }

    private fun toMap(json: JSONObject): Map<String, Any?> {
        val out = mutableMapOf<String, Any?>()
        for (key in json.keys()) out[key] = unwrap(json.opt(key))
        return out
    }

    private fun unwrap(value: Any?): Any? = when (value) {
        JSONObject.NULL, null -> null
        is JSONObject -> toMap(value)
        is JSONArray -> (0 until value.length()).map { unwrap(value.opt(it)) }
        else -> value
    }
}
