package app.arro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.arro.R

/** Arro colour tokens, mirroring the design system. */
object C {
    val Ink = Color(0xFF042D29)
    val Ink2 = Color(0xFF0C3F38)
    val Forest = Color(0xFF2F5748)
    val Lime = Color(0xFFB0FE78)
    val Lime2 = Color(0xFFD4FFB4)
    val Lime3 = Color(0xFFEEFFE0)
    val LimeDeep = Color(0xFF7EDB3F)
    val Tint = Color(0xFFF4FBEE)
    val Mist = Color(0xFFD5E5DB)
    val Line = Color(0xFFE2EBE5)
    val Field = Color(0xFFF5F8F6)
    val Slate = Color(0xFF5A6366)
    val White = Color(0xFFFFFFFF)
    val Ok = Color(0xFF1A7340)
    val OkBg = Color(0xFFE3F5E9)
    val OkText = Color(0xFF145C33)
    val Warn = Color(0xFF9A4507)
    val WarnBg = Color(0xFFFFF0E1)
    val WarnText = Color(0xFF7F3905)
    val WarnFill = Color(0xFFF08A35)
    val Err = Color(0xFFB42318)
    val ErrBg = Color(0xFFFDECEA)
    val ErrText = Color(0xFF93200F)
    val Info = Color(0xFF165A94)
    val InfoBg = Color(0xFFE6F1FB)
    val InfoText = Color(0xFF0F4A7A)
    val DarkCap = Color(0xFFBFD3C9)
    val Panel = Color(0xFFF2F5F3)
    val Soft = Color(0xFFEFF4F0)
    val OffBg = Color(0xFFE4EAE6)
    val OffText = Color(0xFF7A8386)
    val Placeholder = Color(0xFF7E878A)
    val Control = Color(0xFFB9C6BE)
    val Neutral = Color(0xFFEDF2EF)
    val SegBg = Color(0xFFEDF2EE)
    val ToggleOff = Color(0xFFCFD9D3)
    val ProgressBg = Color(0xFFE6EEE8)
    val StepOff = Color(0xFFE1E9E3)
    val DotOff = Color(0xFFC9D8CE)
    val TlDot = Color(0xFFCBD7CF)
    val Badge = Color(0xFFE5484D)
    val Grab = Color(0xFFD3DCD6)
    val Camera = Color(0xFF06110F)
    val Scrim = Color(0x8C042D29)
    val SkeletonA = Color(0xFFECF1ED)
    val SkeletonB = Color(0xFFF6F9F7)
    val Pending = Color(0xFF9FB2A6)

    /** Avatar fills and text, indexed 1..6 (0 is the dark brand avatar). */
    val avatarBg = listOf(Ink, Color(0xFFDDF5C8), Color(0xFFFFE4CC), Color(0xFFD6ECFB), Color(0xFFECE2FA), Color(0xFFFBE0E4), Color(0xFFE3ECE7))
    val avatarFg = listOf(Lime, Color(0xFF1F5130), Color(0xFF7A3A0C), Color(0xFF134F86), Color(0xFF4E2C86), Color(0xFF8A1E33), Color(0xFF2F5748))

    val limeCard = Brush.linearGradient(listOf(Color(0xFFD2FFAE), Color(0xFFAEF676)))
    val skyCard = Brush.linearGradient(listOf(Color(0xFFDDF1FF), Color(0xFFA9DAFB)))
    val peachCard = Brush.linearGradient(listOf(Color(0xFFFFE3C9), Color(0xFFFDBF87)))
}

val Outfit = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
)

private val centered = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None)

private fun style(size: Float, weight: FontWeight = FontWeight.Normal, lineHeight: Float? = null, spacing: TextUnit = 0.sp, color: Color = Color.Unspecified, tabular: Boolean = false) =
    TextStyle(
        fontFamily = Outfit,
        fontSize = size.sp,
        fontWeight = weight,
        lineHeight = if (lineHeight != null) (size * lineHeight).sp else TextUnit.Unspecified,
        letterSpacing = spacing,
        color = color,
        lineHeightStyle = centered,
        fontFeatureSettings = if (tabular) "tnum" else null,
    )

/** Text styles from the design system. */
object T {
    val TitleXL = style(32f, FontWeight.Bold, 1.08f, 0.005.em)
    val TitleL = style(28f, FontWeight.Bold, 1.12f, 0.005.em)
    val Title = style(26f, FontWeight.Bold, 1.12f, 0.005.em)
    val Lede = style(15f, lineHeight = 1.5f)
    val H2 = style(17f, FontWeight.SemiBold, 1.3f)
    val H3 = style(15f, FontWeight.SemiBold, 1.3f)
    val Cap = style(13f, lineHeight = 1.45f)
    val Small = style(12.5f, lineHeight = 1.4f)
    val Over = style(11f, FontWeight.SemiBold, 1.3f, 0.12.em)
    val Amount = style(44f, FontWeight.Bold, 1.0f, (-0.02).em, tabular = true)
    val AmountMd = style(34f, FontWeight.Bold, 1.0f, (-0.02).em, tabular = true)
    val AmountSmall = style(22f, FontWeight.SemiBold, 1.0f, tabular = true)
    val Body = style(15f, lineHeight = 1.4f)
    val Body14 = style(14f, lineHeight = 1.35f)
    val RowTitle = style(15f, FontWeight.Medium, 1.25f)
    val RowSub = style(12.5f, lineHeight = 1.3f)
    val Label = style(13f, FontWeight.Medium, 1.3f)
    val Input = style(16f, lineHeight = 1.25f)
    val Button = style(16f, FontWeight.SemiBold, 1.2f)
    val ButtonMd = style(14f, FontWeight.SemiBold, 1.2f)
    val ButtonSm = style(13f, FontWeight.SemiBold, 1.2f)
    val Pill = style(12f, FontWeight.SemiBold, 1.2f)
    val Chip = style(13f, FontWeight.Medium, 1.2f)
    val Link = style(13f, FontWeight.Medium, 1.3f)
    val Tab = style(11f, FontWeight.Medium, 1.25f)
    val Num = style(15f, FontWeight.SemiBold, 1.25f, tabular = true)
    val Big36 = style(36f, FontWeight.Bold, 1.1f, (-0.02).em, tabular = true)
    val Big28 = style(28f, FontWeight.Bold, 1.15f, tabular = true)
    val Big22 = style(22f, FontWeight.Bold, 1.2f, tabular = true)
    val Key = style(26f, FontWeight.Medium, 1.0f)
    val Wordmark = style(28f, FontWeight.Bold, 1.0f, (-0.03).em)
}

@Composable
fun ArroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.Ink, onPrimary = C.White, secondary = C.Lime, onSecondary = C.Ink,
            background = C.White, onBackground = C.Ink, surface = C.White, onSurface = C.Ink, error = C.Err,
        ),
        content = content,
    )
}
