# Pinterest Patches

Patches for Pinterest, for use with [Morphe](https://morphe.software).

[![Add to Morphe](https://img.shields.io/badge/Morphe-Add%20Source-00A8FF?style=for-the-badge)](https://morphe.software/add-source?github=archivesteak/pinterest-patches)

## 🩹 Patches

<!-- PATCHES_START EXPANDED -->
> **[v1.0.0-dev.1](https://github.com/archivesteak/pinterest-patches/releases/tag/v1.0.0-dev.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;1 patches total
<details open>
<summary>📦 Pinterest&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 14.39.0 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Enable image download](#enable-image-download) | Restores the "Download image" option in the Pin menu and share sheet. |  |

</details>

<!-- PATCHES_END -->

## 🛠️ Building

```sh
./gradlew buildAndroid
```

The bundle is written to `patches/build/libs/patches-*.mpp`. Building needs a GitHub token
with `read:packages`, set as `gpr.user`/`gpr.key` in `~/.gradle/gradle.properties`.

## 📜 License

[GPLv3](LICENSE)
