package app.archivesteak.patches.pinterest.misc.settings

import app.archivesteak.patches.pinterest.shared.EXTENSION_PACKAGE
import app.archivesteak.patches.pinterest.shared.smaliReference
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val SETTINGS_SCREEN_CLASS = "$EXTENSION_PACKAGE/settings/SettingsScreenPatch;"
private const val TOGGLE_ROW_VIEW = "Lcom/pinterest/feature/settings/shared/view/SettingsToggleItemView;"

/**
 * Shows the patch settings on the Social permissions screen when the patches row opens it,
 * so they show with the app's own header, rows and switches.
 */
internal fun BytecodePatchContext.showSettingsOnPermissionsScreen() {
    // region Show the patch title in the screen's header.

    SettingsHeaderSetTitleFingerprint.method.addInstructions(
        0,
        """
            invoke-static/range { p1 .. p1 }, $SETTINGS_SCREEN_CLASS->getScreenTitle(I)I
            move-result p1
        """
    )

    // endregion

    // region Show the patch settings as the screen's rows.

    // Social permissions and its subscreens have their own row sources.
    val rowSources = PermissionsRowViewTypeFingerprint.matchAll()
    rowSources.forEach { match ->
        val rowSourceClass = match.classDef
        rowSourceClass.methods.filter { it.name == "<init>" }.forEach { constructor ->
            // The register holding this can be reused later, so the source is passed on right after
            // the superclass constructor. Constructors calling another constructor of the class are skipped.
            val superCallIndex = constructor.implementation!!.instructions.indexOfFirst { instruction ->
                instruction.opcode == Opcode.INVOKE_DIRECT &&
                    ((instruction as ReferenceInstruction).reference as MethodReference).let {
                        it.name == "<init>" && it.definingClass == rowSourceClass.superclass
                    }
            }
            if (superCallIndex < 0) return@forEach

            val thisRegister = constructor.getInstruction<FiveRegisterInstruction>(superCallIndex).registerC
            constructor.addInstructions(
                superCallIndex + 1,
                "invoke-static/range { v$thisRegister .. v$thisRegister }, " +
                    "$SETTINGS_SCREEN_CLASS->onRowSourceCreated(Ljava/lang/Object;)V"
            )
        }

        // The screens only expect their own rows, and have no view type for others.
        match.method.apply {
            requireLocalRegister()
            addInstructionsWithLabels(
                0,
                """
                    invoke-static/range { p0 .. p1 }, $SETTINGS_SCREEN_CLASS->getRowViewType(Ljava/lang/Object;I)I
                    move-result v0
                    if-ltz v0, :screen_view_type
                    return v0
                """,
                ExternalLabel("screen_view_type", getInstruction(0))
            )
        }
    }

    val rowSourceBaseClass = rowSources.map { it.originalClassDef.superclass!! }.distinct().singleOrNull()
        ?.let { mutableClassDefBy(it) }
        ?: throw PatchException("Settings row sources have different base classes")
    rowSourceBaseClass.methods.singleOrNull { it.name == "loadData" && it.parameters.isEmpty() }?.apply {
        requireLocalRegister()
        addInstructionsWithLabels(
            0,
            """
                invoke-static/range { p0 .. p0 }, $SETTINGS_SCREEN_CLASS->loadRows(Ljava/lang/Object;)Z
                move-result v0
                if-eqz v0, :load_screen_rows
                return-void
            """,
            ExternalLabel("load_screen_rows", getInstruction(0))
        )
    } ?: throw PatchException("Settings row loading not found")

    val setRowsMethod = rowSourceBaseClass.methods.singleOrNull {
        !AccessFlags.STATIC.isSet(it.accessFlags) &&
            it.returnType == "V" &&
            it.parameterTypes.map(CharSequence::toString) == listOf("Ljava/util/List;")
    } ?: throw PatchException("Settings rows setter not found")

    // endregion

    // region Make the rows.

    // The toggle row view shows rows of the base class of toggle rows.
    val toggleRowBaseClass = classDefBy(TOGGLE_ROW_VIEW).fields
        .singleOrNull { it.type.startsWith(SETTINGS_ROWS_PACKAGE) }
        ?.let { classDefBy(it.type) }
        ?: throw PatchException("Toggle row class not found")
    val rowBaseClass = toggleRowBaseClass.superclass!!
    val checkedField = toggleRowBaseClass.checkedField()

    // A toggle row made from a title, a description, whether it's enabled and whether it's checked,
    // and a header row made from a title.
    val toggleRows = mutableListOf<Pair<ClassDef, Method>>()
    val headerRows = mutableListOf<Pair<ClassDef, Method>>()
    classDefForEach { classDef ->
        if (!classDef.type.startsWith(SETTINGS_ROWS_PACKAGE) || AccessFlags.ABSTRACT.isSet(classDef.accessFlags)) {
            return@classDefForEach
        }

        when (classDef.superclass) {
            toggleRowBaseClass.type -> classDef.constructorWith { types ->
                types.size == 4 && types[0] == "I" && types[1].startsWith(SETTINGS_ROWS_PACKAGE) &&
                    types[2] == "Z" && types[3] == "Z"
            }?.let { toggleRows += classDef to it }

            rowBaseClass -> if (classDef.returnsViewType(0)) {
                classDef.constructorWith { it == listOf("I") }?.let { headerRows += classDef to it }
            }
        }
    }
    val (toggleRowClass, toggleRowConstructor) = toggleRows.singleOrNull()
        ?: throw PatchException("Expected one toggle row class, found ${toggleRows.map { it.first.type }}")
    val (headerRowClass, headerRowConstructor) = headerRows.singleOrNull()
        ?: throw PatchException("Expected one header row class, found ${headerRows.map { it.first.type }}")

    val descriptionType = toggleRowConstructor.parameterTypes[1].toString()
    val descriptionConstructor = classDefBy(descriptionType)
        .constructorWith { it == listOf("I", "Ljava/lang/Integer;", "Ljava/lang/String;") }
        ?: throw PatchException("Row description constructor not found")

    // Extension stubs may have no registers besides their parameters,
    // so unused parameters are used as registers.
    val extensionMethods = mutableClassDefBy(SETTINGS_SCREEN_CLASS).methods
    fun implement(name: String, smali: String) =
        extensionMethods.single { it.name == name }.addInstructions(0, smali)

    implement(
        "setRows",
        """
            check-cast p0, ${rowSourceBaseClass.type}
            invoke-virtual { p0, p1 }, ${setRowsMethod.smaliReference}
            return-void
        """
    )
    implement(
        "createHeaderRow",
        """
            new-instance p1, ${headerRowClass.type}
            invoke-direct { p1, p0 }, ${headerRowConstructor.smaliReference}
            return-object p1
        """
    )
    // The description constructor takes a mask of default arguments, and the toggle row
    // constructor takes whether the row is enabled before whether it's checked.
    implement(
        "createToggleRow",
        """
            new-instance p3, $descriptionType
            const/4 p4, 0x0
            const/4 p5, 0x0
            invoke-direct { p3, p4, p5, p1 }, ${descriptionConstructor.smaliReference}
            new-instance p5, ${toggleRowClass.type}
            const/4 p4, 0x1
            invoke-direct { p5, p0, p3, p4, p2 }, ${toggleRowConstructor.smaliReference}
            return-object p5
        """
    )
    implement(
        "isChecked",
        """
            check-cast p0, ${toggleRowBaseClass.type}
            iget-boolean p0, p0, ${toggleRowBaseClass.type}->${checkedField.name}:Z
            return p0
        """
    )

    // endregion

    // region Save a setting when its switch is toggled.

    // The row views set whether their row is checked before telling the screen.
    val checkedWriters = mutableListOf<Pair<ClassDef, Method>>()
    classDefForEach { classDef ->
        if (classDef.type == toggleRowBaseClass.type) return@classDefForEach

        classDef.methods.forEach { method ->
            if (method.implementation?.instructions?.any { it.writesField(checkedField) } == true) {
                checkedWriters += classDef to method
            }
        }
    }
    if (checkedWriters.isEmpty()) throw PatchException("Toggle row switches not found")

    checkedWriters.forEach { (classDef, method) ->
        mutableClassDefBy(classDef).methods.first { it.hasSignatureOf(method) }.apply {
            implementation!!.instructions.withIndex()
                .filter { (_, instruction) -> instruction.writesField(checkedField) }
                .map { it.index }
                .reversed()
                .forEach { index ->
                    val rowRegister = getInstruction<TwoRegisterInstruction>(index).registerB
                    addInstructions(
                        index + 1,
                        "invoke-static/range { v$rowRegister .. v$rowRegister }, " +
                            "$SETTINGS_SCREEN_CLASS->onRowChanged(Ljava/lang/Object;)V"
                    )
                }
        }
    }

    // endregion
}

/** The field holding whether a toggle row is checked, which the constructor sets from its first boolean. */
private fun ClassDef.checkedField(): Field {
    val constructor = constructorWith {
        it == listOf("Ljava/lang/Integer;", "Z", "Ljava/lang/String;", "Z")
    } ?: throw PatchException("Toggle row constructor not found")

    // Parameter registers are the last ones, starting with this.
    val checkedRegister = constructor.implementation!!.registerCount - 3
    val fieldName = constructor.implementation!!.instructions.firstNotNullOfOrNull { instruction ->
        if (instruction.opcode == Opcode.IPUT_BOOLEAN && (instruction as TwoRegisterInstruction).registerA == checkedRegister) {
            ((instruction as ReferenceInstruction).reference as FieldReference).name
        } else {
            null
        }
    } ?: throw PatchException("Toggle row checked field not found")

    return fields.single { it.name == fieldName }
}

private fun ClassDef.constructorWith(parameters: (List<String>) -> Boolean) = methods.firstOrNull {
    it.name == "<init>" && parameters(it.parameterTypes.map(CharSequence::toString))
}

/** Whether the class's getViewType method returns the given constant. */
private fun ClassDef.returnsViewType(viewType: Int) = methods.any { method ->
    val instructions = method.implementation?.instructions?.toList() ?: return@any false
    method.name == "getViewType" && method.parameters.isEmpty() &&
        instructions.size == 2 &&
        instructions[0].opcode == Opcode.CONST_4 &&
        (instructions[0] as NarrowLiteralInstruction).narrowLiteral == viewType &&
        instructions[1].opcode == Opcode.RETURN
}

private fun Instruction.writesField(field: Field) = opcode == Opcode.IPUT_BOOLEAN &&
    ((this as ReferenceInstruction).reference as FieldReference).let {
        it.definingClass == field.definingClass && it.name == field.name
    }

private fun Method.hasSignatureOf(other: Method) = name == other.name &&
    returnType == other.returnType &&
    parameterTypes.map(CharSequence::toString) == other.parameterTypes.map(CharSequence::toString)

/** Throws if v0 is a parameter, so code added at the method's start can't use it. */
private fun Method.requireLocalRegister() {
    if (implementation!!.registerCount <= parameters.size + 1) {
        throw PatchException("$definingClass->$name has no register to use")
    }
}
