package com.piptechnologies.openchat.screenshots

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import app.cash.paparazzi.Paparazzi
import com.piptechnologies.openchat.ui.settings.LanguageOption
import com.piptechnologies.openchat.ui.settings.Languages
import com.piptechnologies.openchat.ui.theme.OpenChatTheme

/**
 * One screen or state of the design map's screenshot inventory (§6), from the §6 fake data. [name] is the English
 * test method that renders it, so the English file (CI's `<name>.png`), and its file name in every language of
 * [LocaleScreenshotTests]. [content] is drawn inside [OpenChatTheme].
 */
data class Scene(val name: String, val content: @Composable () -> Unit)

/** The 34 scenes, in the order of design map §6. */
object Scenes {
    val all: List<Scene> =
        LaunchScenes.all + HomeScenes.all + GateScenes.all + MessagesScenes.all + MediaScenes.all +
            SecondAccountScenes.all + SettingsScenes.all
}

/**
 * The language a scene shows as the app's current one: the Language row of Settings and the checked row of the
 * Language screen. English, unless [LocaleScreenshotTests] renders the scene in another language.
 */
val LocalSceneLanguage: ProvidableCompositionLocal<LanguageOption> = staticCompositionLocalOf { Languages.all.first() }

/** Snapshots [scene] in the app theme, in English; Paparazzi names the file after the test method. */
fun Paparazzi.snapshot(scene: Scene) {
    snapshot {
        OpenChatTheme {
            scene.content()
        }
    }
}
