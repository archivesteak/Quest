package app.archivesteak.patches.gboard.misc.packagename

import app.archivesteak.patches.gboard.misc.signature.signatureCheckPatch
import app.archivesteak.patches.gboard.shared.Constants.COMPATIBILITY_GBOARD
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import org.w3c.dom.Element

private val PACKAGE_NAME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

/**
 * Gboard is preinstalled as a system app, which a re-signed Gboard can't be installed over.
 * The new package name keeps the original as its prefix, because the app checks for its own
 * package by prefix, for example to skip learning from text typed in its own settings.
 */
@Suppress("unused")
val changePackageNamePatch = resourcePatch(
    name = "Change package name",
    description = "Changes the package name of the app, so it can be installed next to the preinstalled Gboard.",
    default = true
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    dependsOn(signatureCheckPatch)

    val packageNameOption by stringOption(
        key = "packageName",
        default = "${COMPATIBILITY_GBOARD.packageName}.quest",
        title = "Package name",
        description = "The package name of the patched app.",
        required = true
    ) { it != null && PACKAGE_NAME_REGEX.matches(it) }

    execute {
        document("AndroidManifest.xml").use { document ->
            val manifest = document.documentElement
            val packageName = manifest.getAttribute("package")
            val newPackageName = packageNameOption!!
            if (newPackageName == packageName) throw PatchException("The package name is unchanged")

            fun String.renamed() = when {
                this == packageName -> newPackageName
                startsWith("$packageName.") -> newPackageName + removePrefix(packageName)
                else -> this
            }

            val elements = document.getElementsByTagName("*").let { nodes ->
                (0 until nodes.length).map { nodes.item(it) as Element }
            }

            // Provider authorities and permissions the app declares must be unique on the device,
            // so they would clash with the preinstalled app's. Code builds them from the package name.
            val renamedNames = elements.flatMap { element ->
                when (element.tagName) {
                    "provider" -> element.getAttribute("android:authorities").split(';')
                    "permission" -> listOf(element.getAttribute("android:name"))
                    else -> emptyList()
                }
            }.filter { it.renamed() != it }.toSet()

            // Rename them everywhere they're used, such as in uses-permission and protected components.
            elements.forEach { element ->
                val attributes = element.attributes
                (0 until attributes.length).map { attributes.item(it) }.forEach { attribute ->
                    val names = attribute.nodeValue.split(';')
                    if (names.any { it in renamedNames }) {
                        attribute.nodeValue = names.joinToString(";") { if (it in renamedNames) it.renamed() else it }
                    }
                }
            }

            manifest.setAttribute("package", newPackageName)
        }
    }
}
