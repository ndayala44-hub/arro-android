package app.arro.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.arro.ui.LocalNav
import app.arro.ui.Routes
import app.arro.ui.container
import app.arro.ui.go
import app.arro.ui.tab
import app.arro.ui.theme.C

/**
 * Screens that carry the floating tab bar. Content scrolls underneath the bar; [top] is pinned above the scroll.
 * The Send button opens the send hub.
 */
@Composable
fun TabScreen(
    selected: Tab,
    modifier: Modifier = Modifier,
    background: Color = C.White,
    scrollState: ScrollState? = null,
    gap: Dp = 0.dp,
    scroll: Boolean = true,
    top: (@Composable ColumnScope.() -> Unit)? = null,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val nav = LocalNav.current
    val c = container()
    Box(modifier.fillMaxSize().background(background)) {
        Column(Modifier.align(Alignment.TopCenter).widthIn(max = MaxContentWidth).fillMaxSize()) {
            top?.invoke(this)
            val ss = scrollState ?: rememberScrollState()
            Column(
                Modifier.weight(1f).fillMaxWidth().then(if (scroll) Modifier.verticalScroll(ss) else Modifier),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                content()
                Spacer(Modifier.navigationBarsPadding().height(112.dp))
            }
        }
        TabBar(
            selected = selected,
            onSelect = { t ->
                when (t) {
                    Tab.Home -> nav.tab(Routes.HOME)
                    Tab.Recipients -> nav.tab(Routes.RECIPIENTS)
                    Tab.Activity -> nav.tab(Routes.ACTIVITY)
                    Tab.Account -> nav.tab(Routes.ACCOUNT)
                }
            },
            onSend = { c.session.send.reset(c.session.send.country); nav.go(Routes.SEND) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        overlay?.invoke(this)
    }
}
