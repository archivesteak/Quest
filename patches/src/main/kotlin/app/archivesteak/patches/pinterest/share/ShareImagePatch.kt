package app.archivesteak.patches.pinterest.share

import app.archivesteak.patches.pinterest.misc.settings.settingsPatch
import app.archivesteak.patches.pinterest.misc.settings.settingsStringsPatch
import app.archivesteak.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import app.archivesteak.patches.pinterest.shared.EXTENSION_PACKAGE
import app.archivesteak.patches.pinterest.shared.pinClass
import app.archivesteak.patches.pinterest.shared.pinModelPatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel

private const val EXTENSION_CLASS = "$EXTENSION_PACKAGE/SharePatch;"

@Suppress("unused")
val shareImagePatch = bytecodePatch(
    name = "Share image with Android",
    description = "Adds an option to share the image of a Pin with Android's share sheet, instead of the app's share menu.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(
        settingsPatch,
        settingsStringsPatch(
            "category_sharing" to "Sharing",
            "share_with_android_title" to "Share with Android",
            "share_with_android_summary" to "Shares the image of a Pin with Android's share sheet, instead of Pinterest's share menu.",
        ),
        pinModelPatch,
    )

    execute {
        OpenShareMenuFingerprint.method.apply {
            // The model is p1. Share menus of boards, profiles and others are left as they are.
            if (implementation!!.registerCount <= parameters.size) throw PatchException("Share menu has no register to use")

            // instance-of only takes the first 16 registers, which p1 isn't in.
            addInstructionsWithLabels(
                0,
                """
                    move-object/from16 v0, p1
                    instance-of v0, v0, ${pinClass.type}
                    if-eqz v0, :share_menu
                    invoke-static/range { p1 .. p1 }, $EXTENSION_CLASS->shareImage(Ljava/lang/Object;)Z
                    move-result v0
                    if-eqz v0, :share_menu
                    return-void
                """,
                ExternalLabel("share_menu", getInstruction(0))
            )
        }
    }
}
