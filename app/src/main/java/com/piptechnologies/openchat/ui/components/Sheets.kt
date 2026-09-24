package com.piptechnologies.openchat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.theme.OcRadius
import com.piptechnologies.openchat.ui.theme.OcTheme
import com.piptechnologies.openchat.ui.theme.ToolTint

/**
 * M3 ModalBottomSheet with the design's 24 dp top radius, our own 36×4 handle, surface colour, no system drag handle.
 *
 * With [showHandle] the handle sits 10 dp from the top with 10 dp below it, then [content]; a sheet that
 * needs more room above its first row adds it in its own content. [SheetPreviewFrame] draws the same
 * handle, so a sheet's content composable renders identically in the app and in screenshots.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcModalSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    topRadius: Dp = 24.dp,
    showHandle: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = OcTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = topRadius, topEnd = topRadius),
        containerColor = c.surface,
        scrimColor = c.scrim,
        dragHandle = null,
    ) {
        if (showHandle) SheetTopHandle()
        content()
    }
}

/** The sheet handle: a 36×4 (by default) pill in borderStrong. */
@Composable
fun SheetHandle(modifier: Modifier = Modifier, width: Dp = 36.dp, height: Dp = 4.dp) {
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(percent = 50))
            .background(OcTheme.colors.borderStrong),
    )
}

/** The handle as the sheet containers place it: centred, 10 dp from the top, 10 dp above the content. */
@Composable
private fun ColumnScope.SheetTopHandle() {
    SheetHandle(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(top = 10.dp, bottom = 10.dp),
    )
}

/**
 * A confirmation sheet (design map §2, §4.20): [icon] in a 44/13 box tinted with [tint], [title], [body]
 * and two buttons. [destructive] colours the confirm button destructive instead of green.
 */
data class ConfirmSpec(
    val icon: LucideIcon,
    val tint: ToolTint,
    val title: String,
    val body: String,
    val cancelLabel: String,
    val confirmLabel: String,
    val destructive: Boolean,
)

/** The confirmation sheet body: padding 24 22 22, IconBox 44/13, title18, body13_5 ink2, DialogButtonRow (green or destructive). */
@Composable
fun ConfirmSheetContent(spec: ConfirmSpec, onCancel: () -> Unit, onConfirm: () -> Unit) {
    val c = OcTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, top = 24.dp, end = 22.dp, bottom = 22.dp),
    ) {
        IconBox(icon = spec.icon, tint = spec.tint, size = 44.dp, radius = 13.dp, iconSize = 22.dp)
        Spacer(Modifier.height(14.dp))
        Text(text = spec.title, style = OcTheme.type.title18, color = c.ink)
        Spacer(Modifier.height(6.dp))
        Text(text = spec.body, style = OcTheme.type.body13_5, color = c.ink2)
        Spacer(Modifier.height(20.dp))
        DialogButtonRow(
            cancelLabel = spec.cancelLabel,
            confirmLabel = spec.confirmLabel,
            onCancel = onCancel,
            onConfirm = onConfirm,
            confirmColor = if (spec.destructive) c.destructive else c.green,
        )
    }
}

/** The modal confirmation sheet: 26 dp top radius, no handle. Cancel, Back and a tap on the scrim all call [onDismiss]. */
@Composable
fun ConfirmSheet(spec: ConfirmSpec, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    OcModalSheet(onDismissRequest = onDismiss, topRadius = OcRadius.dialog, showHandle = false) {
        ConfirmSheetContent(spec = spec, onCancel = onDismiss, onConfirm = onConfirm)
    }
}

/**
 * Screenshot helper: draws [screen], the scrim, then [sheet] bottom-aligned in a white surface with the sheet radius. heightFraction null = wrap content.
 *
 * No ModalBottomSheet (it opens its own window, which Paparazzi does not capture). Like [OcModalSheet],
 * a 24 dp sheet gets the handle and a 26 dp confirmation or rating sheet does not (those pass
 * `showHandle = false`). Call it as `SheetPreviewFrame(topRadius = 26.dp, screen = { … }) { sheet content }`.
 */
@Composable
fun SheetPreviewFrame(
    heightFraction: Float? = null,
    topRadius: Dp = 24.dp,
    screen: @Composable () -> Unit,
    sheet: @Composable ColumnScope.() -> Unit,
) {
    val c = OcTheme.colors
    val sheetHeight = if (heightFraction != null) Modifier.fillMaxHeight(heightFraction) else Modifier
    Box(modifier = Modifier.fillMaxSize()) {
        screen()
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(c.scrim),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .then(sheetHeight),
            shape = RoundedCornerShape(topStart = topRadius, topEnd = topRadius),
            color = c.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            ) {
                if (topRadius < OcRadius.dialog) SheetTopHandle()
                sheet()
            }
        }
    }
}
