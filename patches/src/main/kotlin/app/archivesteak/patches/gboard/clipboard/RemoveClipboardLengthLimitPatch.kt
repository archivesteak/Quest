package app.archivesteak.patches.gboard.clipboard

import app.archivesteak.patches.gboard.misc.settings.SwitchPreference
import app.archivesteak.patches.gboard.misc.settings.preferencesPatch
import app.archivesteak.patches.gboard.misc.settings.settingsPatch
import app.archivesteak.patches.gboard.misc.signature.signatureCheckPatch
import app.archivesteak.patches.gboard.shared.Constants.COMPATIBILITY_GBOARD
import app.archivesteak.patches.gboard.shared.EXTENSION_PACKAGE
import app.archivesteak.patches.gboard.shared.hookFlagReads
import app.morphe.patcher.patch.bytecodePatch

private const val EXTENSION_CLASS = "$EXTENSION_PACKAGE/ClipboardPatch;"

/** Server flag holding the length copied text is cut to. */
private const val LENGTH_LIMIT_FLAG = "text_clip_item_char_limit"

@Suppress("unused")
val removeClipboardLengthLimitPatch = bytecodePatch(
    name = "Remove clipboard length limit",
    description = "Adds an option to keep copied text of any length in the clipboard, instead of cutting it off after 20,000 characters.",
    default = true
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    dependsOn(
        signatureCheckPatch,
        settingsPatch,
        preferencesPatch(
            "Clipboard",
            SwitchPreference(
                key = "quest_remove_clipboard_length_limit",
                title = "Remove length limit",
                summary = "Keep copied text of any length, instead of cutting it off after 20,000 characters",
                defaultValue = true,
            ),
        ),
    )

    execute {
        // Saving a copied item cuts its text to the limit, and the clipboard's
        // add and edit screens limit their text box to it.
        hookFlagReads(mapOf(LENGTH_LIMIT_FLAG to "$EXTENSION_CLASS->overrideLengthLimit(Ljava/lang/Object;)Ljava/lang/Object;"))
    }
}
