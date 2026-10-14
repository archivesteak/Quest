package app.archivesteak.patches.gboard.misc.signature

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

/**
 * Gets the SHA-256 digest of the signing certificate of a package.
 * Its class also checks the certificate against Google's.
 */
internal object SignatureDigestFingerprint : Fingerprint(
    returnType = "[B",
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;"),
    filters = listOf(string("getSignatureSha256DigestForPackage")),
)
