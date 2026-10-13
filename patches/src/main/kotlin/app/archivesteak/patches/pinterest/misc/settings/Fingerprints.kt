package app.archivesteak.patches.pinterest.misc.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object ApplicationOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/pinterest/ReleaseHiltApplication;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf(),
)

/** Builds the rows of the main settings screen. */
internal object MainSettingsRowsFingerprint : Fingerprint(
    filters = listOf(
        string("labs_settings"),
        string("android_creator_hub_paid_partnership_onboarding"),
    )
)

/**
 * Binds a settings row to the view showing it, and sets the view's click listener.
 * Different screens show their rows with different views.
 */
internal object SettingsRowBindFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Lcom/pinterest/feature/settings/menu/model/"),
    filters = listOf(methodCall(name = "setOnClickListener")),
    custom = { method, _ ->
        // The view sets its own listener, so the method's class is a view.
        val thisRegister = method.implementation!!.registerCount - method.parameters.size - 1
        !AccessFlags.STATIC.isSet(method.accessFlags) &&
            method.implementation!!.instructions.any { instruction ->
                instruction is FiveRegisterInstruction &&
                    instruction.registerC == thisRegister &&
                    ((instruction as ReferenceInstruction).reference as? MethodReference)?.name == "setOnClickListener"
            }
    },
)

/**
 * View type of a row of the Social permissions screen, which the patch settings are shown on,
 * or of one of its subscreens. Its class is the source of the screen's rows.
 */
internal object PermissionsRowViewTypeFingerprint : Fingerprint(
    name = "getItemViewType",
    returnType = "I",
    parameters = listOf("I"),
    filters = listOf(string("Item should be SettingsPermissionsMenuItem")),
)

/** Sets the title of the header of settings screens. */
internal object SettingsHeaderSetTitleFingerprint : Fingerprint(
    definingClass = "Lcom/pinterest/settings/SettingsRoundHeaderView;",
    returnType = "V",
    parameters = listOf("I"),
    filters = listOf(
        methodCall(parameters = listOf("Lcom/pinterest/gestalt/text/GestaltText;", "I", "[Ljava/lang/Object;")),
    ),
)
