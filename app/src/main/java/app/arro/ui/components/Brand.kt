package app.arro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.arro.R
import app.arro.ui.theme.C
import app.arro.ui.theme.T

/** The Arro mark on its rounded tile. [dark] gives the ink tile with a lime mark. */
@Composable
fun BrandTile(size: Dp, dark: Boolean = false, modifier: Modifier = Modifier) {
    Box(modifier.size(size).background(if (dark) C.Night else C.Lime, RoundedCornerShape(size * (14f / 48f))), contentAlignment = Alignment.Center) {
        Icon(painterResource(R.drawable.brand_symbol), contentDescription = "Arro", modifier = Modifier.size(size), tint = if (dark) C.Lime else C.OnLime)
    }
}

@Composable
fun Wordmark(size: Float, color: Color = C.Ink, modifier: Modifier = Modifier) =
    Txt("arro", T.Wordmark.copy(fontSize = size.sp), modifier, color = color)

/** Tile plus wordmark, as in the top bars of the welcome and login screens. */
@Composable
fun BrandLockup(tile: Dp = 30.dp, text: Float = 24f, dark: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BrandTile(tile, dark)
        Wordmark(text)
    }
}
