# Quest

Morphe patches for several apps in one bundle (currently Pinterest and Gboard). The layout follows
[crimera/piko](https://github.com/crimera/piko), which does the same for X and Instagram.

## Layout

```
patches/src/main/kotlin/app/archivesteak/patches/
  <app>/shared/Constants.kt          COMPATIBILITY_<APP>: package, signatures, target versions
  <app>/<feature>/<Name>Patch.kt     one patch per file, grouped by feature (download, clipboard, misc/...)
  <app>/<feature>/Fingerprints.kt    fingerprints used by that feature
  shared/                            code used by more than one app (create it when a second app needs something)
extensions/<app>/src/main/java/app/archivesteak/extension/<app>/
                                     Java merged into the app, called from patches (built to extensions/<app>.mpe)
extensions/<app>/stub/               compile-only stubs of app classes the extension uses (never merged into the app)
```

- An extension module is added once a patch of the app needs Java code.
- Stubs live in a nested `stub` project (`java-library`), added with `compileOnly(project(":extensions:<app>:stub"))`.
  The Morphe plugin skips projects whose parent is already an extension, so stubs aren't merged into the app.
  Only stub classes whose names aren't obfuscated, and don't declare obfuscated members.
- Helpers under `pinterest/shared/` (MethodUtils, ModelUtils, StringsPatch) are Pinterest-only for now.
  Move one to `patches/shared/` when another app needs it, rather than copying it.
- Each patch calls `compatibleWith(COMPATIBILITY_<APP>)`. Top-level patch vals are public and `@Suppress("unused")`.

## Writing patches

- Never hard-code obfuscated class, method or field names, because they change every release. Find code by
  strings, flag names, JSON-name annotations, types and instruction shapes, and resolve other classes from
  what was found. Throw `PatchException` with a clear message when an expected shape isn't found,
  so app updates fail loudly instead of patching the wrong thing.
- Fingerprints are `internal object ...Fingerprint : Fingerprint(...)` with a KDoc saying what the method does.
- Patch descriptions are one user-facing sentence. Keep `README.md`'s patch tables in sync with them.
- Extension stubs that patches implement with smali can have no registers besides their parameters,
  so implementations only use parameter registers.

## App notes

### Pinterest
- Model classes are obfuscated, but every field keeps its JSON name in an annotation (`ModelUtils.kt`).
- Patch settings are shown on a settings screen of the app (`misc/settings`). Patches with settings depend on
  `settingsPatch` and add their strings with `settingsStringsPatch`.

### Gboard
- On start, Gboard checks it's signed by Google and crashes otherwise ("APK is signed by unrecognized
  certificates"). Every Gboard patch must `dependsOn(signatureCheckPatch)` (`misc/signature`), since any patched
  APK is re-signed.
- Gboard is a preinstalled system app, and a re-signed Gboard can't be installed over it.
  `Change package name` makes the patched app a separate keyboard. Its default name keeps the original package
  as a prefix, because the app checks for itself with `startsWith("com.google.android.inputmethod.latin")`.
  Provider authorities and declared permissions are renamed in the manifest. Code builds them from `getPackageName()`.
- Behaviour is driven by server flags created as `name -> default` in static initializers. Override them with
  `hookFlagReads` (`shared/FlagUtils.kt`): every read of the flag passes its value through an extension method,
  so values sent by the server are overridden too, and the extension can decide at runtime from a setting.
- Patch settings are on a "Quest" screen of Gboard's settings (`misc/settings`), shown with Gboard's own preference
  widgets. Patches with settings depend on `settingsPatch` and add preferences with `preferencesPatch`. Keys must match
  `Settings.java` in the extension. The screen is `QuestSettingsFragment`, an extension class whose resource method
  is renamed at patch time to override the obfuscated one of `CommonPreferenceFragment`.
- Gboard's settings are in device protected storage (a keyboard is used before unlock). The extension reads them
  from there (`Utils.getPreferences`); reading normal storage only ever returns the defaults.
- `Use KLIPY for GIFs` points Tenor's API flags at `api.klipy.com`, whose API is Tenor's v2 API. Its default API key
  is never committed: the build reads it from the `klipyApiKey` Gradle property or `KLIPY_API_KEY` environment
  variable into `patches/src/main/resources/gboard/klipy.properties`, and releases use the `KLIPY_API_KEY` secret.
- The APK target is the arm64-v8a nodpi APK from APKMirror. Its versionName has a suffix
  (`18.4.1.985164140-release-arm64-v8a`).

## Commits and releases

- Work on `dev`. `main` gets stable releases through the auto-opened `dev` -> `main` PR.
- Conventional commits drive semantic-release: `feat` = minor, `fix`/`perf` = patch, `chore`/`docs`/`build` = no release.
  Scope is the app and patch: `feat(Gboard - Remove clipboard length limit): ...`, or just `feat(Gboard): Add ... patch`.
- Don't edit `CHANGELOG.md`, `patches-bundle.json`, `patches-list.json` or the version in `gradle.properties`.
  The release workflow generates them.

## Building

`./gradlew buildAndroid` writes `patches/build/libs/patches-<version>.mpp`. The Morphe Gradle plugin is on
GitHub Packages, so a token with `read:packages` is needed (`gpr.user`/`gpr.key` or `GITHUB_ACTOR`/`GITHUB_TOKEN`).
