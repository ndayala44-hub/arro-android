package app.arro.ui.components

import app.arro.R
import app.arro.ui.str
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.arro.ui.theme.C
import app.arro.ui.theme.T

/** Widest the phone layout grows on tablets; the background still fills the screen. */
val MaxContentWidth = 520.dp

/**
 * A screen laid out like the design's .scr: optional top bar, a scrolling body with 20dp side padding,
 * and a footer pinned to the bottom (above the keyboard and navigation bar).
 */
@Composable
fun ArroScreen(
    modifier: Modifier = Modifier,
    background: Color = C.White,
    topBar: (@Composable () -> Unit)? = null,
    scroll: Boolean = true,
    scrollState: ScrollState? = null,
    bodyPadding: Dp = 20.dp,
    gap: Dp = 16.dp,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (background == C.Night) DarkSystemBars()
    Box(modifier.fillMaxSize().background(background)) {
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = MaxContentWidth)
                .fillMaxSize()
                .imePadding(),
        ) {
            if (topBar != null) topBar() else Spacer(Modifier.statusBarsPadding().height(12.dp))
            val ss = scrollState ?: rememberScrollState()
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(if (scroll) Modifier.verticalScroll(ss) else Modifier)
                    .padding(horizontal = bodyPadding),
                verticalArrangement = Arrangement.spacedBy(gap),
                content = {
                    content()
                    if (footer == null) Spacer(Modifier.navigationBarsPadding().height(16.dp))
                },
            )
            if (footer != null) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = footer,
                )
            }
        }
        overlay?.invoke(this)
    }
}

/** The design's .bar: back button left, optional centred title, optional right slot. */
@Composable
fun TopBar(
    onBack: (() -> Unit)? = null,
    title: String? = null,
    titleColor: Color = C.Ink,
    backStyle: IconBtn = IconBtn.Lime,
    left: (@Composable () -> Unit)? = null,
    right: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 6.dp)
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            left != null -> left()
            onBack != null -> BackButton(onBack, backStyle)
            else -> Spacer(Modifier.width(44.dp))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (title != null) Txt(title, T.H3.copy(fontSize = T.Button.fontSize), color = titleColor, align = TextAlign.Center, maxLines = 1)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (right != null) right() else Spacer(Modifier.width(44.dp))
        }
    }
}

/** Small caption in the top bar's right slot, like "Step 1 of 5". */
@Composable
fun BarCaption(text: String, color: Color = C.Slate) = Cap(text, color = color)

@Composable
fun ScreenTitle(
    title: String,
    lede: String? = null,
    modifier: Modifier = Modifier,
    size: TitleSize = TitleSize.Default,
    color: Color = C.Ink,
    ledeColor: Color = C.Slate,
    align: TextAlign? = null,
) {
    Column(modifier) {
        val st = when (size) { TitleSize.XL -> T.TitleXL; TitleSize.L -> T.TitleL; TitleSize.Default -> T.Title; TitleSize.S -> T.Title.copy(fontSize = T.Title.fontSize * 0.923f) }
        Txt(title, st, color = color, upper = true, align = align)
        if (lede != null) Txt(lede, T.Lede, Modifier.padding(top = 8.dp), color = ledeColor, align = align)
    }
}

enum class TitleSize { XL, L, Default, S }

/** Section header with an optional action link (.sec). */
@Composable
@Suppress("ModifierParameter") // screens pass the earlier arguments by position
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null, modifier: Modifier = Modifier, size: Float = 16f) {
    Row(modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Txt(title, T.H2.copy(fontSize = androidx.compose.ui.unit.TextUnit(size, androidx.compose.ui.unit.TextUnitType.Sp)))
        if (action != null && onAction != null) LinkText(action, onAction)
    }
}

/** Bottom sheet over a scrim, drawn inside the current screen like the design's .sheet. */
@Composable
fun BoxScope.Sheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    fullHeight: Boolean = false,
    topOffset: Dp = 150.dp,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(enabled = visible && dismissible) { onDismiss() }
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.matchParentSize()) {
        Box(Modifier.fillMaxSize().background(C.Scrim).tap(enabled = dismissible) { onDismiss() })
    }
    AnimatedVisibility(
        visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        Column(
            modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxWidth()
                .then(if (fullHeight) Modifier.fillMaxHeight().padding(top = topOffset) else Modifier)
                .background(C.White, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .tap { }
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 18.dp).size(width = 40.dp, height = 5.dp).background(C.Grab, RoundedCornerShape(3.dp)))
            content()
        }
    }
}

/** Header row for sheets: title and close button. */
@Composable
fun SheetHeader(title: String, onClose: () -> Unit, subtitle: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Txt(title, T.H2.copy(fontSize = T.Title.fontSize * 0.77f), Modifier.weight(1f))
        RoundIcon(Icons.Close, onClose, style = IconBtn.Soft, size = 36.dp, iconSize = 16.dp, description = str(R.string.ui_layout_close))
    }
    if (subtitle != null) Cap(subtitle, Modifier.padding(top = 4.dp))
}

/** A centred confirmation card over a scrim. */
@Composable
fun BoxScope.ConfirmDialog(
    visible: Boolean,
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismiss: String = str(R.string.ui_layout_cancel),
    danger: Boolean = false,
    loading: Boolean = false,
) {
    BackHandler(enabled = visible) { onDismiss() }
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.matchParentSize()) {
        Box(Modifier.fillMaxSize().background(C.Scrim).tap { onDismiss() }, contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .padding(28.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .background(C.White, RoundedCornerShape(24.dp))
                    .tap { }
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Txt(title, T.H2.copy(fontSize = T.Title.fontSize * 0.77f))
                Txt(body, T.Body, color = C.Slate)
                VSpace(8.dp)
                ArroButton(confirm, onConfirm, style = if (danger) Btn.Danger else Btn.Dark, loading = loading)
                ArroButton(dismiss, onDismiss, style = Btn.Text)
            }
        }
    }
}

/** A selectable list in a sheet, used for every dropdown-style field. */
@Composable
fun BoxScope.PickerSheet(
    visible: Boolean,
    title: String,
    options: List<String>,
    selected: String?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    subtitles: Map<String, String> = emptyMap(),
    leading: (@Composable (String) -> Unit)? = null,
) {
    Sheet(visible, onDismiss) {
        SheetHeader(title, onDismiss)
        VSpace(12.dp)
        Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { o ->
                ListRow(
                    title = o,
                    subtitle = subtitles[o],
                    selected = o == selected,
                    leading = leading?.let { l -> { l(o) } },
                    trailing = { Radio(o == selected) },
                    onClick = { onPick(o); onDismiss() },
                )
            }
        }
    }
}

/** Placeholder block for loading skeletons. */
@Composable
fun Skeleton(width: Dp?, height: Dp, modifier: Modifier = Modifier, radius: Dp = 10.dp) {
    val brush = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(C.SkeletonA, C.SkeletonB, C.SkeletonA))
    Box(modifier.then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth()).height(height).background(brush, RoundedCornerShape(radius)))
}
