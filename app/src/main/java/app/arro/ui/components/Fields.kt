package app.arro.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.arro.ui.theme.C
import app.arro.ui.theme.T

private val FieldShape = RoundedCornerShape(14.dp)

/** Field label (.lbl). */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier, color: Color = C.Slate) = Txt(text, T.Label, modifier, color)

/** Helper, error or success line under a field (.hint). */
@Composable
fun FieldHint(error: String? = null, ok: String? = null, hint: String? = null) {
    when {
        error != null -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Ic(Icons.Alert, Modifier.padding(top = 1.dp), size = 16.dp, tint = C.Err)
            Txt(error, T.Small, color = C.Err)
        }
        ok != null -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Ic(Icons.Check, size = 16.dp, tint = C.Ok)
            Txt(ok, T.Small, color = C.Ok)
        }
        hint != null -> Txt(hint, T.Small, color = C.Slate)
    }
}

/** Border and ring for the field states: focus (ink + lime ring), error (red + soft red ring), ok (green). */
fun Modifier.fieldFrame(focused: Boolean, error: Boolean, ok: Boolean, readOnly: Boolean, radius: Dp = 14.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    val ring = when {
        error -> this.selectionRing(radius, 4.dp, C.Err.copy(alpha = 0.08f))
        focused -> this.selectionRing(radius, 4.dp, C.Lime.copy(alpha = 0.5f))
        else -> this
    }
    val border = when { error -> C.Err; focused -> C.Ink; ok -> C.Ok; else -> C.Line }
    return ring.border(1.5.dp, border, shape).clip(shape).background(if (readOnly) C.Field else C.White, shape)
}

/** Text input styled like the design's .input, with label and hint. */
@Composable
fun ArroField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    error: String? = null,
    ok: String? = null,
    hint: String? = null,
    readOnly: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    password: Boolean = false,
    singleLine: Boolean = true,
    height: Dp = 54.dp,
    textStyle: TextStyle = T.Input,
    maxLength: Int = 200,
    focusRequester: FocusRequester? = null,
    onDone: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    prefix: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    labelTrailing: (@Composable () -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FieldLabel(label, Modifier.weight(1f))
            labelTrailing?.invoke()
        }
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= maxLength) onValueChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { focused = it.isFocused },
            readOnly = readOnly,
            textStyle = textStyle.copy(color = C.Ink),
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(capitalization = capitalization, keyboardType = keyboard, imeAction = imeAction),
            keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }, onGo = { onDone?.invoke() }, onSearch = { onDone?.invoke() }),
            visualTransformation = if (password) PasswordVisualTransformation() else visualTransformation,
            cursorBrush = SolidColor(C.Ink),
            decorationBox = { inner ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = height)
                        .fieldFrame(focused && !readOnly, error != null, ok != null, readOnly)
                        .padding(horizontal = 16.dp, vertical = if (singleLine) 0.dp else 14.dp),
                    verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    prefix?.invoke()
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) Txt(placeholder, textStyle, color = C.Placeholder, maxLines = if (singleLine) 1 else 6)
                        inner()
                    }
                    trailing?.invoke()
                }
            },
        )
        FieldHint(error, ok, hint)
    }
}

/** Dial code shown inside phone fields, with a divider. */
@Composable
fun DialPrefix(code: String, flag: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (flag != null) Flag(flag, 20.dp)
        Txt(code, T.Input, color = C.Ink)
        Box(Modifier.width(1.dp).height(22.dp).background(C.Line))
    }
}

/** A field that opens a picker (.input styled button with a chevron). */
@Composable
fun SelectField(
    label: String?,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Choose one",
    error: String? = null,
    hint: String? = null,
    focused: Boolean = false,
    height: Dp = 54.dp,
    leading: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) FieldLabel(label)
        Row(
            Modifier
                .fillMaxWidth()
                .height(height)
                .fieldFrame(focused, error != null, false, !enabled)
                .clickable(enabled = enabled, role = Role.DropdownList, onClick = onClick)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            leading?.invoke()
            Txt(value ?: placeholder, T.Input, Modifier.weight(1f), color = if (value == null) C.Placeholder else C.Ink, maxLines = 1)
            if (enabled) Ic(Icons.ChevronDown, tint = C.Slate)
        }
        FieldHint(error, null, hint)
    }
}

/** Search box (.search). */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    @DrawableRes icon: Int = Icons.Search,
) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused },
        textStyle = T.Body.copy(color = C.Ink),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        cursorBrush = SolidColor(C.Ink),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(1.5.dp, if (focused) C.Ink else C.Line, FieldShape)
                    .background(C.White, FieldShape)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Ic(icon, tint = C.Slate)
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Txt(placeholder, T.Body, color = C.Slate, maxLines = 1)
                    inner()
                }
                if (value.isNotEmpty()) RoundIcon(Icons.Close, { onValueChange("") }, style = IconBtn.Plain, size = 28.dp, iconSize = 16.dp, description = "Clear")
            }
        },
    )
}

/** Visual transformation that groups digits with spaces (card numbers, phone numbers). */
class GroupingTransformation(private val groups: List<Int>, private val separator: Char = ' ') : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val raw = text.text
        val breaks = mutableListOf<Int>()
        var acc = 0
        for (g in groups) {
            acc += g
            if (acc in 1 until raw.length) breaks.add(acc)
        }
        val out = StringBuilder()
        raw.forEachIndexed { i, c ->
            if (i in breaks) out.append(separator)
            out.append(c)
        }
        val sepPositions = breaks.mapIndexed { idx, b -> b + idx }
        val mapping = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset + breaks.count { it < offset }
            override fun transformedToOriginal(offset: Int): Int = (offset - sepPositions.count { it < offset }).coerceIn(0, raw.length)
        }
        return androidx.compose.ui.text.input.TransformedText(androidx.compose.ui.text.AnnotatedString(out.toString()), mapping)
    }
}
