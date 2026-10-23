package app.arro.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.arro.ui.theme.C
import app.arro.ui.theme.T

private val RowShape = RoundedCornerShape(16.dp)

/**
 * The design's .row: bordered 16dp card with a leading tile or avatar, title, subtitle and a trailing value.
 * [flat] removes the border (rows inside a [Group]); [dashed] draws the "add" style.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    flat: Boolean = false,
    dashed: Boolean = false,
    enabled: Boolean = true,
    minHeight: Dp = 62.dp,
    titleStyle: androidx.compose.ui.text.TextStyle = T.RowTitle,
    subtitleColor: Color = C.Slate,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val shape = if (flat) RoundedCornerShape(0.dp) else RowShape
    var m = modifier.fillMaxWidth().heightIn(min = minHeight)
    if (!flat) {
        m = when {
            selected -> m.selectionRing(16.dp).border(1.5.dp, C.Ink, RowShape)
            dashed -> m.drawBehind {
                drawRoundRect(C.Control, cornerRadius = CornerRadius(16.dp.toPx()), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))))
            }
            else -> m.border(1.5.dp, C.Line, RowShape)
        }
    }
    m = m.clip(shape).background(if (flat) Color.Transparent else C.White)
    if (onClick != null) m = m.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    m = m.padding(horizontal = if (flat) 14.dp else 14.dp, vertical = 10.dp).alpha(if (enabled) 1f else 0.6f)
    Row(m, verticalAlignment = verticalAlignment, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Txt(title, titleStyle, maxLines = 2)
            if (subtitle != null) Txt(subtitle, T.RowSub, Modifier.padding(top = 3.dp), color = subtitleColor)
            content?.invoke(this)
        }
        trailing?.invoke()
    }
}

/** Rows grouped in one bordered card with hairlines between them (.group). */
@Composable
fun Group(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).border(1.5.dp, C.Line, RoundedCornerShape(18.dp)).background(C.White),
        content = content,
    )
}

@Composable
fun GroupDivider() = Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))

@Composable
fun Chevron() = Ic(Icons.ChevronRight, tint = C.Slate)

/** Trailing amount with a status line underneath (.e). */
@Composable
fun TrailingValue(value: String, sub: String? = null, subColor: Color = C.Slate, valueColor: Color = C.Ink) {
    Column(horizontalAlignment = Alignment.End) {
        Txt(value, T.Num, color = valueColor)
        if (sub != null) Txt(sub, T.Cap.copy(fontSize = 12.sp), color = subColor)
    }
}

enum class TileStyle {
    Tint, Lime, Dark, Warn, Err, Info, Soft, Ok, White;

    // Getters, not constructor values, so tiles recolour when the theme changes.
    val bg: Color get() = when (this) {
        Tint -> C.Tint; Lime -> C.Lime; Dark -> C.Night; Warn -> C.WarnBg; Err -> C.ErrBg
        Info -> C.InfoBg; Soft -> C.Soft; Ok -> C.OkBg; White -> C.White
    }
    val fg: Color get() = when (this) {
        Tint, Soft, White -> C.Ink; Lime -> C.OnLime; Dark -> C.Lime; Warn -> C.Warn; Err -> C.Err; Info -> C.Info; Ok -> C.Ok
    }
}

/** 40dp rounded icon tile (.tile). */
@Composable
fun Tile(@DrawableRes icon: Int, style: TileStyle = TileStyle.Tint, size: Dp = 40.dp, round: Boolean = false, iconSize: Dp = 20.dp) {
    val shape = if (round) CircleShape else RoundedCornerShape(12.dp)
    Box(Modifier.size(size).background(style.bg, shape), contentAlignment = Alignment.Center) {
        Ic(icon, size = iconSize, tint = style.fg)
    }
}

/** Initials avatar with an optional country flag badge (.av). */
@Composable
fun Avatar(initials: String, color: Int = 1, size: Dp = 44.dp, flag: String? = null, modifier: Modifier = Modifier) {
    val idx = color.coerceIn(0, 6)
    val fontSize = when { size >= 84.dp -> 28.sp; size >= 64.dp -> 22.sp; size <= 36.dp -> 13.sp; else -> 15.sp }
    val flagSize = when { size >= 84.dp -> 26.dp; size >= 64.dp -> 22.dp; else -> 18.dp }
    Box(modifier.size(size)) {
        Box(Modifier.size(size).background(C.avatarBg[idx], CircleShape), contentAlignment = Alignment.Center) {
            Txt(initials, T.H3.copy(fontSize = fontSize), color = C.avatarFg[idx])
        }
        if (flag != null) Flag(flag, flagSize, Modifier.align(Alignment.BottomEnd).offset(3.dp, 3.dp), ring = if (size >= 84.dp) 3.dp else 2.dp)
    }
}

enum class PillKind {
    Ok, Warn, Err, Info, Neutral, Lime, OnDark;

    val bg: Color get() = when (this) {
        Ok -> C.OkBg; Warn -> C.WarnBg; Err -> C.ErrBg; Info -> C.InfoBg; Neutral -> C.Neutral; Lime -> C.Lime; OnDark -> Color(0x1FFFFFFF)
    }
    val fg: Color get() = when (this) {
        Ok -> C.Ok; Warn -> C.Warn; Err -> C.Err; Info -> C.Info; Neutral -> C.Forest; Lime -> C.OnLime; OnDark -> C.OnNight
    }
}

/** Status pill with optional dot (.pill). */
@Composable
@Suppress("ModifierParameter") // screens pass the earlier arguments by position
fun Pill(text: String, kind: PillKind = PillKind.Neutral, dot: Boolean = false, @DrawableRes icon: Int? = null, modifier: Modifier = Modifier) {
    Row(
        modifier.height(24.dp).background(kind.bg, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (dot) Box(Modifier.size(6.dp).background(kind.fg, CircleShape))
        if (icon != null) Ic(icon, size = 14.dp, tint = kind.fg)
        Txt(text, T.Pill, color = kind.fg, maxLines = 1)
    }
}

/** Filter chip (.chip). */
@Composable
@Suppress("ModifierParameter") // screens pass the earlier arguments by position
fun Chip(text: String, selected: Boolean = false, onClick: (() -> Unit)? = null, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null, flag: String? = null) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier
            .height(36.dp)
            .clip(shape)
            .background(if (selected) C.Primary else C.White, shape)
            .border(1.5.dp, if (selected) C.Primary else C.Line, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = if (flag != null) 6.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (flag != null) Flag(flag, 24.dp)
        if (icon != null) Ic(icon, size = 16.dp, tint = if (selected) C.OnPrimary else C.Ink)
        Txt(text, T.Chip, color = if (selected) C.OnPrimary else C.Ink, maxLines = 1)
        if (flag != null) HSpace(4.dp)
    }
}

/** White bordered card (.card). */
@Composable
fun ArroCard(modifier: Modifier = Modifier, padding: Dp = 16.dp, border: Color = C.Line, background: Color = C.White, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().border(1.5.dp, border, RoundedCornerShape(20.dp)).background(background, RoundedCornerShape(20.dp)).padding(padding),
        content = content,
    )
}

enum class PanelKind {
    Grey, Lime, Dark, White;

    val bg: Color get() = when (this) { Grey -> C.Panel; Lime -> C.Lime3; Dark -> C.Night; White -> C.White }
}

/** Filled panel (.panel): grey, lime or dark. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    kind: PanelKind = PanelKind.Grey,
    padding: Dp = 16.dp,
    gap: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(kind.bg, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content,
    )
}

/** Key/value line (.kv). */
@Composable
fun KV(key: String, value: String, strong: Boolean = false, valueColor: Color = C.Ink, keyColor: Color = C.Slate, onDark: Boolean = false, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Txt(key, if (strong) T.Body14.copy(fontWeight = FontWeight.SemiBold) else T.Body14, color = if (strong) (if (onDark) C.OnNight else C.Ink) else if (onDark) C.DarkCap else keyColor)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
            Txt(value, if (strong) T.Body14.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum") else T.Body14.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
                color = if (onDark && valueColor == C.Ink) C.OnNight else valueColor, align = TextAlign.End)
            trailing?.invoke()
        }
    }
}

@Composable
fun HDivider(modifier: Modifier = Modifier, color: Color = C.Line) = Box(modifier.fillMaxWidth().height(1.dp).background(color))

enum class NoteKind {
    Info, Warn, Ok, Err, Lime;

    val bg: Color get() = when (this) { Info -> C.InfoBg; Warn -> C.WarnBg; Ok -> C.OkBg; Err -> C.ErrBg; Lime -> C.Lime3 }
    val fg: Color get() = when (this) { Info -> C.InfoText; Warn -> C.WarnText; Ok -> C.OkText; Err -> C.ErrText; Lime -> C.Ink }
}

/** Inline notice with icon (.note). */
@Composable
@Suppress("ModifierParameter") // screens pass the earlier arguments by position
fun Note(text: String, kind: NoteKind = NoteKind.Info, @DrawableRes icon: Int = Icons.Info, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().background(kind.bg, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Ic(icon, Modifier.padding(top = 1.dp), tint = kind.fg)
        Column(Modifier.weight(1f)) {
            Txt(text, T.Cap, color = kind.fg)
            if (action != null && onAction != null) LinkText(action, onAction, color = kind.fg, strong = true, underline = true)
        }
    }
}

/** Grid of small items with the design's 5-column layout (Send again, Recent recipients). */
@Composable
fun FiveGrid(items: List<@Composable () -> Unit>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.take(5).forEach { item -> Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) { item() } }
        repeat(5 - items.size.coerceAtMost(5)) { Box(Modifier.weight(1f)) }
    }
}

/** Avatar with a first-name caption, for the 5-column people grids. */
@Composable
fun PersonChip(name: String, initials: String, color: Int, flag: String?, onClick: () -> Unit, add: Boolean = false) {
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (add) Box(Modifier.size(44.dp).background(C.Lime, CircleShape), contentAlignment = Alignment.Center) { Ic(Icons.Plus, tint = C.OnLime) }
        else Avatar(initials, color, flag = flag)
        Txt(name, T.Cap, color = C.Ink, maxLines = 1, align = TextAlign.Center)
    }
}

/** Two overlapping flags for a currency pair. */
@Composable
fun FlagPair(from: String, to: String) {
    Box(Modifier.width(40.dp).height(28.dp)) {
        Flag(from, 24.dp, Modifier.offset(y = 2.dp))
        Flag(to, 26.dp, Modifier.offset(x = 14.dp, y = 1.dp), ring = 2.dp)
    }
}

/** Gradient fill helper for the balance cards. */
fun cardBrush(cur: String): Brush = when (cur) { "EUR" -> C.skyCard; "USD" -> C.peachCard; else -> C.limeCard }
fun cardInk(cur: String): Color = when (cur) {
    "EUR" -> if (C.dark) Color(0xFFA9D3F7) else Color(0xFF134F86)
    "USD" -> if (C.dark) Color(0xFFF7C08C) else Color(0xFF7A3A0C)
    else -> C.Forest
}

@Composable
fun RowScope.Spring() = Box(Modifier.weight(1f))

/** The lime 3dp focus/selection ring drawn outside the element, like the design's box-shadow ring. */
fun Modifier.selectionRing(radius: Dp, width: Dp = 3.dp, color: Color = C.Lime.copy(alpha = 0.7f)): Modifier = drawBehind {
    val w = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(-w, -w),
        size = androidx.compose.ui.geometry.Size(size.width + 2 * w, size.height + 2 * w),
        cornerRadius = CornerRadius(radius.toPx() + w),
    )
}
