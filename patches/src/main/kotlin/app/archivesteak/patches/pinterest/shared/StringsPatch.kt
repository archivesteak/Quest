package app.archivesteak.patches.pinterest.shared

import app.archivesteak.patches.shared.escapeForResource
import app.morphe.patcher.patch.resourcePatch

/** Prefix of the app strings the patches add. The extension uses the same prefix. */
private const val STRING_PREFIX = "archivesteak_"

/**
 * Adds strings to the app's resources, named without [STRING_PREFIX].
 * Strings another patch already added are skipped.
 */
internal fun stringsPatch(vararg strings: Pair<String, String>) = resourcePatch {
    execute {
        document("res/values/strings.xml").use { document ->
            val resources = document.documentElement
            val stringElements = resources.getElementsByTagName("string")
            val existingNames = (0 until stringElements.length)
                .map { stringElements.item(it).attributes.getNamedItem("name").nodeValue }
                .toSet()

            strings.forEach { (name, value) ->
                val resourceName = STRING_PREFIX + name
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
