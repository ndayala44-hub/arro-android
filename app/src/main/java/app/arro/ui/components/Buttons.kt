package app.arro.ui.components

import app.arro.R
import app.arro.ui.str
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.arro.ui.theme.C
import app.arro.ui.theme.T

enum class Btn { Dark, Lime, Line, LineOnDark, Text, TextOnDark, Danger, DangerText, Off }
enum class BtnSize(val height: Dp, val pad: Dp) { L(56.dp, 24.dp), M(44.dp, 18.dp), S(36.dp, 14.dp) }

private val Pill = RoundedCornerShape(999.dp)

/** The design's pill button. Large buttons fill the width; medium and small hug their label. */
@Composable
fun ArroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: Btn = Btn.Dark,
    size: BtnSize = BtnSize.L,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    fill: Boolean = size == BtnSize.L,
    padding: Dp? = null,
) {
    val s = if (!enabled && !loading) Btn.Off else style
    val (bg, fg, border) = when (s) {
        Btn.Dark -> Triple(C.Primary, C.OnPrimary, null)
        Btn.Lime -> Triple(C.Lime, C.OnLime, null)
        Btn.Line -> Triple(C.White, C.Ink, BorderStroke(1.5.dp, C.Mist))
        Btn.LineOnDark -> Triple(Color.Transparent, C.OnNight, BorderStroke(1.5.dp, C.OnNight.copy(alpha = 0.28f)))
        Btn.Text -> Triple(Color.Transparent, C.Ink, null)
        Btn.TextOnDark -> Triple(Color.Transparent, C.OnNight, null)
        Btn.Danger -> Triple(C.ErrFill, C.OnNight, null)
        Btn.DangerText -> Triple(Color.Transparent, C.Err, null)
        Btn.Off -> Triple(C.OffBg, C.OffText, null)
    }
    val h = if (s == Btn.Text || s == Btn.TextOnDark || s == Btn.DangerText) minOf(size.height, 44.dp) else size.height
    val textStyle = when (size) { BtnSize.L -> T.Button; BtnSize.M -> T.ButtonMd; BtnSize.S -> T.ButtonSm }
    Row(
        modifier
            .then(if (fill) Modifier.fillMaxWidth() else Modifier)
            .height(h)
            .clip(Pill)
            .background(bg, Pill)
            .then(if (border != null) Modifier.border(border, Pill) else Modifier)
            .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .padding(horizontal = padding ?: size.pad),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), color = fg, strokeWidth = 2.dp)
        } else {
            if (icon != null) Ic(icon, size = if (size == BtnSize.L) 18.dp else 16.dp, tint = fg)
            Txt(text, textStyle, color = fg, maxLines = 1)
        }
    }
}

enum class IconBtn { Lime, Soft, Line, OnDark, Dark, Plain }

/** 44dp round icon button (.icon-btn). */
@Composable
fun RoundIcon(
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: IconBtn = IconBtn.Lime,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    description: String? = null,
    badge: Boolean = false,
) {
    val (bg, fg) = when (style) {
        IconBtn.Lime -> C.Lime to C.OnLime
        IconBtn.Soft -> C.Soft to C.Ink
        IconBtn.Line -> C.White to C.Ink
        IconBtn.OnDark -> C.OnNight.copy(alpha = 0.12f) to C.OnNight
        IconBtn.Dark -> C.Night to C.Lime
        IconBtn.Plain -> Color.Transparent to C.Ink
    }
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(bg, CircleShape)
            .then(if (style == IconBtn.Line) Modifier.border(1.5.dp, C.Line, CircleShape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { if (description != null) contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Ic(icon, size = iconSize, tint = fg)
        if (badge) Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 11.dp)
                .size(9.dp)
                .background(C.Badge, CircleShape)
                .border(2.dp, C.White, CircleShape),
        )
    }
}

/** Back arrow in the top bar. */
@Composable
fun BackButton(onClick: () -> Unit, style: IconBtn = IconBtn.Lime) = RoundIcon(Icons.ArrowLeft, onClick, style = style, description = str(R.string.ui_buttons_back))

/** Underlined text link (.link). */
@Composable
fun LinkText(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = C.Slate, strong: Boolean = false, underline: Boolean = false) {
    val st = T.Link.copy(
        color = color,
        fontWeight = if (strong) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Medium,
        textDecoration = if (underline) androidx.compose.ui.text.style.TextDecoration.Underline else null,
    )
    Txt(text, st, modifier.clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button, onClick = onClick).padding(vertical = 4.dp))
}
