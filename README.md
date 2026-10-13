# Pinterest Patches

Patches for Pinterest, for use with [Morphe](https://morphe.software).

[![Add to Morphe](https://img.shields.io/badge/Morphe-Add%20Source-00A8FF?style=for-the-badge)](https://morphe.software/add-source?github=archivesteak/pinterest-patches)

## 🩹 Patches

<!-- PATCHES_START EXPANDED -->
<!-- PATCHES_END -->

## 🛠️ Building

```sh
./gradlew buildAndroid
```

The bundle is written to `patches/build/libs/patches-*.mpp`. Building needs a GitHub token
with `read:packages`, set as `gpr.user`/`gpr.key` in `~/.gradle/gradle.properties`.

## 📜 License

[GPLv3](LICENSE)
