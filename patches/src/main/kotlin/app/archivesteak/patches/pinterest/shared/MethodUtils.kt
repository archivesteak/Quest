package app.archivesteak.patches.pinterest.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.toInstructions

/**
 * Adds [smaliInstructions] before the instruction at [index], so that branches to that
 * instruction also run them. A plain insert leaves branch targets on the original
 * instruction, skipping the new code.
 *
 * The instructions must not use labels.
 */
fun MutableMethod.addInstructionsAtBranchTarget(index: Int, smaliInstructions: String) {
    val original = getInstruction(index)
    val instructions = smaliInstructions.toInstructions(this)

    // The replaced instruction keeps the branch targets.
    replaceInstruction(index, instructions.first())
    addInstructions(index + 1, instructions.drop(1) + original)
}
