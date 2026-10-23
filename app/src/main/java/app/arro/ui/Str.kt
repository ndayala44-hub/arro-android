package app.arro.ui

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes

/**
 * Looks up UI text in res/values/strings.xml. All of the app's screen text lives there, so it can be edited or
 * translated in one place. Usable anywhere in the UI layer: composables, click handlers, notifications and widgets.
 * [init] runs first thing in [app.arro.ArroApp.onCreate].
 */
object Str {
    @Volatile private var res: Resources? = null

    fun init(context: Context) {
        res = context.applicationContext.resources
    }

    internal fun get(@StringRes id: Int): String = checkNotNull(res) { "Str.init wasn't called" }.getString(id)

    internal fun get(@StringRes id: Int, vararg args: Any?): String = checkNotNull(res) { "Str.init wasn't called" }.getString(id, *args)
}

/** The string [id] from strings.xml. */
fun str(@StringRes id: Int): String = Str.get(id)

/** The string [id] from strings.xml, with [args] in its %1$s, %2$s... placeholders. */
fun str(@StringRes id: Int, vararg args: Any?): String = Str.get(id, *args)
