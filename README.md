# Pinterest Patches

Patches for Pinterest, for use with [Morphe](https://morphe.software).

## Patches

| Patch                     | Description                                                                          |
|---------------------------|--------------------------------------------------------------------------------------|
| Enable image download     | Restores the "Download image" option in the Pin menu and share sheet.                  |
| Enable video download     | Adds "Download image" to video Pins and saves the video in the best quality.          |
| Copy image                | Adds "Copy image" below "Download image" in the Pin menu.                            |
| Share image with Android  | Adds an option to share the image of a Pin with Android's share sheet, instead of the app's share menu. |
| Show date posted          | Adds an option to show the date a Pin was posted, above its creator.                 |
| Customize long-press menu | Adds Download and Copy image to the menu shown when holding a Pin, and options for its other items. |

Supported Pinterest version: 14.39.0

Options are in Pinterest's settings, under "Pinterest Patches".

## Usage

Add this repository as a patch source in Morphe:
https://morphe.software/add-source?github=archivesteak/pinterest-patches

## Building

```sh
./gradlew buildAndroid
```

The bundle is written to `patches/build/libs/patches-*.mpp`. Building needs a GitHub token
with `read:packages`, set as `gpr.user`/`gpr.key` in `~/.gradle/gradle.properties`.

## License

[GPLv3](LICENSE)
