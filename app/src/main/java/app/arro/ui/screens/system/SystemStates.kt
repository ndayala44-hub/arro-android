package app.arro.ui.screens.system

import app.arro.R
import app.arro.ui.str
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.arro.core.Text
import app.arro.ui.components.ArroButton
import app.arro.ui.components.Btn
import app.arro.ui.components.Cap
import app.arro.ui.components.Halo
import app.arro.ui.components.Ic
import app.arro.ui.components.Icons
import app.arro.ui.components.Illustration
import app.arro.ui.components.MaxContentWidth
import app.arro.ui.components.ScreenTitle
import app.arro.ui.components.Skeleton
import app.arro.ui.components.TitleSize
import app.arro.ui.components.Txt
import app.arro.ui.theme.C
import app.arro.ui.theme.T

/** 12.07 banner: shown across the top of the app whenever there's no connection. */
@Composable
fun BoxScope.OfflineBar(lastOnline: Long) {
    Row(
        Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .widthIn(max = MaxContentWidth)
            .fillMaxWidth()
            .background(C.Night, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Ic(Icons.WifiOff, size = 16.dp, tint = C.WarnFill)
        Txt(str(R.string.system_no_internet_connection), T.Cap, Modifier.weight(1f), color = C.OnNight)
        Txt(str(R.string.system_last_updated_x, Text.time(lastOnline)), T.Cap, color = C.DarkCap)
    }
}

/** 12.07 full state: shown on Home when offline. */
@Composable
fun OfflineState(balanceText: String, onRetry: () -> Unit, onContinue: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(C.Tint).padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().padding(top = 90.dp).height(260.dp), contentAlignment = Alignment.Center) {
            Halo(220.dp) { Illustration(app.arro.ui.components.Art.Offline, 240.dp, description = str(R.string.system_a_cloud_with_a_line_through)) }
        }
        Column(Modifier.padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ScreenTitle(str(R.string.system_you_re_offline), size = TitleSize.XL, align = TextAlign.Center)
            Txt(str(R.string.system_check_your_wi_fi_or_mobile), T.Lede, color = C.Slate, align = TextAlign.Center)
        }
        Row(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp).fillMaxWidth()
                .background(C.White, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            Cap(str(R.string.system_balance_when_last_online))
            Txt(balanceText, T.Num)
        }
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ArroButton(str(R.string.system_try_again), onRetry, icon = Icons.Refresh)
            ArroButton(str(R.string.system_show_my_last_saved_details), onContinue, style = Btn.Text)
        }
    }
}

/** 12.01 Loading skeleton that mirrors the home layout. */
@Composable
fun HomeSkeleton() {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Skeleton(44.dp, 44.dp, radius = 22.dp)
            Column(Modifier.padding(start = 10.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Skeleton(70.dp, 10.dp); Skeleton(110.dp, 14.dp)
            }
            Skeleton(44.dp, 44.dp, radius = 22.dp); Box(Modifier.size(8.dp)); Skeleton(44.dp, 44.dp, radius = 22.dp)
        }
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Skeleton(90.dp, 12.dp); Skeleton(210.dp, 40.dp, radius = 12.dp); Skeleton(150.dp, 20.dp)
        }
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 26.dp)) {
            repeat(4) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Skeleton(54.dp, 54.dp, radius = 27.dp); Skeleton(40.dp, 10.dp)
                }
            }
        }
        Row(Modifier.padding(top = 26.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) { Skeleton(160.dp, 120.dp, radius = 22.dp) }
        }
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Skeleton(140.dp, 16.dp)
            listOf(0.6f to 0.4f, 0.5f to 0.35f, 0.55f to 0.3f).forEach { (a, b) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Skeleton(44.dp, 44.dp, radius = 22.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Skeleton(null, 12.dp, Modifier.fillMaxWidth(a)); Skeleton(null, 10.dp, Modifier.fillMaxWidth(b))
                    }
                    Skeleton(70.dp, 12.dp)
                }
            }
        }
    }
}

/** Empty state used for activity and recipients (12.02, 12.03). */
@Composable
fun EmptyState(art: Int, title: String, body: String, modifier: Modifier = Modifier, actions: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.padding(top = 30.dp), contentAlignment = Alignment.Center) { Halo(200.dp) { Illustration(art, 200.dp) } }
        Txt(title, T.H2.copy(fontSize = T.Title.fontSize * 0.85f), align = TextAlign.Center)
        Txt(body, T.Lede, color = C.Slate, align = TextAlign.Center)
        Column(Modifier.padding(top = 10.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) { actions() }
    }
}
