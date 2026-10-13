package app.archivesteak.patches.pinterest.menu

import app.archivesteak.patches.pinterest.misc.settings.settingsPatch
import app.archivesteak.patches.pinterest.misc.settings.settingsStringsPatch
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
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "$EXTENSION_PACKAGE/LongPressMenuPatch;"
private const val MENU_ITEM_VIEW = "Lcom/pinterest/ui/menu/ContextMenuItemView;"
private const val DOWNLOAD_ICON = "DOWNLOAD"
private const val COPY_ICON = "COPY_TO_CLIPBOARD"

@Suppress("unused")
val longPressMenuPatch = bytecodePatch(
    name = "Customize long-press menu",
    description = "Adds Download and Copy image to the menu shown when holding a Pin, " +
        "and settings to choose which items the menu shows.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(
        settingsPatch,
        // Keys of the app's items end with the app's name for them.
        settingsStringsPatch(
            "category_long_press_menu" to "Long-press menu",
            "long_press_download_title" to "Download",
            "long_press_download_summary" to "Saves the image, or the video of video Pins.",
            "long_press_copy_image_title" to "Copy image",
            "long_press_copy_image_summary" to "Copies the image to the clipboard.",
            "long_press_REPIN_title" to "Save",
            "long_press_SEND_title" to "Send",
            "long_press_share_app_title" to "Send with app",
            "long_press_share_app_summary" to "Sends with the app you last shared with.",
            "long_press_SEE_MORE_title" to "See more like this",
            "long_press_HIDE_title" to "See less",
            "long_press_VISUAL_SEARCH_title" to "Search image",
            "long_press_EDIT_title" to "Edit",
            "long_press_EDIT_summary" to "Only on your own Pins.",
            "long_press_REORDER_title" to "Reorder",
            "long_press_REORDER_summary" to "Only on your boards.",
        ),
        pinModelPatch,
    )

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

        // The builder styles every item it makes, which sets the item's colors for each theme.
        val styleItemMethods = PinLongPressMenuBuilderFingerprint.originalMethod.implementation!!.instructions
            .filter { it.opcode == Opcode.INVOKE_STATIC }
            .map { (it as ReferenceInstruction).reference as MethodReference }
            .filter { it.returnType == "V" && it.parameterTypes.map(CharSequence::toString) == listOf(MENU_ITEM_VIEW) }
            .map { "${it.definingClass}->${it.name}(${MENU_ITEM_VIEW})V" }
            .distinct()
        val styleItemMethod = styleItemMethods.singleOrNull()
            ?: throw PatchException("Expected one long-press menu item style method, found $styleItemMethods")

        val itemFactory = ContextMenuItemFactoryFingerprint.originalMethod
        val itemModelType = itemFactory.parameterTypes[1].toString()
        val itemModelConstructor = classDefBy(itemModelType).methods.single {
            it.name == "<init>" && it.parameterTypes.size == 4
        }
        val iconType = itemModelConstructor.parameterTypes[0].toString()
        val iconNames = classDefBy(iconType).fields.map { it.name }
        listOf(DOWNLOAD_ICON, COPY_ICON).forEach { icon ->
            if (icon !in iconNames) throw PatchException("Icon $icon not found")
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
        extensionMethods.single { it.name == "getDownloadIcon" }.addInstructions(
            0,
            """
                sget-object v0, $iconType->$DOWNLOAD_ICON:$iconType
                return-object v0
            """
        )
        extensionMethods.single { it.name == "getCopyIcon" }.addInstructions(
            0,
            """
                sget-object v0, $iconType->$COPY_ICON:$iconType
                return-object v0
            """
        )
        // Extension stubs may have no registers besides their parameters.
        // The action parameter (p3) is null and the item parameter (p4) holds the item.
        extensionMethods.single { it.name == "createMenuItem" }.addInstructions(
            0,
            """
                new-instance p4, $itemModelType
                check-cast p2, $iconType
                invoke-direct { p4, p2, p1, p1, p3 }, $itemModelType-><init>(${itemModelConstructor.parameterTypes.joinToString("")})V
                invoke-static { p0, p4 }, ${itemFactory.definingClass}->${itemFactory.name}(Landroid/content/Context;$itemModelType)$MENU_ITEM_VIEW
                move-result-object p4
                invoke-static { p4 }, $styleItemMethod
                return-object p4
            """
        )

        // Items are tagged with the id of their kind, such as context_share_recent.
        val itemKindField = classDefBy(MENU_ITEM_VIEW).itemKindField()
        extensionMethods.single { it.name == "getMenuItemKind" }.addInstructions(
            0,
            """
                check-cast p0, $MENU_ITEM_VIEW
                iget p0, p0, $MENU_ITEM_VIEW->$itemKindField:I
                return p0
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

/** The field tagging a menu item with its kind, the only number field the item view starts as -1. */
private fun ClassDef.itemKindField(): String {
    val fieldNames = methods.filter { it.name == "<init>" }.flatMap { constructor ->
        val instructions = constructor.implementation?.instructions?.toList().orEmpty()
        instructions.withIndex().mapNotNull { (index, instruction) ->
            if (instruction.opcode != Opcode.IPUT) return@mapNotNull null

            val field = (instruction as ReferenceInstruction).reference as FieldReference
            val valueRegister = (instruction as TwoRegisterInstruction).registerA
            val value = instructions.subList(0, index).lastOrNull {
                it is NarrowLiteralInstruction && (it as OneRegisterInstruction).registerA == valueRegister
            } as? NarrowLiteralInstruction
            field.name.takeIf { field.definingClass == type && field.type == "I" && value?.narrowLiteral == -1 }
        }
    }.distinct()

    return fieldNames.singleOrNull()
        ?: throw PatchException("Expected one long-press menu item kind field, found $fieldNames")
}
