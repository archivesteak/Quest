package app.archivesteak.patches.pinterest.misc.settings

import app.morphe.patcher.patch.resourcePatch

/** Prefix of the app strings the patch settings use. The extension uses the same prefix. */
private const val SETTINGS_STRING_PREFIX = "archivesteak_settings_"

/**
 * Adds strings the patch settings use to the app's resources. The settings screen only shows
 * settings that have a title, so a setting shows when the patch adding its strings is applied.
 *
 * Names are without the prefix: `category_<category>` for category titles,
 * and `<key>_title` and `<key>_summary` for settings.
 */
internal fun settingsStringsPatch(vararg strings: Pair<String, String>) = resourcePatch {
    execute {
        document("res/values/strings.xml").use { document ->
            val resources = document.documentElement
            val stringElements = resources.getElementsByTagName("string")
            val existingNames = (0 until stringElements.length)
                .map { stringElements.item(it).attributes.getNamedItem("name").nodeValue }
                .toSet()

            strings.forEach { (name, value) ->
                val resourceName = SETTINGS_STRING_PREFIX + name
                // Several patches can add the same category.
                if (resourceName in existingNames) return@forEach

                resources.appendChild(
                    document.createElement("string").apply {
                        setAttribute("name", resourceName)
                        textContent = value.escapeForResource()
                    }
                )
            }
        }
    }
}

private fun String.escapeForResource() = replace("\\", "\\\\")
    .replace("'", "\\'")
    .replace("\"", "\\\"")
    .replace(Regex("^([@?])"), "\\\\$1")
