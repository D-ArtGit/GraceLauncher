package com.galaxyrio.gracelauncher.ui.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.galaxyrio.gracelauncher.R
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

/** AndroidX delegates to the system on Android 13+; Room must not hold a second preference. */
@Composable
internal fun rememberAppLanguage(): MutableState<String> {
    val configuration = LocalConfiguration.current
    val language = remember(configuration) { mutableStateOf(currentAppLanguage()) }
    LifecycleResumeEffect(configuration) {
        language.value = currentAppLanguage()
        onPauseOrDispose { }
    }
    return language
}

private fun currentAppLanguage(): String = AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag().orEmpty()

@Composable
internal fun appLanguageSummary(languageTag: String): String = if (languageTag.isEmpty()) {
    stringResource(R.string.settings_follow_system)
} else nativeLanguageName(Locale.forLanguageTag(languageTag))

private fun nativeLanguageName(locale: Locale): String = locale.getDisplayName(locale)
    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

/** The system settings and in-app list share one source of supported languages. */
internal fun supportedAppLanguages(context: Context): List<Locale> = buildList {
    context.resources.getXml(R.xml.locales_config).use { parser ->
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                val tag = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "name")
                add(Locale.forLanguageTag(tag))
            }
            parser.next()
        }
    }
}

@Composable
internal fun LanguageSettings(onBack: () -> Unit) {
    val context = LocalContext.current
    val displayLocale = LocalConfiguration.current.locales[0]
    val languages = remember(context) { supportedAppLanguages(context) }
    var languageTag by rememberAppLanguage()
    val requested = languageTag.takeIf(String::isNotEmpty)?.let(Locale::forLanguageTag)
    val selectedLanguage = languages.firstOrNull { it == requested }
        ?: requested?.let { locale -> languages.firstOrNull { LocaleListCompat.matchesLanguageAndScript(it, locale) } }
    val choices = listOf<Locale?>(null) + languages

    SettingsScaffold(stringResource(R.string.settings_language), "settings_language", onBack) { padding ->
        SettingsList(padding) {
            item { Spacer(Modifier.height(24.dp)) }
            itemsIndexed(choices, key = { _, locale -> locale?.toLanguageTag() ?: "system" }) { index, locale ->
                val tag = locale?.toLanguageTag().orEmpty()
                val checked = if (locale == null) languageTag.isEmpty() else locale == selectedLanguage
                val title = appLanguageSummary(tag)
                val summary = locale?.getDisplayName(displayLocale)?.takeUnless { it.equals(title, ignoreCase = true) }
                SettingsActionItem(title, summary, index, choices.size, "language:${tag.ifEmpty { "system" }}",
                    leading = { RadioButton(checked, onClick = null) },
                    modifier = Modifier.semantics { role = Role.RadioButton; selected = checked },
                ) {
                    if (tag != languageTag) {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                        // Selecting the same effective language may not recreate the Activity.
                        languageTag = tag
                    }
                }
            }
        }
    }
}
