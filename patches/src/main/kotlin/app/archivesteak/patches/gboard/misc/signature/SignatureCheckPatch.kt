package app.archivesteak.patches.gboard.misc.signature

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * On start, the app checks that it's signed by Google and crashes otherwise. Patched apps are
 * re-signed, so every Gboard patch depends on this.
 *
 * Only the check of the app's own package passes. The check is also used for other apps,
 * which must still be signed by Google.
 */
internal val signatureCheckPatch = bytecodePatch {
    execute {
        val checkMethod = SignatureDigestFingerprint.classDef.methods.singleOrNull {
            AccessFlags.STATIC.isSet(it.accessFlags) &&
                it.returnType == "Z" &&
                it.parameterTypes.map(CharSequence::toString) == listOf("Landroid/content/Context;", "Ljava/lang/String;")
        } ?: throw PatchException("Signature check not found")

        checkMethod.apply {
            if (implementation!!.registerCount <= parameters.size) {
                throw PatchException("Signature check has no register to use")
            }

            addInstructionsWithLabels(
                0,
                """
                    invoke-virtual { p0 }, Landroid/content/Context;->getPackageName()Ljava/lang/String;
                    move-result-object v0
                    invoke-virtual { v0, p1 }, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                    move-result v0
                    if-eqz v0, :check_signature
                    return v0
                """,
                ExternalLabel("check_signature", getInstruction(0))
            )
        }
    }
}
