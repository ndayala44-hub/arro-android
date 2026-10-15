package app.arro.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.arro.ui.theme.C
import app.arro.ui.theme.T

/** Number pad (.keys). [onKey] receives "0".."9" or "."; [onDelete] the backspace. */
@Composable
fun Keypad(
    onKey: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
    keyHeight: Dp = 58.dp,
    dark: Boolean = false,
    bottomLeft: (@Composable () -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val fg = if (dark) C.White else C.Ink
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { k -> Key(k, keyHeight, fg) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onKey(k) } }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            when {
                decimal -> Key(".", keyHeight, fg) { onKey(".") }
                bottomLeft != null -> Box(Modifier.weight(1f).height(keyHeight), contentAlignment = Alignment.Center) { bottomLeft() }
                else -> Box(Modifier.weight(1f).height(keyHeight))
            }
            Key("0", keyHeight, fg) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onKey("0") }
            Box(
                Modifier.weight(1f).height(keyHeight).clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button) { onDelete() }
                    .semantics { contentDescription = "Delete" },
                contentAlignment = Alignment.Center,
            ) { Ic(Icons.Backspace, size = 24.dp, tint = fg) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Key(label: String, h: Dp, fg: Color, onClick: () -> Unit) {
    Box(
        Modifier.weight(1f).height(h).clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Txt(label, T.Key, color = fg) }
}

/** Six code boxes (.otp). The current box gets the focus ring. */
@Composable
fun OtpBoxes(code: String, length: Int = 6, error: Boolean = false, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(length) { i ->
            val ch = code.getOrNull(i)?.toString() ?: ""
            val current = i == code.length.coerceAtMost(length - 1) && code.length < length
            val shape = RoundedCornerShape(14.dp)
            Box(
                Modifier
                    .weight(1f)
                    .height(60.dp)
                    .then(if (current && !error) Modifier.selectionRing(14.dp, 4.dp, C.Lime.copy(alpha = 0.55f)) else Modifier)
                    .border(1.5.dp, when { error -> C.Err; current -> C.Ink; ch.isNotEmpty() -> C.Pending; else -> C.Line }, shape)
                    .background(C.White, shape),
                contentAlignment = Alignment.Center,
            ) {
                if (ch.isNotEmpty()) Txt(ch, T.Big22.copy(fontSize = 24.sp, fontWeight = FontWeight.SemiBold))
                else if (current) Box(Modifier.width(2.dp).height(26.dp).background(C.Ink))
            }
        }
    }
}

/** Passcode dots (.dots). */
@Composable
fun PasscodeDots(filled: Int, total: Int = 6, dark: Boolean = false, error: Boolean = false, modifier: Modifier = Modifier) {
    val ring = when { error -> C.Err; dark -> C.Lime; else -> C.Ink }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        repeat(total) { i ->
            Box(
                Modifier.size(16.dp).border(2.dp, ring, CircleShape)
                    .background(if (i < filled) ring else Color.Transparent, CircleShape),
            )
        }
    }
}

/** On/off switch (.tog). */
@Composable
fun Toggle(on: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val knobX by animateDpAsState(if (on) 23.dp else 3.dp, tween(160), label = "knob")
    val track by animateColorAsState(if (on) C.Ink else C.ToggleOff, tween(160), label = "track")
    Box(
        modifier
            .size(width = 50.dp, height = 30.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(track)
            .clickable(enabled = enabled, role = Role.Switch) { onChange(!on) },
    ) {
        Box(
            Modifier.offset(x = knobX, y = 3.dp).size(24.dp)
                .shadow(2.dp, CircleShape)
                .background(if (on) C.Lime else C.White, CircleShape),
        )
    }
}

/** Radio dot (.radio). */
@Composable
fun Radio(on: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier.size(22.dp)
            .border(if (on) 7.dp else 2.dp, if (on) C.Ink else C.Control, CircleShape)
            .background(C.White.copy(alpha = if (on) 1f else 0f), CircleShape),
    )
}

/** Checkbox (.check). */
@Composable
fun CheckBox(on: Boolean, onChange: ((Boolean) -> Unit)? = null, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(7.dp)
    Box(
        modifier.size(22.dp).clip(shape)
            .background(if (on) C.Ink else Color.Transparent, shape)
            .border(2.dp, if (on) C.Ink else C.Control, shape)
            .then(if (onChange != null) Modifier.clickable(role = Role.Checkbox) { onChange(!on) } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (on) Ic(Icons.Check, size = 14.dp, tint = C.Lime)
    }
}

/** Checkbox with a label; the whole row is tappable. */
@Composable
fun CheckRow(on: Boolean, onChange: (Boolean) -> Unit, text: String, modifier: Modifier = Modifier, sub: String? = null) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(role = Role.Checkbox) { onChange(!on) }.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CheckBox(on, null)
        Column(Modifier.weight(1f)) {
            Txt(text, T.Cap, color = C.Ink)
            if (sub != null) Txt(sub, T.Small, color = C.Slate)
        }
    }
}

/** Segmented control (.seg). */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().background(C.SegBg, RoundedCornerShape(14.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, o ->
            val on = i == selected
            Box(
                Modifier.weight(1f).height(40.dp)
                    .then(if (on) Modifier.shadow(1.dp, RoundedCornerShape(10.dp)) else Modifier)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) C.White else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) { Txt(o, T.Body14.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium), color = if (on) C.Ink else C.Slate, maxLines = 1) }
        }
    }
}

/** Step bars across the top of a flow (.stepper). */
@Composable
fun StepBars(filled: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i -> Box(Modifier.weight(1f).height(4.dp).background(if (i < filled) C.Ink else C.StepOff, RoundedCornerShape(2.dp))) }
    }
}

/** Progress bar (.prog). */
@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier, track: Color = C.ProgressBg, fill: Color = C.Ink, height: Dp = 8.dp) {
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)).background(track)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(height / 2)).background(fill))
    }
}

/** Pager dots (.carousel). */
@Composable
fun CarouselDots(index: Int, total: Int, modifier: Modifier = Modifier, dark: Boolean = false) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            val w by animateDpAsState(if (i == index) 24.dp else 8.dp, label = "dot")
            Box(Modifier.size(width = w, height = 8.dp).background(if (i == index) (if (dark) C.Lime else C.Ink) else (if (dark) C.White.copy(alpha = 0.3f) else C.DotOff), RoundedCornerShape(4.dp)))
        }
    }
}

enum class StepState { Done, Now, Todo, Bad, Hold }

data class TimelineItem(
    val title: String,
    val subtitle: String? = null,
    val time: String = "",
    val state: StepState = StepState.Todo,
    @DrawableRes val icon: Int? = null,
)

/** Vertical timeline (.tl). */
@Composable
fun Timeline(items: List<TimelineItem>, modifier: Modifier = Modifier, compact: Boolean = false) {
    Column(modifier.fillMaxWidth()) {
        items.forEachIndexed { i, it ->
            val last = i == items.lastIndex
            val dot = if (compact) 22.dp else 28.dp
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.width(28.dp).fillMaxHeight()) {
                    if (!last) {
                        val lineColor = if (it.state == StepState.Done) C.Ink else C.Line
                        Box(Modifier.align(Alignment.TopCenter).padding(top = dot + 2.dp, bottom = 2.dp).width(2.dp).fillMaxHeight().background(lineColor))
                    }
                    val (bg, border, fg) = when (it.state) {
                        StepState.Done -> Triple(C.Ink, C.Ink, C.Lime)
                        StepState.Now -> Triple(C.Lime, C.Ink, C.Ink)
                        StepState.Bad -> Triple(C.Err, C.Err, C.White)
                        StepState.Hold -> Triple(C.WarnFill, C.WarnFill, C.White)
                        StepState.Todo -> Triple(C.White, C.TlDot, C.Slate)
                    }
                    Box(
                        Modifier.align(Alignment.TopCenter).size(dot).background(bg, CircleShape).border(2.dp, border, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        val icon = it.icon ?: when (it.state) {
                            StepState.Done -> Icons.Check
                            StepState.Bad -> Icons.Close
                            StepState.Hold -> Icons.Pause
                            StepState.Now -> Icons.Bolt
                            StepState.Todo -> null
                        }
                        if (icon != null) Ic(icon, size = if (compact) 12.dp else 16.dp, tint = fg)
                    }
                }
                Column(Modifier.weight(1f).padding(top = if (compact) 1.dp else 3.dp, bottom = if (last) 0.dp else if (compact) 14.dp else 22.dp)) {
                    Txt(it.title, if (compact) T.H3.copy(fontSize = 14.sp) else T.H3,
                        color = if (it.state == StepState.Todo) C.OffText else C.Ink,
                        weight = if (it.state == StepState.Todo) FontWeight.Medium else null)
                    if (it.subtitle != null) Txt(it.subtitle, T.Small, Modifier.padding(top = 2.dp), color = C.Slate)
                }
                if (it.time.isNotEmpty()) Txt(it.time, T.Cap.copy(fontSize = 12.sp, fontFeatureSettings = "tnum"), Modifier.padding(top = 5.dp), color = C.Slate)
            }
        }
    }
}

/** Circular progress ring used on approval and processing screens. */
@Composable
fun SpinnerRing(size: Dp, modifier: Modifier = Modifier, stroke: Dp = 5.dp, track: Color = C.Line, color: Color = C.LimeDeep) {
    val t = rememberInfiniteTransition(label = "spin")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "angle")
    Canvas(modifier.size(size)) {
        val w = stroke.toPx()
        drawCircle(track, radius = (this.size.minDimension - w) / 2, style = Stroke(w))
        drawArc(color, angle - 90f, 70f, false, topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
            size = androidx.compose.ui.geometry.Size(this.size.width - w, this.size.height - w), style = Stroke(w, cap = StrokeCap.Round))
    }
}
