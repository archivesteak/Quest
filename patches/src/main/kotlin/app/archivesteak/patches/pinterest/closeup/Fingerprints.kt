package app.archivesteak.patches.pinterest.closeup

import app.morphe.patcher.Fingerprint

/** Binds the Pin page module that shows the Pin's creator to a Pin. */
internal object CreatorModuleUpdatePinFingerprint : Fingerprint(
    name = "updatePin",
    returnType = "V",
    parameters = listOf("Lcom/pinterest/api/model/"),
    custom = { _, classDef ->
        classDef.fields.any { it.type == "Lcom/pinterest/activity/pin/view/PinCloseupCreatorAttributionView;" }
    },
)
