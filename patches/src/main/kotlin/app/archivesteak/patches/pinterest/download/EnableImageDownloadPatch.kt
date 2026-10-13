package app.archivesteak.patches.pinterest.download

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.archivesteak.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue

/**
 * JSON key of the Pin field the server now sends as `false`, which hides
 * "Download image" from the Pin overflow menu and the share sheet.
 */
private const val IMAGE_DOWNLOAD_ELIGIBILITY_KEY = "is_eligible_for_image_download"

@Suppress("unused")
val enableImageDownloadPatch = bytecodePatch(
    name = "Enable image download",
    description = "Restores the \"Download image\" option in the Pin menu and share sheet.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    execute {
        // The Pin model is obfuscated, so find it by the field's JSON name annotation.
        val pinClass = classDefBy { classDef ->
            classDef.type.startsWith("Lcom/pinterest/api/model/") &&
                classDef.fields.any { it.isImageDownloadEligibilityField() }
        }
        val eligibilityField = pinClass.fields.single { it.isImageDownloadEligibilityField() }

        // The getter returns the field, or false if unset. Both the overflow menu
        // and the share sheet read eligibility only through it.
        mutableClassDefBy(pinClass).methods.single { method ->
            method.parameters.isEmpty() &&
                method.returnType == eligibilityField.type &&
                method.implementation?.instructions?.any { instruction ->
                    instruction.opcode == Opcode.IGET_OBJECT &&
                        ((instruction as ReferenceInstruction).reference as FieldReference).let {
                            it.definingClass == pinClass.type && it.name == eligibilityField.name
                        }
                } == true
        }.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                return-object v0
            """
        )
    }
}

private fun Field.isImageDownloadEligibilityField() = annotations.any { annotation ->
    annotation.elements.any { (it.value as? StringEncodedValue)?.value == IMAGE_DOWNLOAD_ELIGIBILITY_KEY }
}
