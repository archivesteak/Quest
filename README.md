# Quest

Patches for Pinterest and Gboard, for use with [Morphe](https://morphe.software).

## Patches

### Pinterest

Supported version: 14.39.0

| Patch                     | Description                                                                          |
|---------------------------|--------------------------------------------------------------------------------------|
| Enable image download     | Restores the "Download image" option in the Pin menu and share sheet.                  |
| Enable video download     | Adds "Download image" to video Pins and saves the video in the best quality.          |
| Copy image                | Adds "Copy image" below "Download image" in the Pin menu.                            |
| Share image with Android  | Adds an option to share the image of a Pin with Android's share sheet, instead of the app's share menu. |
| Show date posted          | Adds an option to show the date a Pin was posted, above its creator.                 |
| Customize long-press menu | Adds Download and Copy image to the menu shown when holding a Pin, and options for its other items. |

Options are in Pinterest's settings, under "Pinterest Patches".

### Gboard

Supported version: 18.4.1.985164140 (arm64-v8a, nodpi)

| Patch                         | Description                                                                  |
|-------------------------------|------------------------------------------------------------------------------|
| Remove clipboard length limit | Adds an option to keep copied text of any length in the clipboard, instead of cutting it off after 20,000 characters. |
| Use KLIPY for GIFs            | Adds an option to get GIFs and stickers from [KLIPY](https://klipy.com) instead of Tenor. |
| Change package name           | Changes the package name of the app, so it can be installed next to the preinstalled Gboard. |
| Change app name               | Changes the name of the app, so it can be told apart from the preinstalled Gboard. |

Options are in Gboard's settings, under "Quest". KLIPY works without setup. You can set your own
KLIPY API key there, or when patching.

Gboard comes preinstalled on many phones, and a patched Gboard can't be installed over it. The patched
app installs next to it as a separate keyboard, "Gboard Quest". Enable it in Settings > System > Keyboard > On-screen keyboard.

## Usage

Add this repository as a patch source in Morphe:
https://morphe.software/add-source?github=archivesteak/Quest

## Building

```sh
./gradlew buildAndroid
```

The bundle is written to `patches/build/libs/patches-*.mpp`. Building needs a GitHub token
with `read:packages`, set as `gpr.user`/`gpr.key` in `~/.gradle/gradle.properties`.

The default KLIPY API key isn't in the repository. Set it when building as the `klipyApiKey` Gradle property
or the `KLIPY_API_KEY` environment variable. Releases get it from the `KLIPY_API_KEY` repository secret.

## License

[GPLv3](LICENSE)
