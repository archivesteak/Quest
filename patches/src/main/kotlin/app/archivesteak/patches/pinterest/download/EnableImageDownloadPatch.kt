package app.archivesteak.patches.pinterest.download

import app.archivesteak.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import app.archivesteak.patches.pinterest.shared.fieldWithJsonName
import app.archivesteak.patches.pinterest.shared.getterOf
import app.archivesteak.patches.pinterest.shared.modelClassWithJsonFields
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod

/**
 * JSON key of the Pin field the server now sends as `false`, which hides
 * "Download image" from the Pin overflow menu and the share sheet.
 */
internal const val IMAGE_DOWNLOAD_ELIGIBILITY_KEY = "is_eligible_for_image_download"

@Suppress("unused")
val enableImageDownloadPatch = bytecodePatch(
    name = "Enable image download",
    description = "Restores the \"Download image\" option in the Pin menu and share sheet.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    execute {
        val pinClass = modelClassWithJsonFields(IMAGE_DOWNLOAD_ELIGIBILITY_KEY)
        val eligibilityField = pinClass.fieldWithJsonName(IMAGE_DOWNLOAD_ELIGIBILITY_KEY)

        // The getter returns the field, or false if unset. Both the overflow menu
        // and the share sheet read eligibility only through it.
        (mutableClassDefBy(pinClass).getterOf(eligibilityField) as MutableMethod).addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                return-object v0
            """
        )
    }
}
