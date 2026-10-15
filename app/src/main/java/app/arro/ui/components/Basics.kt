package app.arro.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.arro.R
import app.arro.ui.theme.C
import app.arro.ui.theme.T

/** Text with a design style. */
@Composable
fun Txt(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    weight: FontWeight? = null,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    upper: Boolean = false,
) {
    Text(
        text = if (upper) text.uppercase() else text,
        modifier = modifier,
        style = style,
        color = if (color != Color.Unspecified) color else if (style.color != Color.Unspecified) style.color else C.Ink,
        fontWeight = weight,
        textAlign = align,
        maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
    )
}

@Composable
fun Txt(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    align: TextAlign? = null,
) {
    Text(text = text, modifier = modifier, style = style, color = if (color != Color.Unspecified) color else C.Ink, textAlign = align)
}

@Composable
fun Cap(text: String, modifier: Modifier = Modifier, color: Color = C.Slate, align: TextAlign? = null, size: Float? = null) =
    Txt(text, if (size != null) T.Cap.copy(fontSize = size.sp) else T.Cap, modifier, color, align = align)

@Composable
fun Over(text: String, modifier: Modifier = Modifier, color: Color = C.Slate) = Txt(text, T.Over, modifier, color, upper = true)

/** Line icon from the Arro set, tinted. Default 20dp like the design's .ic. */
@Composable
fun Ic(@DrawableRes id: Int, modifier: Modifier = Modifier, size: Dp = 20.dp, tint: Color = C.Ink, contentDescription: String? = null) {
    Icon(painter = painterResource(id), contentDescription = contentDescription, modifier = modifier.size(size), tint = tint)
}

@Composable
fun Illustration(@DrawableRes id: Int, size: Dp, modifier: Modifier = Modifier, description: String? = null) {
    Image(painter = painterResource(id), contentDescription = description, modifier = modifier.size(size), contentScale = ContentScale.Fit)
}

/** Round flag for a country or currency code (gb, eu, us, rw, ke, ug, ng, gh, in, ph). */
@Composable
fun Flag(code: String, size: Dp = 24.dp, modifier: Modifier = Modifier, ring: Dp = 0.dp, ringColor: Color = C.White) {
    val res = flagRes(code)
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .then(if (ring > 0.dp) Modifier.border(ring, ringColor, CircleShape) else Modifier)
            .background(C.Line),
    ) {
        if (res != 0) Image(painterResource(res), null, Modifier.size(size), contentScale = ContentScale.Crop)
    }
}

fun flagRes(code: String): Int = when (code.lowercase()) {
    "gb", "gbp" -> R.drawable.flag_gb
    "eu", "eur" -> R.drawable.flag_eu
    "us", "usd" -> R.drawable.flag_us
    "rw", "rwf" -> R.drawable.flag_rw
    "ke", "kes" -> R.drawable.flag_ke
    "ug", "ugx" -> R.drawable.flag_ug
    "ng", "ngn" -> R.drawable.flag_ng
    "gh", "ghs" -> R.drawable.flag_gh
    "in", "inr" -> R.drawable.flag_in
    "ph", "php" -> R.drawable.flag_ph
    else -> 0
}

@Composable
fun VSpace(h: Dp) = Spacer(Modifier.height(h))

@Composable
fun HSpace(w: Dp) = Spacer(Modifier.width(w))

/** Clickable without the ripple, for large tappable areas styled by the design. */
@Composable
fun Modifier.tap(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

/** Soft radial halo used behind illustrations. */
fun haloBrush(color: Color = C.Lime, alpha: Float = 0.55f, stop: Float = 0.7f): Brush =
    Brush.radialGradient(0f to color.copy(alpha = alpha), stop to color.copy(alpha = 0f), 1f to color.copy(alpha = 0f))

@Composable
fun Halo(size: Dp, modifier: Modifier = Modifier, alpha: Float = 0.55f, content: @Composable () -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(Modifier.size(size).background(haloBrush(alpha = alpha), CircleShape))
        content()
    }
}

/** Icon resource ids by the names used in the design files. */
object Icons {
    val ArrowLeft = R.drawable.ic_arrow_left
    val ChevronRight = R.drawable.ic_chevron_right
    val ChevronDown = R.drawable.ic_chevron_down
    val Download = R.drawable.ic_download
    val Eye = R.drawable.ic_eye
    val WalletSimple = R.drawable.ic_wallet_simple
    val Pause = R.drawable.ic_pause
    val Trash = R.drawable.ic_trash
    val Info = R.drawable.ic_info
    val Lock = R.drawable.ic_lock
    val Help = R.drawable.ic_help_circle
    val Pencil = R.drawable.ic_pencil
    val Gift = R.drawable.ic_gift
    val User = R.drawable.ic_user
    val IdCard = R.drawable.ic_id_card
    val CreditCard = R.drawable.ic_credit_card
    val Statement = R.drawable.ic_statement
    val ShieldCheck = R.drawable.ic_shield_check
    val Bell = R.drawable.ic_bell
    val Globe = R.drawable.ic_globe
    val Chat = R.drawable.ic_chat
    val Filters = R.drawable.ic_filters
    val LogOut = R.drawable.ic_log_out
    val Home = R.drawable.ic_home
    val Users = R.drawable.ic_users
    val Activity = R.drawable.ic_activity
    val ArrowUpRight = R.drawable.ic_arrow_up_right
    val ArrowUpRightBold = R.drawable.ic_arrow_up_right_bold
    val Copy = R.drawable.ic_copy
    val Share = R.drawable.ic_share
    val Smartphone = R.drawable.ic_smartphone
    val Laptop = R.drawable.ic_laptop
    val Document = R.drawable.ic_document
    val CashSimple = R.drawable.ic_cash_simple
    val Check = R.drawable.ic_check
    val CheckBold = R.drawable.ic_check_bold
    val Circle = R.drawable.ic_circle
    val FaceId = R.drawable.ic_face_id
    val Backspace = R.drawable.ic_backspace
    val Clock = R.drawable.ic_clock
    val Alert = R.drawable.ic_alert_circle
    val Plus = R.drawable.ic_plus
    val Swap = R.drawable.ic_swap_horizontal
    val Trending = R.drawable.ic_trending_up
    val Bolt = R.drawable.ic_bolt
    val ArrowDownLeft = R.drawable.ic_arrow_down_left
    val Bank = R.drawable.ic_bank
    val Cash = R.drawable.ic_cash
    val Wallet = R.drawable.ic_wallet
    val Camera = R.drawable.ic_camera
    val Refresh = R.drawable.ic_refresh
    val MapPin = R.drawable.ic_map_pin
    val Warning = R.drawable.ic_alert_triangle
    val Search = R.drawable.ic_search
    val Close = R.drawable.ic_close
    val Passport = R.drawable.ic_passport
    val IdCardPerson = R.drawable.ic_id_card_person
    val Sun = R.drawable.ic_sun
    val Square = R.drawable.ic_square
    val Scan = R.drawable.ic_scan
    val More = R.drawable.ic_more_horizontal
    val Edit = R.drawable.ic_edit
    val Star = R.drawable.ic_star
    val StarFilled = R.drawable.ic_star_filled
    val ArrowDown = R.drawable.ic_arrow_down
    val Paperclip = R.drawable.ic_paperclip
    val Send = R.drawable.ic_send
    val Image = R.drawable.ic_image
    val Percent = R.drawable.ic_percent
    val Shield = R.drawable.ic_shield
    val WifiOff = R.drawable.ic_wifi_off
    val Cross = R.drawable.ic_cross
    val SwapVertical = R.drawable.ic_swap_vertical
}

/** Illustration resource ids. */
object Art {
    val Welcome = R.drawable.ill_coins
    val Plane = R.drawable.ill_plane
    val Shield = R.drawable.ill_shield
    val IdCard = R.drawable.ill_idcard
    val IdCardWarn = R.drawable.ill_idcard_warn
    val Bank = R.drawable.ill_bank
    val Lock = R.drawable.ill_lock
    val Bell = R.drawable.ill_bell
    val Envelope = R.drawable.ill_envelope
    val Warning = R.drawable.ill_warning
    val Wallet = R.drawable.ill_wallet
    val Globe = R.drawable.ill_globe
    val Clock = R.drawable.ill_clock
    val Refund = R.drawable.ill_refund
    val Offline = R.drawable.ill_offline
    val FaceScan = R.drawable.ill_facescan
    val Gift = R.drawable.ill_gift
    val Doc = R.drawable.ill_doc
    val Phone = R.drawable.ill_phone
}
