package app.archivesteak.patches.gboard.misc.appname

import app.archivesteak.patches.gboard.misc.signature.signatureCheckPatch
import app.archivesteak.patches.gboard.shared.Constants.COMPATIBILITY_GBOARD
import app.archivesteak.patches.gboard.shared.editString
import app.archivesteak.patches.shared.escapeForResource
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import org.w3c.dom.Element

private const val STRING_REFERENCE_PREFIX = "@string/"

@Suppress("unused")
val changeAppNamePatch = resourcePatch(
    name = "Change app name",
    description = "Changes the name of the app, so it can be told apart from the preinstalled Gboard.",
    default = true
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    dependsOn(signatureCheckPatch)

    val appNameOption by stringOption(
        key = "appName",
        default = "Gboard Quest",
        title = "App name",
        description = "The name of the app, shown in the app list and the list of keyboards.",
        required = true
    ) { !it.isNullOrBlank() }

    execute {
        // The keyboard and the launcher entry have no name of their own, so they show the app's.
        val label = document("AndroidManifest.xml").use { document ->
            (document.getElementsByTagName("application").item(0) as Element).getAttribute("android:label")
        }
        if (!label.startsWith(STRING_REFERENCE_PREFIX)) {
            throw PatchException("The app name isn't a string resource: $label")
        }

        editString(label.removePrefix(STRING_REFERENCE_PREFIX)) { appNameOption!!.escapeForResource() }
    }
}
