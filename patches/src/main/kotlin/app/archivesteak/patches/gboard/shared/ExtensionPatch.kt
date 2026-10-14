package app.archivesteak.patches.gboard.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.Method

private const val APPLICATION_CLASS = "Lcom/google/android/apps/inputmethod/latin/LatinApp;"
private const val UTILS_CLASS = "$EXTENSION_PACKAGE/shared/Utils;"

/**
 * Merges the extension into the app, and gives it the app's context when the app starts.
 * Patches that call the extension depend on this.
 */
internal val sharedExtensionPatch = bytecodePatch {
    extendWith("extensions/gboard.mpe")

    execute {
        // The application class inherits onCreate from an obfuscated superclass.
        val onCreateClass = generateSequence(classDefBy(APPLICATION_CLASS)) { classDef ->
            classDef.superclass?.let { classDefByOrNull(it) }
        }.firstOrNull { classDef -> classDef.methods.any { it.isOnCreate() } }
            ?: throw PatchException("Application onCreate not found")

        mutableClassDefBy(onCreateClass).methods.first { it.isOnCreate() }.addInstructions(
            0,
            "invoke-static/range { p0 .. p0 }, $UTILS_CLASS->setContext(Landroid/content/Context;)V"
        )
    }
}

private fun Method.isOnCreate() = name == "onCreate" && parameters.isEmpty() && returnType == "V"
