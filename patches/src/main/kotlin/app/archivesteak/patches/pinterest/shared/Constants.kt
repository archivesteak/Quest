package app.archivesteak.patches.pinterest.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

internal const val EXTENSION_PACKAGE = "Lapp/archivesteak/extension/pinterest"

object Constants {
    val COMPATIBILITY_PINTEREST = Compatibility(
        name = "Pinterest",
        packageName = "com.pinterest",
        // Universal APK (all ABIs, nodpi) from APKMirror.
        apkFileType = ApkFileType.APK,
        appIconColor = 0xE60023,
        // SHA-256 of Pinterest's signing certificate (CN=Carl Rice, O=Pinterest Inc).
        signatures = setOf(
            "341d6881b1ecf38361fbf8c8fbae0aa516b45375c39ef5e78b161869acc1bcfa"
        ),
        targets = listOf(
            AppTarget(
                version = "14.39.0"
            )
        )
    )
}
