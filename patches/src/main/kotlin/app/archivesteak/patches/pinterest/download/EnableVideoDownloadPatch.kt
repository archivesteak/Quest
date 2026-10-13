package app.archivesteak.patches.pinterest.download

import app.archivesteak.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import app.archivesteak.patches.pinterest.shared.fieldWithJsonName
import app.archivesteak.patches.pinterest.shared.getterOf
import app.archivesteak.patches.pinterest.shared.modelClassWithJsonFields
import app.archivesteak.patches.pinterest.shared.smaliReference
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.ReferenceType
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val EXTENSION_CLASS = "Lapp/archivesteak/extension/pinterest/VideoDownloadPatch;"

@Suppress("unused")
val enableVideoDownloadPatch = bytecodePatch(
    name = "Enable video download",
    description = "Adds \"Download image\" to the menu of video Pins and saves the video in the best available quality.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    // The menu row also requires the Pin to be eligible for image download.
    dependsOn(enableImageDownloadPatch)

    extendWith("extensions/pinterest.mpe")

    execute {
        // region Show the download row for video Pins.

        // The menu builders skip the download row when GridActionsViewParams.isVideo is set.
        val isVideoField = GridActionsViewParamsToStringFingerprint.originalMethod
            .fieldPrintedAfter(IS_VIDEO_LABEL)
        val menuItemType = OverflowMenuItemEnumFingerprint.originalClassDef.type

        val menuBuilders = Fingerprint(
            filters = listOf(
                fieldAccess(reference = isVideoField, opcode = Opcode.IGET_BOOLEAN),
                fieldAccess(definingClass = menuItemType, name = "DOWNLOAD", opcode = Opcode.SGET_OBJECT),
            )
        ).matchAll()

        // isVideo is only used for the download row in these methods, so it can be read as false.
        menuBuilders.forEach { match ->
            match.method.apply {
                implementation!!.instructions.withIndex().filter { (_, instruction) ->
                    instruction.opcode == Opcode.IGET_BOOLEAN &&
                        ((instruction as ReferenceInstruction).reference as FieldReference).let {
                            it.definingClass == isVideoField.definingClass && it.name == isVideoField.name
                        }
                }.map { it.index }.reversed().forEach { index ->
                    val register = getInstruction<TwoRegisterInstruction>(index).registerA
                    replaceInstruction(index, "const/4 v$register, 0x0")
                }
            }
        }

        // endregion

        // region Download the video instead of the image when the row is tapped.

        val pinClass = modelClassWithJsonFields(IMAGE_DOWNLOAD_ELIGIBILITY_KEY, "videos")
        val videosGetter = pinClass.getterOf(pinClass.fieldWithJsonName("videos"))

        val videoClass = classDefBy(videosGetter.returnType)
        val videoListGetter = videoClass.getterOf(videoClass.fieldWithJsonName("video_list"))

        // Values of video_list, one per encoding.
        val videoVariantClass = modelClassWithJsonFields("url", "manifest", "captions_urls")
        val urlGetter = videoVariantClass.getterOf(videoVariantClass.fieldWithJsonName("url"))

        var hookedHandlers = 0
        DownloadImageClickFingerprint.matchAll().forEach { match ->
            // The menu presenters keep the Pin the menu was opened for.
            val pinField = match.originalClassDef.fields.singleOrNull { it.type == pinClass.type }
                ?: return@forEach

            match.method.addInstructionsWithLabels(
                0,
                """
                    iget-object v0, p0, ${match.originalClassDef.type}->${pinField.name}:${pinField.type}
                    if-eqz v0, :download_image
                    invoke-virtual { v0 }, ${videosGetter.smaliReference}
                    move-result-object v0
                    if-eqz v0, :download_image
                    invoke-virtual { v0 }, ${videoListGetter.smaliReference}
                    move-result-object v0
                    const-string v1, "${urlGetter.name}"
                    invoke-static { p1, v0, v1 }, $EXTENSION_CLASS->downloadVideo(Landroid/content/Context;Ljava/util/Map;Ljava/lang/String;)Z
                    move-result v0
                    if-eqz v0, :download_image
                    return-void
                """,
                ExternalLabel("download_image", match.method.getInstruction(0))
            )
            hookedHandlers++
        }

        if (hookedHandlers == 0) throw PatchException("No download click handler holds a Pin")

        // endregion
    }
}

/**
 * The boolean field a data class `toString()` prints after [label].
 *
 * R8 moves the appends into helpers such as `append(sb, previousValue, label, value, nextLabel)`
 * and loads values out of order, so the field is found by following the label's register
 * to the call that prints it, then the value register back to the field read.
 */
private fun Method.fieldPrintedAfter(label: String): FieldReference {
    val instructions = implementation!!.instructions.toList()

    val labelIndex = instructions.indexOfFirst {
        it.opcode == Opcode.CONST_STRING &&
            ((it as ReferenceInstruction).reference as StringReference).string == label
    }
    val labelRegister = (instructions[labelIndex] as OneRegisterInstruction).registerA

    val calls = instructions.withIndex().drop(labelIndex + 1)
        .filter { (_, instruction) -> instruction.opcode.referenceType == ReferenceType.METHOD }
    val labelCall = calls.first { (_, instruction) -> labelRegister in instruction.argumentRegisters }

    val labelCallArguments = labelCall.value.argumentRegisters
    val valueArgumentIndex = labelCallArguments.indexOf(labelRegister) + 1
    // A plain StringBuilder.append(label) prints the value in the next call.
    val valueCall = if (valueArgumentIndex < labelCallArguments.size) labelCall
    else calls.first { (index, _) -> index > labelCall.index }
    val valueRegister = if (valueCall === labelCall) labelCallArguments[valueArgumentIndex]
    else valueCall.value.argumentRegisters.last()

    val valueRead = instructions.subList(0, valueCall.index).last {
        it.opcode == Opcode.IGET_BOOLEAN && (it as TwoRegisterInstruction).registerA == valueRegister
    }
    return (valueRead as ReferenceInstruction).reference as FieldReference
}

private val Instruction.argumentRegisters: List<Int>
    get() = when (this) {
        is RegisterRangeInstruction -> List(registerCount) { startRegister + it }
        is FiveRegisterInstruction -> listOf(registerC, registerD, registerE, registerF, registerG)
            .take(registerCount)
        else -> emptyList()
    }
