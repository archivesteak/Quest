package app.archivesteak.patches.pinterest.closeup

import app.archivesteak.patches.pinterest.misc.settings.settingsPatch
import app.archivesteak.patches.pinterest.misc.settings.settingsStringsPatch
import app.archivesteak.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import app.archivesteak.patches.pinterest.shared.EXTENSION_PACKAGE
import app.archivesteak.patches.pinterest.shared.pinModelPatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

private const val EXTENSION_CLASS = "$EXTENSION_PACKAGE/DatePostedPatch;"

@Suppress("unused")
val showDatePostedPatch = bytecodePatch(
    name = "Show date posted",
    description = "Adds an option to show the date a Pin was posted, above its creator.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(
        settingsPatch,
        settingsStringsPatch(
            "category_pin_page" to "Pin page",
            "show_date_posted_title" to "Show date posted",
            "show_date_posted_summary" to "Shows when a Pin was posted, above its creator.",
        ),
        pinModelPatch,
    )

    execute {
        CreatorModuleUpdatePinFingerprint.method.addInstructions(
            0,
            "invoke-static/range { p0 .. p1 }, " +
                "$EXTENSION_CLASS->onCreatorModuleUpdated(Landroid/view/View;Ljava/lang/Object;)V"
        )
    }
}
