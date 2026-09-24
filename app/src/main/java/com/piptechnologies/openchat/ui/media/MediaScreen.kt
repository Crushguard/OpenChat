package com.piptechnologies.openchat.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.media.MediaCategory
import com.piptechnologies.openchat.core.media.MediaDayGrouper
import com.piptechnologies.openchat.core.media.RecoveredMedia
import com.piptechnologies.openchat.core.phone.RelativeTime
import com.piptechnologies.openchat.ui.components.ConfirmSheet
import com.piptechnologies.openchat.ui.components.ConfirmSpec
import com.piptechnologies.openchat.ui.components.EmptyState
import com.piptechnologies.openchat.ui.components.FilterPill
import com.piptechnologies.openchat.ui.components.HatchedPlaceholder
import com.piptechnologies.openchat.ui.components.LocalMediaThumbnail
import com.piptechnologies.openchat.ui.components.OcModalSheet
import com.piptechnologies.openchat.ui.components.OcTopBar
import com.piptechnologies.openchat.ui.components.PrimaryButton
import com.piptechnologies.openchat.ui.components.ScreenSurface
import com.piptechnologies.openchat.ui.components.SectionEyebrow
import com.piptechnologies.openchat.ui.components.TopBarIconButton
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.messages.ToolSettingsSheetContent
import com.piptechnologies.openchat.ui.theme.OcRadius
import com.piptechnologies.openchat.ui.theme.OcTheme
import com.piptechnologies.openchat.ui.theme.ToolTint

/** Gap between tiles, both ways, and below each day eyebrow. */
private val TileGap = 8.dp

/** The time chip's 85 % white. */
private val TimeChipBackground = Color.White.copy(alpha = 0.85f)

/**
 * Deleted media (design map §4.12): bar with the tool settings button, the five category pills, then the
 * [MediaUiState.tab]'s items as a 3-column grid under day eyebrows ("Today · 2"), or the empty state, which
 * adds "Allow media access" while the permission is missing (ruling R17). [thumbnail] draws photo and
 * video tiles (screenshots pass a placeholder). The tool settings sheet and the clear-all confirmation
 * open from [MediaUiState.toolSettingsOpen] and [MediaUiState.confirmClear].
 */
@Composable
fun MediaScreen(
    state: MediaUiState,
    callbacks: MediaCallbacks,
    thumbnail: @Composable (RecoveredMedia, Modifier) -> Unit = { m, mod -> LocalMediaThumbnail(m.localPath, mod) },
) {
    ScreenSurface {
        OcTopBar(
            title = stringResource(R.string.media_title),
            onBack = callbacks.onBack,
            actions = {
                TopBarIconButton(
                    icon = LucideIcon.SlidersHorizontal,
                    contentDescription = stringResource(R.string.media_tool_settings),
                    onClick = callbacks.onOpenToolSettings,
                )
            },
        )
        CategoryPills(selected = state.tab, onSelect = callbacks.onTab)
        if (state.groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                MediaEmptyState(tab = state.tab, hasPermission = state.hasPermission, onRequestPermission = callbacks.onRequestPermission)
            }
        } else {
            // Keyed by tab: each category opens scrolled to its newest day.
            key(state.tab) {
                MediaGrid(
                    groups = state.groups,
                    onOpen = callbacks.onOpen,
                    thumbnail = thumbnail,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            }
        }
        // Ruling R2: no ad banner slot; the screen keeps the slot's 14 dp bottom padding instead.
        Spacer(Modifier.height(14.dp))
    }
    if (state.toolSettingsOpen) {
        OcModalSheet(onDismissRequest = callbacks.onDismissToolSettings) {
            ToolSettingsSheetContent(
                title = stringResource(R.string.media_title),
                paused = state.paused,
                excludedCount = state.excludedCount,
                clearLabel = stringResource(R.string.tool_settings_clear_media),
                onTogglePause = callbacks.onTogglePause,
                onExclude = callbacks.onExclude,
                onClear = callbacks.onAskClear,
            )
        }
    }
    if (state.confirmClear) {
        ConfirmSheet(spec = clearMediaSpec(), onDismiss = callbacks.onDismissClear, onConfirm = callbacks.onConfirmClear)
    }
}

/** "Delete all recovered media?" (§4.10, §4.20): trash in the destructive tints, Keep / Clear in destructive. */
@Composable
@ReadOnlyComposable
fun clearMediaSpec(): ConfirmSpec {
    val c = OcTheme.colors
    return ConfirmSpec(
        icon = LucideIcon.Trash,
        tint = ToolTint(bg = c.destructiveTint, fg = c.destructive),
        title = stringResource(R.string.clear_media_title),
        body = stringResource(R.string.clear_body),
        cancelLabel = stringResource(R.string.keep),
        confirmLabel = stringResource(R.string.clear),
        destructive = true,
    )
}

/** Photos, Videos, Audio, Documents, Stickers: padding 2 20 12, gap 8, scrolls sideways when it overflows. */
@Composable
private fun CategoryPills(selected: MediaCategory, onSelect: (MediaCategory) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 2.dp, end = 20.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MediaCategory.entries.forEach { category ->
            FilterPill(label = category.tabLabel, selected = category == selected, onClick = { onSelect(category) })
        }
    }
}

/**
 * One grid for every day: each day's eyebrow spans the three columns (2 dp above the first, 16 above the
 * others counting the row gap, 8 below), then its square tiles. Padding 0 20 12.
 */
@Composable
private fun MediaGrid(
    groups: List<MediaDayGrouper.Group>,
    onOpen: (RecoveredMedia) -> Unit,
    thumbnail: @Composable (RecoveredMedia, Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(TileGap),
        horizontalArrangement = Arrangement.spacedBy(TileGap),
    ) {
        groups.forEachIndexed { index, group ->
            item(
                // A day is named after its newest item: labels alone ("Mon 22 Sep") can repeat a year apart.
                key = "day-${group.items.firstOrNull()?.id ?: group.label}",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "day",
            ) {
                SectionEyebrow(
                    text = stringResource(R.string.media_day_count, group.label, group.items.size),
                    modifier = Modifier.padding(top = if (index == 0) 2.dp else 16.dp - TileGap),
                )
            }
            items(items = group.items, key = { it.id }, contentType = { "tile" }) { media ->
                MediaTile(media = media, onClick = { onOpen(media) }, thumbnail = thumbnail)
            }
        }
    }
}

/**
 * Square tile, radius 14, 1 px border. Photos and videos show [thumbnail] (videos add a filled play 20);
 * audio, documents and stickers show their type icon and file name on the hatched placeholder. The time
 * chip sits 8 from the left and 7 from the bottom.
 */
@Composable
private fun MediaTile(media: RecoveredMedia, onClick: () -> Unit, thumbnail: @Composable (RecoveredMedia, Modifier) -> Unit) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(OcRadius.md)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .border(1.dp, c.border, shape)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        when (media.category) {
            MediaCategory.PHOTO, MediaCategory.VIDEO -> thumbnail(media, Modifier.matchParentSize())
            MediaCategory.AUDIO -> FileTile(icon = LucideIcon.Music, name = media.displayName)
            MediaCategory.DOCUMENT -> FileTile(icon = LucideIcon.FileText, name = media.displayName)
            MediaCategory.STICKER -> FileTile(icon = LucideIcon.Sticker, name = media.displayName)
        }
        if (media.category == MediaCategory.VIDEO) {
            LucideIconImage(
                icon = LucideIcon.Play,
                size = 20.dp,
                tint = c.inkMuted,
                filled = true,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        val time = remember(media.originalModifiedAt) { RelativeTime.clock(media.originalModifiedAt) }
        Text(
            text = time,
            style = OcTheme.type.tileTime9_5,
            color = c.ink2,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 7.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(TimeChipBackground)
                .padding(horizontal = 5.dp, vertical = 2.dp),
        )
    }
}

/** Audio, document and sticker tiles: type icon 24 inkMuted over the file name (mono 10.5), clear of the time chip. */
@Composable
private fun BoxScope.FileTile(icon: LucideIcon, name: String) {
    val c = OcTheme.colors
    HatchedPlaceholder(modifier = Modifier.matchParentSize())
    Column(
        modifier = Modifier
            .matchParentSize()
            .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LucideIconImage(icon = icon, size = 24.dp, tint = c.inkMuted)
        Spacer(Modifier.height(6.dp))
        Text(
            text = name,
            style = OcTheme.type.mono10_5,
            color = c.ink2,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * "Nothing recovered yet" (Photos, Videos) or "No audio yet" … with the image icon and the kept-files body.
 * Without the storage/media permission it adds "Allow media access", the only place that asks for it.
 */
@Composable
private fun MediaEmptyState(tab: MediaCategory, hasPermission: Boolean, onRequestPermission: () -> Unit) {
    val body = stringResource(R.string.media_empty_body)
    if (hasPermission) {
        EmptyState(icon = LucideIcon.Image, title = tab.emptyTitle, body = body)
    } else {
        val allow = stringResource(R.string.media_allow)
        EmptyState(
            icon = LucideIcon.Image,
            title = tab.emptyTitle,
            body = body,
            action = {
                PrimaryButton(text = allow, onClick = onRequestPermission, modifier = Modifier.widthIn(max = 240.dp))
            },
        )
    }
}
