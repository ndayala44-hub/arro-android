package app.arro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.arro.core.Appearance
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

/**
 * Arro colour tokens, mirroring the design system, with a dark variant of each.
 *
 * Tokens are read at composition time, so switching [dark] recolours every screen. A few tokens never change:
 * [Lime], [LimeDeep], [DarkCap], [OnNight] and [OnLime], because they sit on brand surfaces that stay the same in both themes.
 */
object C {
    /** True while the dark theme is showing. Set by [ArroTheme]. */
    var dark by mutableStateOf(false)

    private fun pick(light: Long, dark: Long): Color = Color(if (this.dark) dark else light)

    // ---- text and surfaces (swap in dark mode)
    /** Primary text and icons. */
    val Ink: Color get() = pick(0xFF042D29, 0xFFE6F0EA)
    /** Screens, cards, sheets and fields. */
    val White: Color get() = pick(0xFFFFFFFF, 0xFF0A1412)
    val Slate: Color get() = pick(0xFF5A6366, 0xFF9AA9A3)
    val Forest: Color get() = pick(0xFF2F5748, 0xFF9CC7B0)
    val Tint: Color get() = pick(0xFFF4FBEE, 0xFF101C18)
    val Mist: Color get() = pick(0xFFD5E5DB, 0xFF2A3A34)
    val Line: Color get() = pick(0xFFE2EBE5, 0xFF22302B)
    val Field: Color get() = pick(0xFFF5F8F6, 0xFF121D1A)
    val Panel: Color get() = pick(0xFFF2F5F3, 0xFF131E1B)
    val Soft: Color get() = pick(0xFFEFF4F0, 0xFF17231F)
    val Lime2: Color get() = pick(0xFFD4FFB4, 0xFF2A4420)
    val Lime3: Color get() = pick(0xFFEEFFE0, 0xFF16261A)

    // ---- brand (fixed)
    val Lime = Color(0xFFB0FE78)
    val LimeDeep = Color(0xFF7EDB3F)
    /** Secondary text on the dark brand surfaces. */
    val DarkCap = Color(0xFFBFD3C9)
    /** Text and icons on [Night], [Camera] and coloured fills. Always white. */
    val OnNight = Color(0xFFFFFFFF)
    /** Text and icons on lime fills. Always dark green. */
    val OnLime = Color(0xFF042D29)
    /** The deep-green brand surface (success, unlock, invite, dark panels, banners). Stays dark in both themes. */
    val Night: Color get() = pick(0xFF042D29, 0xFF143A31)
    /** Fixed card art colours that don't follow the theme. */
    val Ink2Fixed = Color(0xFF0C3F38)
    val InkFixed = Color(0xFF042D29)
    val ForestFixed = Color(0xFF2F5748)
    val Ink2: Color get() = Ink2Fixed

    // ---- primary actions and selection: deep green in light mode, lime in dark mode
    val Primary: Color get() = if (dark) Lime else InkFixed
    /** Text on [Primary]. */
    val OnPrimary: Color get() = if (dark) InkFixed else OnNight
    /** Accent icons on [Primary] (check marks, the send arrow, toggle knobs). */
    val PrimaryAccent: Color get() = if (dark) InkFixed else Lime

    // ---- status
    val Ok: Color get() = pick(0xFF1A7340, 0xFF57C98A)
    val OkBg: Color get() = pick(0xFFE3F5E9, 0xFF12301F)
    val OkText: Color get() = pick(0xFF145C33, 0xFF8EE0B0)
    val Warn: Color get() = pick(0xFF9A4507, 0xFFF0A35E)
    val WarnBg: Color get() = pick(0xFFFFF0E1, 0xFF33230F)
    val WarnText: Color get() = pick(0xFF7F3905, 0xFFF7C08C)
    val WarnFill = Color(0xFFF08A35)
    val Err: Color get() = pick(0xFFB42318, 0xFFFF7A6E)
    val ErrBg: Color get() = pick(0xFFFDECEA, 0xFF3A1714)
    val ErrText: Color get() = pick(0xFF93200F, 0xFFFFB3AA)
    /** Fill for destructive buttons, with white text in both themes. */
    val ErrFill: Color get() = pick(0xFFB42318, 0xFFD6382C)
    val Info: Color get() = pick(0xFF165A94, 0xFF74B6F0)
    val InfoBg: Color get() = pick(0xFFE6F1FB, 0xFF102536)
    val InfoText: Color get() = pick(0xFF0F4A7A, 0xFFA9D3F7)

    // ---- controls and neutrals
    val OffBg: Color get() = pick(0xFFE4EAE6, 0xFF1E2925)
    val OffText: Color get() = pick(0xFF7A8386, 0xFF6E7B76)
    val Placeholder: Color get() = pick(0xFF7E878A, 0xFF7C8A84)
    val Control: Color get() = pick(0xFFB9C6BE, 0xFF4A5A54)
    val Neutral: Color get() = pick(0xFFEDF2EF, 0xFF1A2622)
    val SegBg: Color get() = pick(0xFFEDF2EE, 0xFF16221E)
    val ToggleOff: Color get() = pick(0xFFCFD9D3, 0xFF34433E)
    val ProgressBg: Color get() = pick(0xFFE6EEE8, 0xFF1E2B27)
    val StepOff: Color get() = pick(0xFFE1E9E3, 0xFF22302B)
    val DotOff: Color get() = pick(0xFFC9D8CE, 0xFF34433E)
    val TlDot: Color get() = pick(0xFFCBD7CF, 0xFF3A4A44)
    val Grab: Color get() = pick(0xFFD3DCD6, 0xFF34433E)
    val SkeletonA: Color get() = pick(0xFFECF1ED, 0xFF16211E)
    val SkeletonB: Color get() = pick(0xFFF6F9F7, 0xFF1C2824)
    val Pending: Color get() = pick(0xFF9FB2A6, 0xFF5E716A)
    val Scrim: Color get() = pick(0x8C042D29, 0xB3000000)
    val Badge = Color(0xFFE5484D)
    val Camera = Color(0xFF06110F)

    /** Avatar fills and text, indexed 1..6 (0 is the dark brand avatar). The pastels read well in both themes. */
    val avatarBg: List<Color> get() = listOf(Night, Color(0xFFDDF5C8), Color(0xFFFFE4CC), Color(0xFFD6ECFB), Color(0xFFECE2FA), Color(0xFFFBE0E4), Color(0xFFE3ECE7))
    val avatarFg: List<Color> get() = listOf(Lime, Color(0xFF1F5130), Color(0xFF7A3A0C), Color(0xFF134F86), Color(0xFF4E2C86), Color(0xFF8A1E33), Color(0xFF2F5748))

    val limeCard: Brush get() = if (dark) Brush.linearGradient(listOf(Color(0xFF26461C), Color(0xFF18321A))) else Brush.linearGradient(listOf(Color(0xFFD2FFAE), Color(0xFFAEF676)))
    val skyCard: Brush get() = if (dark) Brush.linearGradient(listOf(Color(0xFF173650), Color(0xFF0F2538))) else Brush.linearGradient(listOf(Color(0xFFDDF1FF), Color(0xFFA9DAFB)))
    val peachCard: Brush get() = if (dark) Brush.linearGradient(listOf(Color(0xFF4A2E15), Color(0xFF33200F))) else Brush.linearGradient(listOf(Color(0xFFFFE3C9), Color(0xFFFDBF87)))
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

/**
 * Applies the appearance chosen in Account (Light, Dark, or the phone's setting) and Arro's text styles.
 * Before anyone signs in, the app follows the light design.
 */
@Composable
fun ArroTheme(appearance: Appearance, content: @Composable () -> Unit) {
    val dark = when (appearance) {
        Appearance.DARK -> true
        Appearance.LIGHT -> false
        Appearance.SYSTEM -> isSystemInDarkTheme()
    }
    C.dark = dark
    MaterialTheme(
        colorScheme = if (dark) darkColorScheme(
            primary = C.Lime, onPrimary = C.OnLime, secondary = C.Lime, onSecondary = C.OnLime,
            background = C.White, onBackground = C.Ink, surface = C.White, onSurface = C.Ink, error = C.Err,
        ) else lightColorScheme(
            primary = C.Ink, onPrimary = C.White, secondary = C.Lime, onSecondary = C.Ink,
            background = C.White, onBackground = C.Ink, surface = C.White, onSurface = C.Ink, error = C.Err,
        ),
        content = content,
    )
}
