package app.archivesteak.patches.gboard.misc.settings

import app.archivesteak.patches.gboard.misc.signature.signatureCheckPatch
import app.archivesteak.patches.gboard.shared.EXTENSION_PACKAGE
import app.archivesteak.patches.gboard.shared.sharedExtensionPatch
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val FRAGMENT_CLASS = "$EXTENSION_PACKAGE/settings/QuestSettingsFragment;"
private const val COMMON_PREFERENCE_FRAGMENT =
    "Lcom/google/android/libraries/inputmethod/preferencewidgets/CommonPreferenceFragment;"

/**
 * Adds Quest to Gboard's settings, a screen with the settings of the applied patches, shown with
 * Gboard's own preference widgets. Patches with settings depend on this and add them with [preferencesPatch].
 */
val settingsPatch = bytecodePatch {
    dependsOn(signatureCheckPatch, sharedExtensionPatch, settingsResourcePatch)

    execute {
        // The Quest screen overrides the method screens return the resource of their preferences from.
        mutableClassDefBy(FRAGMENT_CLASS).methods.single { it.name == "getPreferencesResource" }.name =
            preferencesResourceMethodName()
    }
}

/**
 * The method of the base class of settings screens returning the resource of a screen's preferences.
 * The base class passes its result to the method loading preferences from a resource into a group.
 */
private fun BytecodePatchContext.preferencesResourceMethodName(): String {
    val fragmentClass = classDefBy(COMMON_PREFERENCE_FRAGMENT)
    val names = fragmentClass.methods.flatMap { method ->
        val instructions = method.implementation?.instructions?.toList() ?: return@flatMap emptyList()
        instructions.indices.mapNotNull { index ->
            val reference = (instructions[index] as? ReferenceInstruction)?.reference as? MethodReference
            if (instructions[index].opcode != Opcode.INVOKE_VIRTUAL ||
                reference?.definingClass != fragmentClass.type ||
                reference.returnType != "I" ||
                reference.parameterTypes.isNotEmpty()
            ) {
                return@mapNotNull null
            }

            val result = instructions.getOrNull(index + 1)
            if (result?.opcode != Opcode.MOVE_RESULT) return@mapNotNull null
            val resultRegister = (result as OneRegisterInstruction).registerA

            val loadsResource = instructions.drop(index + 2).take(4).any { instruction ->
                instruction is FiveRegisterInstruction && instruction.registerD == resultRegister &&
                    ((instruction as ReferenceInstruction).reference as? MethodReference)
                        ?.parameterTypes?.map(CharSequence::toString) == listOf("I", "Landroidx/preference/PreferenceGroup;")
            }
            if (loadsResource) reference.name else null
        }
    }.distinct()

    return names.singleOrNull()
        ?: throw PatchException("Expected one method returning the preferences resource, found $names")
}
