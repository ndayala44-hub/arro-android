package app.arro.platform

import app.arro.ui.str
import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import app.arro.MainActivity
import app.arro.R
import app.arro.core.AppState
import app.arro.core.BackendEvent
import app.arro.core.RateSource
import app.arro.core.StateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.time.LocalTime

/** Keeps the backend's JSON document in internal storage, written atomically. */
class FileStore(private val file: File) : StateStore {
    override val persistent = true
    override fun read(): String? = if (file.exists()) file.readText() else null
    @Synchronized override fun write(text: String) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) { file.writeText(text); tmp.delete() }
    }
}

/** Live mid-market rates from the free ExchangeRate-API endpoint (no key needed). */
class HttpRateSource(private val url: String = "https://open.er-api.com/v6/latest/GBP") : RateSource {
    override suspend fun fetch(): Map<String, Double>? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URI(url).toURL().openConnection() as HttpURLConnection
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.setRequestProperty("Accept", "application/json")
            try {
                if (conn.responseCode != 200) return@runCatching null
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val root = Json.parseToJsonElement(text).jsonObject
                if (root["result"]?.jsonPrimitive?.contentOrNull != "success") return@runCatching null
                val rates = root["rates"]?.jsonObject ?: return@runCatching null
                app.arro.core.Currency.entries.mapNotNull { c -> rates[c.code]?.jsonPrimitive?.doubleOrNull?.let { c.code to it } }.toMap()
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }
}

/** Watches the default network so screens can show the offline state. */
class Connectivity(private val app: Application) {
    private val _online = MutableStateFlow(true)
    val online: StateFlow<Boolean> = _online.asStateFlow()
    var lastOnlineAt: Long = System.currentTimeMillis()
        private set

    fun start() {
        val cm = app.getSystemService(ConnectivityManager::class.java) ?: return
        _online.value = isOnline(cm)
        runCatching {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { _online.value = true; lastOnlineAt = System.currentTimeMillis() }
                override fun onLost(network: Network) { _online.value = isOnline(cm); if (!_online.value) lastOnlineAt = System.currentTimeMillis() }
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    _online.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }
            })
        }
    }

    fun recheck() {
        val cm = app.getSystemService(ConnectivityManager::class.java) ?: return
        _online.value = isOnline(cm)
    }

    private fun isOnline(cm: ConnectivityManager): Boolean {
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) } ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

/** Posts Arro's system notifications (transfer updates, codes, rate alerts). */
class Notifier(private val app: Application) {
    companion object {
        const val TRANSFERS = "transfers"
        const val SECURITY = "security"
        const val RATES = "rates"
        const val EXTRA_TRANSFER = "transferId"
        const val EXTRA_ROUTE = "route"
    }

    fun createChannels() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = app.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(TRANSFERS, app.getString(R.string.channel_transfers), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = app.getString(R.string.channel_transfers_desc)
        })
        nm.createNotificationChannel(NotificationChannel(SECURITY, app.getString(R.string.channel_security), NotificationManager.IMPORTANCE_HIGH).apply {
            description = app.getString(R.string.channel_security_desc)
        })
        nm.createNotificationChannel(NotificationChannel(RATES, app.getString(R.string.channel_rates), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = app.getString(R.string.channel_rates_desc)
        })
    }

    fun permitted(): Boolean = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun quiet(state: AppState): Boolean {
        val s = state.account?.settings ?: return false
        if (!s.quietHours) return false
        val now = LocalTime.now()
        val from = runCatching { LocalTime.parse(s.quietFrom) }.getOrDefault(LocalTime.of(22, 0))
        val to = runCatching { LocalTime.parse(s.quietTo) }.getOrDefault(LocalTime.of(7, 0))
        return if (from <= to) now >= from && now < to else now >= from || now < to
    }

    @SuppressLint("MissingPermission")
    fun post(e: BackendEvent, state: AppState) {
        if (!permitted()) return
        val settings = state.account?.settings
        val (channel, title, body, transferId) = when (e) {
            is BackendEvent.Code -> Quad(SECURITY, str(R.string.platform_arro), e.purpose, "")
            is BackendEvent.TransferUpdate -> {
                if (settings?.transferUpdates == false || quiet(state)) return
                Quad(TRANSFERS, e.title, e.body, e.transferId)
            }
            is BackendEvent.RateHit -> {
                if (settings?.rateAlerts == false || quiet(state)) return
                Quad(RATES, e.title, e.body, "")
            }
            is BackendEvent.KycDecided -> Quad(SECURITY, if (e.approved) str(R.string.platform_you_re_verified) else str(R.string.platform_we_need_a_clearer_photo),
                if (e.approved) str(R.string.platform_your_account_is_ready_to_send) else str(R.string.platform_retake_your_id_photo_to_finish), "")
            is BackendEvent.ChatReply -> Quad(TRANSFERS, str(R.string.platform_amina_from_arro), e.text, "")
            is BackendEvent.Info -> Quad(TRANSFERS, e.title, e.body, "")
            is BackendEvent.OfferAvailable -> {
                if (settings?.rateAlerts == false || quiet(state)) return
                Quad(RATES, e.title, e.body, "")
            }
            is BackendEvent.LockEnded -> {
                if (settings?.rateAlerts == false || quiet(state)) return
                Quad(RATES, e.title, e.body, "")
            }
        }
        val rateRoute = when (e) {
            is BackendEvent.OfferAvailable -> "lock_rate/${e.to}"
            is BackendEvent.LockEnded -> "lock_rate/${e.to}"
            else -> null
        }
        val intent = Intent(app, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (transferId.isNotEmpty()) putExtra(EXTRA_TRANSFER, transferId)
            if (e is BackendEvent.ChatReply) putExtra(EXTRA_ROUTE, "chat")
            if (rateRoute != null) putExtra(EXTRA_ROUTE, rateRoute)
        }
        val pi = PendingIntent.getActivity(app, (title + transferId).hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(app, channel)
            .setSmallIcon(R.drawable.brand_symbol)
            .setColor(ContextCompat.getColor(app, R.color.arro_ink))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(if (channel == SECURITY) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { NotificationManagerCompat.from(app).notify((title + body).hashCode(), n) }
    }

    private data class Quad(val a: String, val b: String, val c: String, val d: String)
}

/** Fingerprint and face unlock through the system prompt. */
object Biometrics {
    private const val AUTH = BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun available(context: Context): Boolean = BiometricManager.from(context).canAuthenticate(AUTH) == BiometricManager.BIOMETRIC_SUCCESS

    fun prompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negative: String = str(R.string.platform_use_passcode),
        onSuccess: () -> Unit,
        onFallback: () -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) onFallback()
                else onError(errString.toString())
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negative)
            .setAllowedAuthenticators(AUTH)
            .build()
        prompt.authenticate(info)
    }
}

/** Sharing, saving and copying. */
object Share {
    fun copy(context: Context, label: String, text: String) {
        val cm = context.getSystemService(ClipboardManager::class.java) ?: return
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        if (Build.VERSION.SDK_INT < 33) Toast.makeText(context, str(R.string.platform_copied), Toast.LENGTH_SHORT).show()
    }

    fun text(context: Context, text: String, subject: String = "") {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            if (subject.isNotEmpty()) putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** A file in the shareable cache folder. */
    fun cacheFile(context: Context, name: String): File = File(File(context.cacheDir, "shared").apply { mkdirs() }, name)

    fun file(context: Context, file: File, mime: String, subject: String = "") {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            if (subject.isNotEmpty()) putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, subject.ifEmpty { null }).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Saves to Downloads on Android 10+, otherwise opens the share sheet. Returns a message for the user. */
    fun saveToDownloads(context: Context, file: File, mime: String): String {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mime)
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                return str(R.string.platform_saved_to_downloads_as_x, file.name)
            }
        }
        file(context, file, mime, file.name)
        return str(R.string.platform_choose_where_to_save_x, file.name)
    }

    fun openUrl(context: Context, url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun email(context: Context, to: String, subject: String) {
        val i = Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:$to")).putExtra(Intent.EXTRA_SUBJECT, subject)
        runCatching { context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun dial(context: Context, number: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
