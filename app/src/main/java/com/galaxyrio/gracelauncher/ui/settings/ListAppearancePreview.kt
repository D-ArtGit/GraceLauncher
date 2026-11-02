@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.galaxyrio.gracelauncher.ui.settings

import android.content.ComponentName
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import com.galaxyrio.gracelauncher.R
import com.galaxyrio.gracelauncher.data.FolderPlacement
import com.galaxyrio.gracelauncher.data.LauncherApp
import com.galaxyrio.gracelauncher.data.LauncherFolder
import com.galaxyrio.gracelauncher.data.ListAppearance
import com.galaxyrio.gracelauncher.data.notifications.AppNotification
import com.galaxyrio.gracelauncher.ui.LauncherUiState
import com.galaxyrio.gracelauncher.ui.components.AppListSectionHeader
import com.galaxyrio.gracelauncher.ui.components.FolderRow
import com.galaxyrio.gracelauncher.ui.components.LauncherAppRow
import com.galaxyrio.gracelauncher.ui.components.LocalLauncherInputEnabled
import com.galaxyrio.gracelauncher.ui.components.LocalListAppearance
import com.galaxyrio.gracelauncher.ui.components.LocalPopupGestureSettings
import com.galaxyrio.gracelauncher.ui.components.endPadding
import com.galaxyrio.gracelauncher.ui.components.startPadding
import com.galaxyrio.gracelauncher.ui.theme.LocalLauncherAppearance
import com.galaxyrio.gracelauncher.ui.theme.rememberLauncherAppearance
import com.galaxyrio.gracelauncher.ui.theme.wallpaperTint

@Composable
internal fun ListAppearancePreview(uiState: LauncherUiState, list: ListAppearance, iconSize: Int, modifier: Modifier) {
    val appearance = rememberLauncherAppearance(uiState.textMode, uiState.themedIcons, iconSize)
    val primary = MaterialTheme.colorScheme.primary
    val colorScheme = MaterialTheme.colorScheme
    val tint = remember(colorScheme, appearance.darkText, list.wallpaperDimColor) {
        appearance.wallpaperTint(primary, list.wallpaperDimColor, colorScheme)
    }
    val dimAlpha = if (uiState.settings.dimWallpaper) uiState.settings.wallpaperDimAmount.coerceIn(0, 100) / 100f else 0f
    val shape = ListItemDefaults.segmentedShapes(0, 1).shape
    val example = stringResource(R.string.icon_designer_example)
    val ordinaryApps = remember(uiState.apps, uiState.effectiveHiddenAppKeys, example) {
        val candidates = uiState.apps.filter {
            it.folderId == null && it.shortcut == null && it.user == null && !it.isLauncherSettings &&
                !it.isPrivateSpace && !it.isWorkProfile && it.key !in uiState.effectiveHiddenAppKeys
        }.distinctBy(LauncherApp::packageName)
        List(2) { index -> candidates.getOrNull(index)
            ?: LauncherApp(ComponentName("list.preview.app$index", "Sample"), example, null) }
    }
    val folderName = stringResource(R.string.list_preview_folder)
    val folder = remember(folderName) {
        LauncherFolder("list-preview", folderName, listOf("list.preview.messages/Sample", "list.preview.calendar/Sample"),
            FolderPlacement.AppList)
    }
    val firstTitle = stringResource(R.string.list_preview_notification_first_title)
    val firstText = stringResource(R.string.list_preview_notification_first_text)
    val secondTitle = stringResource(R.string.list_preview_notification_second_title)
    val secondText = stringResource(R.string.list_preview_notification_second_text)
    val postedAt = remember { System.currentTimeMillis() }
    // Local sample content is independent of notification access and never reads real notifications.
    val sampleNotifications = remember(firstTitle, firstText, secondTitle, secondText, postedAt) {
        listOf(
            AppNotification("list-preview:messages", "list.preview.messages", firstTitle, firstText,
                postedAt, 0L, canDismiss = false, canOpen = false),
            AppNotification("list-preview:calendar", "list.preview.calendar", secondTitle, secondText,
                postedAt, 0L, canDismiss = false, canOpen = false),
        )
    }
    val popupSettings = uiState.settings.shortcutsFolders
    val notifications = popupSettings.previewNotifications(sampleNotifications)
    CompositionLocalProvider(
        LocalLauncherAppearance provides appearance,
        LocalListAppearance provides list,
        LocalLauncherInputEnabled provides false,
        LocalPopupGestureSettings provides popupSettings,
    ) {
        Layout(
            modifier = modifier.fillMaxWidth().testTag("list_appearance_preview").drawWithContent {
                val outline = shape.createOutline(size, layoutDirection, this)
                drawOutline(outline, Color.Transparent, blendMode = BlendMode.Clear)
                drawOutline(outline, tint.copy(alpha = dimAlpha))
                drawContent()
            }.clip(shape),
            content = {
                Column(Modifier.fillMaxWidth().padding(start = list.startPadding, end = list.endPadding)) {
                    AppListSectionHeader("A", Modifier.testTag("list_preview_section"))
                    LauncherAppRow(ordinaryApps[0], {}, {}, Modifier.testTag("list_preview_first_app"),
                        showLabel = list.showNames(favorites = false))
                    FolderRow(folder, {}, null, Modifier.testTag("list_preview_folder"),
                        showLabel = list.showNames(favorites = false), notifications = notifications)
                    LauncherAppRow(ordinaryApps[1], {}, {}, Modifier.testTag("list_preview_last_app"),
                        showLabel = list.showNames(favorites = false))
                }
            },
        ) { measurables, constraints ->
            // Measure complete rows before scaling, including the current notification line count.
            val content = measurables.single().measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
            val width = constraints.constrainWidth(content.width)
            val height = constraints.constrainHeight(content.height)
            val scale = (height.toFloat() / content.height.coerceAtLeast(1)).coerceAtMost(1f)
            layout(width, height) {
                content.placeWithLayer((width - content.width) / 2, (height - content.height) / 2) {
                    scaleX = scale
                    scaleY = scale
                }
            }
        }
    }
}
