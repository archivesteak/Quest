package app.archivesteak.patches.gboard.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

internal const val EXTENSION_PACKAGE = "Lapp/archivesteak/extension/gboard"

object Constants {
    val COMPATIBILITY_GBOARD = Compatibility(
        name = "Gboard",
        packageName = "com.google.android.inputmethod.latin",
        // arm64-v8a, nodpi APK from APKMirror.
        apkFileType = ApkFileType.APK,
        appIconColor = 0x4285F4,
        // SHA-256 of Gboard's signing certificates: Google's key, and the key it was rotated to for Android 13+.
        signatures = setOf(
            "f0fd6c5b410f25cb25c3b53346c8972fae30f8ee7411df910480ad6b2d60db83",
            "7ce83c1b71f3d572fed04c8d40c5cb10ff75e6d87d9df6fbd53f0468c2905053",
        ),
        // Gboard's version names end with the build type and ABI.
        targets = listOf(
            AppTarget(
                version = "18.4.1.985164140-release-arm64-v8a"
            )
        )
    )
}
