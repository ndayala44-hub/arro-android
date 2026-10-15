package app.arro.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.arro.core.Currency
import app.arro.core.Money
import app.arro.ui.theme.C
import app.arro.ui.theme.T

/** "£2,955" large with ".62" smaller and grey, like the design's .amt. */
@Composable
fun BigAmount(minor: Long, cur: Currency, modifier: Modifier = Modifier, size: TextUnit = 44.sp, color: Color = C.Ink, decimalsColor: Color = C.Slate) {
    val (main, dec) = Money.split(minor, cur)
    val small = if (size.value >= 40f) 22.sp else (size.value * 0.53f).sp
    Txt(
        buildAnnotatedString {
            append(main)
            withStyle(SpanStyle(fontSize = small, fontWeight = FontWeight.SemiBold, color = decimalsColor, letterSpacing = 0.sp)) { append(dec) }
        },
        T.Amount.copy(fontSize = size),
        modifier,
        color = color,
    )
}

/** Balance card with a soft gradient (.bcard). */
@Composable
fun BalanceCard(
    cur: Currency,
    minor: Long,
    modifier: Modifier = Modifier,
    detail: String? = null,
    width: Dp = 160.dp,
    height: Dp = 120.dp,
    onClick: () -> Unit,
) {
    val ink = cardInk(cur.code)
    Column(
        modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(22.dp))
            .background(cardBrush(cur.code))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Txt(cur.displayName, T.Cap.copy(fontSize = 12.sp), color = ink)
            Txt(Money.format(minor, cur), T.Big22, Modifier.padding(top = 4.dp), maxLines = 1)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Flag(cur.flag, 18.dp)
            Txt(cur.code, T.Cap.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = C.Ink)
            if (detail != null) Txt(detail, T.Cap.copy(fontSize = 12.sp), Modifier.weight(1f), color = ink, align = androidx.compose.ui.text.style.TextAlign.End, maxLines = 1)
        }
    }
}

/** One of the four round quick actions under the balance (.qa). */
@Composable
fun QuickAction(label: String, @DrawableRes icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier, lime: Boolean = false) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).clickable(role = Role.Button, onClick = onClick).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(54.dp).background(if (lime) C.Lime else C.Ink, CircleShape), contentAlignment = Alignment.Center) {
            Ic(icon, tint = if (lime) C.Ink else C.White)
        }
        Txt(label, T.Cap.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Medium), color = C.Ink, maxLines = 1)
    }
}

@Composable
fun QuickActions(items: List<Triple<String, Int, () -> Unit>>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { i, (label, icon, action) ->
            QuickAction(label, icon, action, Modifier.weight(1f), lime = i == items.lastIndex && label == "Rates")
        }
    }
}

enum class Tab(val label: String, @DrawableRes val icon: Int) {
    Home("Home", Icons.Home), Recipients("Recipients", Icons.Users), Activity("Activity", Icons.Activity), Account("Account", Icons.User)
}

/** Floating tab bar: a pill with four destinations and a separate Send button. */
@Composable
fun TabBar(selected: Tab, onSelect: (Tab) -> Unit, onSend: () -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val narrow = maxWidth < 350.dp
        val pillH = if (narrow) 58.dp else 64.dp
        val sendSize = if (narrow) 56.dp else 64.dp
        Box(Modifier.fillMaxWidth()) {
            // fade so content scrolling underneath stays readable
            Box(
                Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, C.White.copy(alpha = 0.92f), C.White))),
            )
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = MaxContentWidth)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val pill = RoundedCornerShape(999.dp)
                Row(
                    Modifier
                        .weight(1f)
                        .height(pillH)
                        .shadow(14.dp, pill, ambientColor = C.Ink.copy(alpha = 0.10f), spotColor = C.Ink.copy(alpha = 0.10f))
                        .background(C.Field, pill)
                        .border(1.dp, C.Line, pill)
                        .padding(horizontal = if (narrow) 2.dp else 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Tab.entries.forEach { t ->
                        val on = t == selected
                        Column(
                            Modifier.weight(1f).fillMaxHeight().clip(pill).clickable(role = Role.Tab) { onSelect(t) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                        ) {
                            Box(
                                Modifier.size(width = if (narrow) 42.dp else 50.dp, height = if (narrow) 28.dp else 30.dp)
                                    .background(if (on) C.Lime2 else Color.Transparent, RoundedCornerShape(15.dp)),
                                contentAlignment = Alignment.Center,
                            ) { Ic(t.icon, tint = if (on) C.Ink else C.Slate) }
                            Txt(t.label, T.Tab.copy(fontSize = if (narrow) 10.sp else 11.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                                color = if (on) C.Ink else C.Slate, maxLines = 1)
                        }
                    }
                }
                Box(
                    Modifier
                        .size(sendSize)
                        .shadow(12.dp, CircleShape, ambientColor = C.Ink.copy(alpha = 0.22f), spotColor = C.Ink.copy(alpha = 0.22f))
                        .background(C.White, CircleShape)
                        .border(1.dp, C.Line, CircleShape)
                        .padding(if (narrow) 4.dp else 5.dp)
                        .clip(CircleShape)
                        .background(C.Ink, CircleShape)
                        .clickable(role = Role.Button, onClick = onSend)
                        .semantics { contentDescription = "Send money" },
                    contentAlignment = Alignment.Center,
                ) { Ic(Icons.ArrowUpRightBold, size = 26.dp, tint = C.Lime) }
            }
        }
    }
}
