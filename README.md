# Pinterest Patches

Patches for Pinterest, for use with [Morphe](https://morphe.software).

## Patches

| Patch                 | Description                                                         |
|-----------------------|---------------------------------------------------------------------|
| Enable image download | Restores the "Download image" option in the Pin menu and share sheet. |

Supported Pinterest version: 14.39.0

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
