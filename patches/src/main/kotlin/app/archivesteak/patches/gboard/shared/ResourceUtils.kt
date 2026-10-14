package app.archivesteak.patches.gboard.shared

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatchContext
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Changes the string resource [name] in every language it's translated to.
 *
 * @throws PatchException If the app has no such string.
 */
internal fun ResourcePatchContext.editString(name: String, edit: (String) -> String) {
    var found = false
    get("res").listFiles { file -> file.isDirectory && file.name.startsWith("values") }!!
        .map { "res/${it.name}/strings.xml" }
        .filter { get(it).exists() }
        .forEach { path ->
            document(path).use { document ->
                document.strings().filter { it.getAttribute("name") == name }.forEach {
                    it.textContent = edit(it.textContent)
                    found = true
                }
            }
        }
    if (!found) throw PatchException("String not found: $name")
}

internal fun Document.strings() = getElementsByTagName("string").let { nodes ->
    (0 until nodes.length).map { nodes.item(it) as Element }
}
