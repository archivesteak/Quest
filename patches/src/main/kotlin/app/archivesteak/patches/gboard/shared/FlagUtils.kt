package app.archivesteak.patches.gboard.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

/*
 * Gboard's behaviour is driven by server flags. A static initializer creates each flag from its name and
 * default value and stores it in a static field. Code reads a flag's value with a getter returning Object,
 * which returns the value sent by the server, or the default.
 */

/** How many instructions after loading a flag its getter is called. */
private const val MAX_GETTER_DISTANCE = 3

/**
 * Passes the value of each flag in [hooks] through a method of the extension wherever it's read,
 * so the extension can override it, including values sent by the server.
 *
 * @param hooks Flag names, mapped to static methods that take the flag's value and return the value to use,
 * as smali references taking and returning `Ljava/lang/Object;`.
 * @param unusedFlags Flags of [hooks] the app may not read. Other flags must be read.
 */
fun BytecodePatchContext.hookFlagReads(hooks: Map<String, String>, unusedFlags: Set<String> = emptySet()) {
    val fieldHooks = hooks.map { (flagName, hook) -> flagField(flagName) to hook }
    fun hookOf(instruction: Instruction) = fieldHooks.firstOrNull { (field, _) -> instruction.readsField(field) }

    val readers = mutableListOf<Pair<ClassDef, Method>>()
    classDefForEach { classDef ->
        classDef.methods.forEach { method ->
            if (method.implementation?.instructions?.any { hookOf(it) != null } == true) {
                readers += classDef to method
            }
        }
    }

    val hookedFields = mutableSetOf<FieldReference>()
    readers.forEach { (classDef, method) ->
        mutableClassDefBy(classDef).methods.first { it.hasSignatureOf(method) }.apply {
            val instructions = implementation!!.instructions.toList()
            instructions.indices.reversed().forEach { readIndex ->
                val (field, hook) = hookOf(instructions[readIndex]) ?: return@forEach
                val flagRegister = (instructions[readIndex] as OneRegisterInstruction).registerA

                val getterIndex = (readIndex + 1..readIndex + MAX_GETTER_DISTANCE).firstOrNull { index ->
                    instructions.getOrNull(index)?.isGetterCall(flagRegister) == true
                }
                val moveResult = getterIndex?.let { instructions.getOrNull(it + 1) }
                if (moveResult?.opcode != Opcode.MOVE_RESULT_OBJECT) {
                    throw PatchException("Flag ${field.name} is read differently in $definingClass->$name")
                }
                val valueRegister = (moveResult as OneRegisterInstruction).registerA

                addInstructions(
                    getterIndex + 2,
                    """
                        invoke-static/range { v$valueRegister .. v$valueRegister }, $hook
                        move-result-object v$valueRegister
                    """
                )
                hookedFields += field
            }
        }
    }

    val unreadFlags = hooks.keys.zip(fieldHooks)
        .filter { (flagName, fieldHook) -> flagName !in unusedFlags && fieldHook.first !in hookedFields }
        .map { it.first }
    if (unreadFlags.isNotEmpty()) throw PatchException("Flags not read: $unreadFlags")
}

/** The static field a flag is stored in, found from the flag's name. */
private fun BytecodePatchContext.flagField(flagName: String): FieldReference {
    val fields = classDefByStrings(flagName, StringComparisonType.EQUALS).flatMap { classDef ->
        classDef.methods.filter { it.name == "<clinit>" }.mapNotNull { initializer ->
            val instructions = initializer.implementation?.instructions?.toList() ?: return@mapNotNull null
            val nameIndex = instructions.indexOfFirst {
                ((it as? ReferenceInstruction)?.reference as? StringReference)?.string == flagName
            }
            if (nameIndex < 0) return@mapNotNull null

            instructions.drop(nameIndex).firstOrNull { it.opcode == Opcode.SPUT_OBJECT }
                ?.let { (it as ReferenceInstruction).reference as FieldReference }
        }
    }.distinct()

    return fields.singleOrNull() ?: throw PatchException("Expected one field for flag $flagName, found $fields")
}

private fun Instruction.readsField(field: FieldReference) = opcode == Opcode.SGET_OBJECT &&
    ((this as ReferenceInstruction).reference as FieldReference).let {
        it.definingClass == field.definingClass && it.name == field.name && it.type == field.type
    }

/** Whether this calls a getter without parameters returning Object on the object in [register]. */
private fun Instruction.isGetterCall(register: Int): Boolean {
    val method = (this as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    val firstRegister = when (this) {
        is FiveRegisterInstruction -> registerC
        is RegisterRangeInstruction -> startRegister
        else -> return false
    }
    return firstRegister == register && method.parameterTypes.isEmpty() && method.returnType == "Ljava/lang/Object;"
}

private fun Method.hasSignatureOf(other: Method) = name == other.name &&
    returnType == other.returnType &&
    parameterTypes.map(CharSequence::toString) == other.parameterTypes.map(CharSequence::toString)
