package app.archivesteak.patches.pinterest.closeup

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

/**
 * `updateView()` of the Pin closeup module that shows the title and description.
 * After building the description it reads its description view field and wraps the
 * text for it, which the second and third filters match.
 */
internal object PinDescriptionUpdateViewFingerprint : Fingerprint(
    name = "updateView",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        string(" Instagram  "),
        fieldAccess(definingClass = "this", opcode = Opcode.IGET_OBJECT),
        methodCall(
            name = "<init>",
            parameters = listOf("Ljava/lang/CharSequence;"),
            returnType = "V",
            location = MatchAfterWithin(6),
        ),
    )
)
