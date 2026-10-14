package app.archivesteak.patches.gboard.misc.settings

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Document
import org.w3c.dom.Element

internal const val SETTINGS_SCREEN_TITLE = "Quest"
private const val SETTINGS_SCREEN_KEY = "quest_settings"
private const val SETTINGS_SCREEN_FILE = "res/xml/$SETTINGS_SCREEN_KEY.xml"
private const val SETTINGS_FRAGMENT = "app.archivesteak.extension.gboard.settings.QuestSettingsFragment"

/** Gboard's main settings screens. The legacy one is shown on some devices. */
private val MAIN_SETTINGS_FILES = listOf("res/xml/settings.xml", "res/xml/settings_legacy.xml")
private const val HEADER_PREFERENCE = "com.google.android.libraries.inputmethod.settings.widget.HeaderPreference"
private const val CATEGORY = "androidx.preference.PreferenceCategory"

/** Its icon is used for the Quest row. */
private const val ICON_SOURCE_FRAGMENT = "com.google.android.apps.inputmethod.latin.preference.PreferencesSettingsFragment"

/**
 * Adds the Quest screen, and a row opening it to the main settings screens.
 */
internal val settingsResourcePatch = resourcePatch {
    execute {
        get(SETTINGS_SCREEN_FILE).writeText(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android"
                    android:title="$SETTINGS_SCREEN_TITLE"
                    android:key="$SETTINGS_SCREEN_KEY"/>
            """.trimIndent()
        )

        MAIN_SETTINGS_FILES.filter { get(it).exists() }.forEach { path ->
            document(path).use { document ->
                val screen = document.documentElement
                val categories = screen.childElements().filter { it.tagName == CATEGORY }
                val icon = document.elements(HEADER_PREFERENCE)
                    .firstOrNull { it.getAttribute("android:fragment") == ICON_SOURCE_FRAGMENT }
                    ?.getAttribute("android:icon")

                val row = document.createElement(HEADER_PREFERENCE).apply {
                    if (!icon.isNullOrEmpty()) setAttribute("android:icon", icon)
                    setAttribute("android:persistent", "false")
                    setAttribute("android:title", SETTINGS_SCREEN_TITLE)
                    setAttribute("android:key", SETTINGS_SCREEN_KEY)
                    setAttribute("android:fragment", SETTINGS_FRAGMENT)
                }
                val category = document.createElement(CATEGORY).apply { appendChild(row) }

                // Above the last group, which has privacy, help and about.
                screen.insertBefore(category, categories.lastOrNull())
            }
        }
    }
}

/** A setting on the Quest screen. Its key must match the setting in the extension. */
internal sealed class QuestPreference(val key: String, val title: String, val summary: String) {
    abstract fun toElement(document: Document): Element

    protected fun Element.setCommonAttributes() = apply {
        setAttribute("android:key", key)
        setAttribute("android:title", title)
        setAttribute("android:summary", summary)
    }
}

internal class SwitchPreference(
    key: String,
    title: String,
    summary: String,
    private val defaultValue: Boolean,
) : QuestPreference(key, title, summary) {
    override fun toElement(document: Document): Element = document.createElement("SwitchPreferenceCompat").apply {
        setCommonAttributes()
        setAttribute("android:defaultValue", defaultValue.toString())
    }
}

internal class TextPreference(key: String, title: String, summary: String) : QuestPreference(key, title, summary) {
    override fun toElement(document: Document): Element = document.createElement("EditTextPreference").apply {
        setCommonAttributes()
        setAttribute("android:dialogTitle", title)
    }
}

/**
 * Adds [preferences] to the Quest screen, in a category titled [category].
 */
internal fun preferencesPatch(category: String, vararg preferences: QuestPreference) = resourcePatch {
    dependsOn(settingsResourcePatch)

    execute {
        document(SETTINGS_SCREEN_FILE).use { document ->
            val screen = document.documentElement
            val categoryElement = screen.childElements().firstOrNull { it.getAttribute("android:title") == category }
                ?: document.createElement(CATEGORY).apply {
                    setAttribute("android:title", category)
                    screen.appendChild(this)
                }

            preferences.forEach { categoryElement.appendChild(it.toElement(document)) }
        }
    }
}

private fun Element.childElements() = (0 until childNodes.length).mapNotNull { childNodes.item(it) as? Element }

private fun Document.elements(tagName: String) = getElementsByTagName(tagName).let { nodes ->
    (0 until nodes.length).map { nodes.item(it) as Element }
}
