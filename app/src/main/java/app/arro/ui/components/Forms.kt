package app.arro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.arro.core.Validation
import app.arro.ui.theme.C
import app.arro.ui.theme.T

/** Password input with the show/hide eye. */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    ok: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    onDone: (() -> Unit)? = null,
    labelTrailing: (@Composable () -> Unit)? = null,
    focusRequester: FocusRequester? = null,
) {
    var show by remember { mutableStateOf(false) }
    ArroField(
        value, onValueChange, modifier, label = label, error = error, ok = ok,
        keyboard = KeyboardType.Password, imeAction = imeAction, password = !show, onDone = onDone,
        textStyle = if (show) T.Input else T.Input.copy(fontSize = 18.sp, letterSpacing = 2.sp),
        labelTrailing = labelTrailing, focusRequester = focusRequester,
        trailing = { RoundIcon(Icons.Eye, { show = !show }, style = IconBtn.Plain, size = 36.dp, description = if (show) "Hide password" else "Show password") },
    )
}

/** Strength bar and label under a new password (Weak, Good, Strong). */
@Composable
fun PasswordStrength(password: String, showRules: Boolean = true) {
    val r = Validation.password(password)
    val color = when { r.score >= 4 -> C.Ok; r.score == 3 -> C.Ok; r.score == 2 -> C.Warn; else -> C.Err }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (password.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ProgressBar(r.score / 4f, Modifier.weight(1f), fill = if (r.score >= 3) C.LimeDeep else if (r.score == 2) C.WarnFill else C.Err, height = 6.dp)
            Txt(r.label, T.Cap.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = color)
        }
        if (showRules) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Rule(r.length, "At least 8 characters")
            Rule(r.cases, "An uppercase and a lowercase letter")
            Rule(r.number, "At least one number")
            Rule(r.symbol, "A symbol, like ! or # (recommended)")
        }
    }
}

@Composable
private fun Rule(met: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Ic(if (met) Icons.Check else Icons.Circle, size = 16.dp, tint = if (met) C.Ok else C.Slate)
        Txt(text, T.Small, color = if (met) C.Ok else C.Slate)
    }
}

/** Six-digit code entry: a hidden text field drawn as the design's code boxes. */
@Composable
fun CodeInput(
    code: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: Boolean = false,
    autoFocus: Boolean = true,
    onComplete: (String) -> Unit = {},
) {
    val focus = remember { FocusRequester() }
    BasicTextField(
        value = code,
        onValueChange = { v ->
            val digits = v.filter { it.isDigit() }.take(6)
            onChange(digits)
            if (digits.length == 6 && digits != code) onComplete(digits)
        },
        modifier = modifier.fillMaxWidth().focusRequester(focus),
        textStyle = T.Input.copy(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        singleLine = true,
        decorationBox = { inner ->
            androidx.compose.foundation.layout.Box {
                OtpBoxes(code, error = error)
                androidx.compose.foundation.layout.Box(Modifier.matchParentSizeHack()) { inner() }
            }
        },
    )
    LaunchedEffect(Unit) { if (autoFocus) runCatching { focus.requestFocus() } }
}

/** Keeps the real text field tiny and invisible behind the code boxes. */
private fun Modifier.matchParentSizeHack(): Modifier = this.then(Modifier.fillMaxWidth())
