package app.archivesteak.patches.pinterest.misc.settings

import app.archivesteak.patches.pinterest.shared.EXTENSION_PACKAGE
import app.archivesteak.patches.pinterest.shared.addInstructionsAtBranchTarget
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val UTILS_CLASS = "$EXTENSION_PACKAGE/shared/Utils;"
private const val SETTINGS_MENU_CLASS = "$EXTENSION_PACKAGE/settings/SettingsMenuPatch;"

internal const val SETTINGS_ROWS_PACKAGE = "Lcom/pinterest/feature/settings/menu/model/"
private const val SCREEN_LOCATION = "Lcom/pinterest/framework/screens/ScreenLocation;"

/**
 * Adds a row to the app's settings that opens the patch settings.
 * Patches with settings depend on this, and add the strings of their settings with [settingsStringsPatch].
 */
val settingsPatch = bytecodePatch {
    dependsOn(settingsStringsPatch("title" to "Pinterest Patches"))

    extendWith("extensions/pinterest.mpe")

    execute {
        // Extension code needs a context, for example to read settings.
        ApplicationOnCreateFingerprint.method.addInstructions(
            0,
            "invoke-static/range { p0 .. p0 }, $UTILS_CLASS->setContext(Landroid/content/Context;)V"
        )

        // region Add the patches row to the main settings screen.

        val rowClass = findRowClassFromTitle()
        mutableClassDefBy(SETTINGS_MENU_CLASS).methods.single { it.name == "createRow" }.addInstructions(
            0,
            """
                new-instance p1, ${rowClass.type}
                invoke-direct { p1, p0 }, ${rowClass.type}-><init>(I)V
                return-object p1
            """
        )

        MainSettingsRowsFingerprint.let {
            it.method.apply {
                val instructions = implementation!!.instructions.toList()
                val lastStringIndex = it.instructionMatches.last().index
                val returnIndex = (lastStringIndex until instructions.size)
                    .first { index -> instructions[index].opcode == Opcode.RETURN_OBJECT }
                val rowsRegister = getInstruction<OneRegisterInstruction>(returnIndex).registerA

                // The returned list must be the one the rows are added to.
                val isRowList = (lastStringIndex until returnIndex).any { index ->
                    val instruction = instructions[index]
                    instruction is FiveRegisterInstruction &&
                        instruction.registerC == rowsRegister &&
                        ((instruction as ReferenceInstruction).reference as? MethodReference)?.name == "add"
                }
                if (!isRowList) throw PatchException("Main settings rows list not found")

                addInstructionsAtBranchTarget(
                    returnIndex,
                    "invoke-static/range { v$rowsRegister .. v$rowsRegister }, " +
                        "$SETTINGS_MENU_CLASS->addSettingsRow(Ljava/util/List;)V"
                )
            }
        }

        // Open the patch settings when the row is tapped.
        SettingsRowBindFingerprint.matchAll().forEach { match ->
            match.method.addInstructions(
                0,
                "invoke-static/range { p0 .. p1 }, $SETTINGS_MENU_CLASS->onBindRow(Landroid/view/View;Ljava/lang/Object;)V"
            )
        }

        // endregion

        showSettingsOnPermissionsScreen()
    }
}

/**
 * The settings row class that opens another screen and is made from just a title resource,
 * such as "Refine your recommendations".
 */
private fun BytecodePatchContext.findRowClassFromTitle(): ClassDef {
    val rowClasses = mutableListOf<ClassDef>()
    classDefForEach { classDef ->
        if (classDef.type.startsWith(SETTINGS_ROWS_PACKAGE) &&
            !AccessFlags.ABSTRACT.isSet(classDef.accessFlags) &&
            classDef.methods.any { it.name == "<init>" && it.parameterTypes.map(CharSequence::toString) == listOf("I") } &&
            classDef.methods.any { it.parameters.isEmpty() && it.returnType == SCREEN_LOCATION }
        ) {
            rowClasses += classDef
        }
    }
    return rowClasses.singleOrNull()
        ?: throw PatchException("Expected one settings row class made from a title, found ${rowClasses.map { it.type }}")
}
