package app.arro

import android.app.Application
import android.content.Context
import android.os.Build
import app.arro.core.ArroBackend
import app.arro.core.BackendEvent
import app.arro.platform.Connectivity
import app.arro.platform.FileStore
import app.arro.platform.HttpRateSource
import app.arro.platform.Notifier
import app.arro.ui.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File

class ArroApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.start()
    }
}

/** Long-lived objects shared by every screen. */
class AppContainer(val app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val backend = ArroBackend(FileStore(File(app.filesDir, "arro_state.json")), HttpRateSource(), scope)
    val session = Session()
    val notifier = Notifier(app)
    val connectivity = Connectivity(app)

    /** Backend events for in-app banners (the UI collects these while visible). */
    private val _banners = MutableSharedFlow<BackendEvent>(extraBufferCapacity = 32)
    val banners: SharedFlow<BackendEvent> = _banners.asSharedFlow()

    /** True while an Arro screen is visible; system notifications are only posted when it isn't. */
    @Volatile var inForeground: Boolean = false

    fun start() {
        val maker = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        ArroBackend.DEVICE_NAME = if (Build.MODEL.startsWith(maker, ignoreCase = true)) Build.MODEL else "$maker ${Build.MODEL}"
        notifier.createChannels()
        connectivity.start()
        backend.start()
        scope.launch {
            backend.events.collect { e ->
                _banners.tryEmit(e)
                val state = backend.state.value
                // Codes always go out as a notification so they can be copied from the shade, like a real SMS.
                if (e is BackendEvent.Code || !inForeground) notifier.post(e, state)
            }
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as ArroApp).container
