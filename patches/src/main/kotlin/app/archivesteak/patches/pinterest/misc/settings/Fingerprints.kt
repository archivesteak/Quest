package app.archivesteak.patches.pinterest.misc.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

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

/** Binds a settings row to the view showing it. */
internal object SettingsRowBindFingerprint : Fingerprint(
    definingClass = "Lcom/pinterest/feature/settings/shared/view/SettingsListActionItemView;",
    returnType = "V",
    parameters = listOf("Lcom/pinterest/feature/settings/menu/model/"),
    filters = listOf(methodCall(name = "setOnClickListener")),
)
