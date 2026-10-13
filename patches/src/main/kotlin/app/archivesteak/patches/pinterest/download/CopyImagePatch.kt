package app.archivesteak.patches.pinterest.download

import app.archivesteak.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import app.archivesteak.patches.pinterest.shared.EXTENSION_PACKAGE
import app.archivesteak.patches.pinterest.shared.smaliReference
import app.archivesteak.patches.pinterest.shared.stringsPatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "$EXTENSION_PACKAGE/MenuCopyImagePatch;"
private const val ROW_TYPE = "Landroid/widget/RelativeLayout;"
private const val DOWNLOAD_ICON = "DOWNLOAD"
private const val COPY_ICON = "COPY_TO_CLIPBOARD"

@Suppress("unused")
val copyImagePatch = bytecodePatch(
    name = "Copy image",
    description = "Adds \"Copy image\" below \"Download image\" in the Pin menu.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    // Copying is done by the download row's handler, so the row must be shown.
    dependsOn(
        enableImageDownloadPatch,
        menuDownloadHookPatch,
        stringsPatch("copy_image" to "Copy image"),
    )

    execute {
        // The Pin menu views make their rows with a static factory taking the menu, a title and an icon.
        val rowFactories = mutableListOf<Method>()
        classDefForEach { classDef ->
            classDef.methods.filterTo(rowFactories) { method ->
                AccessFlags.STATIC.isSet(method.accessFlags) && method.returnType == ROW_TYPE &&
                    method.parameterTypes.map(CharSequence::toString).let {
                        it.size == 4 && it[0] == classDef.type && it[1] == "I" && it[3] == "I" &&
                            it[2].startsWith("L") && !it[2].startsWith("Ljava/") && !it[2].startsWith("Landroid/")
                    }
            }
        }
        rowFactories.retainAll { factory ->
            classDefBy(factory.parameterTypes[2].toString()).fields.map { it.name }
                .containsAll(listOf(DOWNLOAD_ICON, COPY_ICON))
        }

        // Add the copy row after the menu adds the download row.
        val hookedFactories = rowFactories.filter { factory ->
            var hooked = false
            mutableClassDefBy(factory.definingClass).methods.forEach { method ->
                val instructions = method.implementation?.instructions?.toList() ?: return@forEach
                instructions.withIndex()
                    .filter { (_, instruction) -> instruction.calls(factory) }
                    .map { it.index }
                    .reversed()
                    .forEach { callIndex ->
                        val call = instructions[callIndex]
                        val iconRegister = call.argumentRegisters[2]
                        val iconLoad = instructions.subList(0, callIndex).lastOrNull {
                            it is OneRegisterInstruction && it.registerA == iconRegister && it.opcode.setsRegister()
                        }
                        if (iconLoad?.opcode != Opcode.SGET_OBJECT ||
                            ((iconLoad as ReferenceInstruction).reference as FieldReference).name != DOWNLOAD_ICON
                        ) {
                            return@forEach
                        }

                        val result = instructions.getOrNull(callIndex + 1)
                        if (result?.opcode != Opcode.MOVE_RESULT_OBJECT) return@forEach
                        val rowRegister = (result as OneRegisterInstruction).registerA

                        val addIndex = (callIndex + 2 until instructions.size).firstOrNull { index ->
                            val instruction = instructions[index]
                            ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.let {
                                it.name == "addView" && it.parameterTypes.map(CharSequence::toString) == listOf("Landroid/view/View;")
                            } == true && instruction.argumentRegisters.getOrNull(1) == rowRegister
                        } ?: return@forEach

                        method.addInstructions(
                            addIndex + 1,
                            "invoke-static/range { v$rowRegister .. v$rowRegister }, " +
                                "$EXTENSION_CLASS->onDownloadRowAdded(Landroid/view/View;)V"
                        )
                        hooked = true
                    }
            }
            hooked
        }
        if (hookedFactories.isEmpty()) throw PatchException("Pin menu download row not found")

        // Extension stubs may have no registers besides their parameters,
        // so the unused parameter is used as a register.
        val extensionMethods = mutableClassDefBy(EXTENSION_CLASS).methods
        extensionMethods.single { it.name == "createRow" }.addInstructions(
            0,
            hookedFactories.withIndex().joinToString("\n") { (index, factory) ->
                """
                    instance-of p4, p0, ${factory.definingClass}
                    if-eqz p4, :not_menu_$index
                    check-cast p0, ${factory.definingClass}
                    check-cast p2, ${factory.parameterTypes[2]}
                    invoke-static { p0, p1, p2, p3 }, ${factory.smaliReference}
                    move-result-object p0
                    return-object p0
                    :not_menu_$index
                """
            } + """
                const/4 p0, 0x0
                return-object p0
            """
        )
        val iconType = hookedFactories.first().parameterTypes[2]
        extensionMethods.single { it.name == "getCopyIcon" }.addInstructions(
            0,
            """
                sget-object v0, $iconType->$COPY_ICON:$iconType
                return-object v0
            """
        )
    }
}

private fun Instruction.calls(method: Method) =
    (opcode == Opcode.INVOKE_STATIC || opcode == Opcode.INVOKE_STATIC_RANGE) &&
        ((this as ReferenceInstruction).reference as MethodReference).let {
            it.definingClass == method.definingClass && it.name == method.name &&
                it.parameterTypes.map(CharSequence::toString) == method.parameterTypes.map(CharSequence::toString)
        }

private val Instruction.argumentRegisters: List<Int>
    get() = when (this) {
        is RegisterRangeInstruction -> List(registerCount) { startRegister + it }
        is FiveRegisterInstruction -> listOf(registerC, registerD, registerE, registerF, registerG)
            .take(registerCount)
        else -> emptyList()
    }
