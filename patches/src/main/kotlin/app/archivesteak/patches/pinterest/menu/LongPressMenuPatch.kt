package app.archivesteak.patches.pinterest.menu

import app.archivesteak.patches.pinterest.misc.settings.settingsPatch
import app.archivesteak.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import app.archivesteak.patches.pinterest.shared.EXTENSION_PACKAGE
import app.archivesteak.patches.pinterest.shared.addInstructionsAtBranchTarget
import app.archivesteak.patches.pinterest.shared.pinClass
import app.archivesteak.patches.pinterest.shared.pinModelPatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "$EXTENSION_PACKAGE/LongPressMenuPatch;"

@Suppress("unused")
val longPressMenuPatch = bytecodePatch(
    name = "Customize long-press menu",
    description = "Adds Download to the menu shown when holding a Pin, " +
        "and settings to choose which items the menu shows.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(settingsPatch, pinModelPatch)

    execute {
        // region Remove the menu items turned off in settings.

        PinLongPressMenuBuilderFingerprint.let {
            it.method.apply {
                val instructions = implementation!!.instructions.toList()
                val experimentCheckIndex = it.instructionMatches.first().index

                // The item types come from the last call returning a list before the experiment check.
                val itemsCallIndex = (experimentCheckIndex downTo 0).first { index ->
                    ((instructions[index] as? ReferenceInstruction)?.reference as? MethodReference)
                        ?.returnType == "Ljava/util/ArrayList;"
                }
                val itemsResultIndex = itemsCallIndex + 1
                if (instructions[itemsResultIndex].opcode != Opcode.MOVE_RESULT_OBJECT) {
                    throw PatchException("Long-press menu item types not found")
                }
                val itemsRegister = getInstruction<OneRegisterInstruction>(itemsResultIndex).registerA

                addInstructionsAtBranchTarget(
                    itemsResultIndex + 1,
                    """
                        invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->onBuildPinMenu(Ljava/lang/Object;)V
                        invoke-static/range { v$itemsRegister .. v$itemsRegister }, $EXTENSION_CLASS->filterMenuItems(Ljava/util/List;)V
                    """
                )
            }
        }

        // endregion

        // region Add a download item.

        val menuBuilderClass = PinLongPressMenuBuilderFingerprint.originalClassDef
        val menuPinField = menuBuilderClass.fields.single { it.type == pinClass.type }

        val itemFactory = ContextMenuItemFactoryFingerprint.originalMethod
        val itemModelType = itemFactory.parameterTypes[1].toString()
        val itemModelConstructor = classDefBy(itemModelType).methods.single {
            it.name == "<init>" && it.parameterTypes.size == 4
        }
        val iconType = itemModelConstructor.parameterTypes[0].toString()
        if (classDefBy(iconType).fields.none { it.name == "DOWNLOAD" }) {
            throw PatchException("Download icon not found")
        }

        val extensionMethods = mutableClassDefBy(EXTENSION_CLASS).methods
        extensionMethods.single { it.name == "getMenuPin" }.addInstructions(
            0,
            """
                check-cast p0, ${menuBuilderClass.type}
                iget-object p0, p0, ${menuBuilderClass.type}->${menuPinField.name}:${menuPinField.type}
                return-object p0
            """
        )
        // Extension stubs may have no registers besides their parameters. The icon parameter (p2)
        // holds the icon, the action parameter (p3) is null and the item parameter (p4) holds the item.
        extensionMethods.single { it.name == "createMenuItem" }.addInstructions(
            0,
            """
                new-instance p4, $itemModelType
                sget-object p2, $iconType->DOWNLOAD:$iconType
                invoke-direct { p4, p2, p1, p1, p3 }, $itemModelType-><init>(${itemModelConstructor.parameterTypes.joinToString("")})V
                invoke-static { p0, p4 }, ${itemFactory.definingClass}->${itemFactory.name}(Landroid/content/Context;$itemModelType)Lcom/pinterest/ui/menu/ContextMenuItemView;
                move-result-object p4
                return-object p4
            """
        )

        ContextMenuSetItemsFingerprint.method.addInstructions(
            0,
            """
                invoke-static/range { p0 .. p1 }, $EXTENSION_CLASS->addMenuItems(Landroid/view/View;Ljava/util/List;)Ljava/util/List;
                move-result-object p1
            """
        )

        // endregion
    }
}
