package app.archivesteak.patches.pinterest.closeup

import app.archivesteak.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import app.archivesteak.patches.pinterest.shared.fieldWithJsonName
import app.archivesteak.patches.pinterest.shared.getterOf
import app.archivesteak.patches.pinterest.shared.smaliReference
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "Lapp/archivesteak/extension/pinterest/DatePostedPatch;"

@Suppress("unused")
val showDatePostedPatch = bytecodePatch(
    name = "Show date posted",
    description = "Adds the date a Pin was posted to the end of its description. Expand the description to see it.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    extendWith("extensions/pinterest.mpe")

    execute {
        PinDescriptionUpdateViewFingerprint.let { fingerprint ->
            // region Implement the extension's date lookup.

            val getPin = fingerprint.originalMethod.implementation!!.instructions.firstNotNullOf { instruction ->
                ((instruction as? ReferenceInstruction)?.reference as? MethodReference)
                    ?.takeIf { it.name == "getPin" && it.parameterTypes.isEmpty() }
            }
            val pinClass = classDefBy(getPin.returnType)
            val createdAtGetter = pinClass.getterOf(pinClass.fieldWithJsonName("created_at"))

            mutableClassDefBy(EXTENSION_CLASS).methods.single { it.name == "getDatePosted" }.addInstructions(
                0,
                """
                    check-cast p0, ${getPin.definingClass}
                    invoke-virtual { p0 }, ${getPin.definingClass}->getPin()${getPin.returnType}
                    move-result-object v0
                    if-eqz v0, :no_pin
                    invoke-virtual { v0 }, ${createdAtGetter.smaliReference}
                    move-result-object v0
                    :no_pin
                    return-object v0
                """
            )

            // endregion

            // region Append the date to the description before it's shown.

            val textRegister = fingerprint.instructionMatches[2].getInstruction<FiveRegisterInstruction>().registerD
            val viewFieldReadIndex = fingerprint.instructionMatches[1].index

            fingerprint.method.apply {
                val viewFieldRead = getInstruction<TwoRegisterInstruction>(viewFieldReadIndex)
                val moduleRegister = viewFieldRead.registerB
                val viewField = (viewFieldRead as ReferenceInstruction).reference as FieldReference

                if (textRegister > 15 || moduleRegister > 15) {
                    throw PatchException("Description registers are out of range for invoke-static")
                }

                // The field read is also a branch target. Inserting before it would let that branch
                // skip the hook, so the read is replaced in place (keeping its label) and re-added after.
                replaceInstruction(
                    viewFieldReadIndex,
                    "invoke-static { v$textRegister, v$moduleRegister }, " +
                        "$EXTENSION_CLASS->appendDatePosted(Ljava/lang/CharSequence;Ljava/lang/Object;)V"
                )
                addInstruction(
                    viewFieldReadIndex + 1,
                    "iget-object v${viewFieldRead.registerA}, v$moduleRegister, " +
                        "${viewField.definingClass}->${viewField.name}:${viewField.type}"
                )
            }

            // endregion
        }
    }
}
