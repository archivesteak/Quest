package app.archivesteak.patches.pinterest.share

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/** Opens the app's share menu for a Pin, board, profile or other model, which is the second parameter. */
internal object OpenShareMenuFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(
        "L",
        "L",
        "I",
        "Lcom/pinterest/sendshare/model/SendableObject;",
        "Landroid/net/Uri;",
        "Lcom/pinterest/feature/sharesheet/model/MultiPinSharingPayload;",
        "Z",
        "Lcom/pinterest/feature/sharesheet/model/GreetingIntentData;",
        "I",
    ),
    custom = { method, _ -> AccessFlags.STATIC.isSet(method.accessFlags) },
)
