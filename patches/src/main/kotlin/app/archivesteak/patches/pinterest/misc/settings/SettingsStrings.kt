package app.archivesteak.patches.pinterest.misc.settings

import app.archivesteak.patches.pinterest.shared.stringsPatch

/**
 * Adds strings the patch settings use to the app's resources. The settings screen only shows
 * settings that have a title, so a setting shows when the patch adding its strings is applied.
 *
 * Names are `category_<category>` for category titles,
 * and `<key>_title` and `<key>_summary` for settings.
 */
internal fun settingsStringsPatch(vararg strings: Pair<String, String>) =
    stringsPatch(*strings.map { (name, value) -> "settings_$name" to value }.toTypedArray())
