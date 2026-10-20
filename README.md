# Fluxlator

Fluxlator is a community-modified Winlator build for Android. It adds per-container **Performance**, **Balanced**, and **Compatibility** profiles, plus a **Custom** option for manual tuning.

**Website:** https://jeffda67.github.io/Fluxlator/  
**Download:** [Fluxlator releases](https://github.com/jeffda67/Fluxlator/releases)

## First APK

Version 0.1.0 is a debug-signed preview APK for ARM64 Android devices. It is intended for sideloading; Android may ask you to allow installation from the app you use to open the APK. Performance and game compatibility vary by device and title. Roblox support is not guaranteed, and this build does not bypass or hide from anti-cheat systems.

## Profiles

- **Performance** selects the Box64 Performance preset and Essential startup services.
- **Balanced** selects the Box64 Intermediate preset and Essential startup services.
- **Compatibility** selects the Box64 Stability preset and Normal startup services.
- **Custom** leaves the existing manual options available.

Profiles are starting points. The best choice depends on the device, graphics driver, and game.

## Build from source

Requirements: JDK 17, Android SDK Platform 35, Android Build Tools 34 or later, NDK 24.0.8215888, and CMake 3.22.1.

In PowerShell, run:

```powershell
./gradlew.bat assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Upstream and licensing

Fluxlator is derived from [Winlator](https://github.com/brunodev85/winlator-app) and is not an official Winlator release. This repository keeps the upstream source history and license notices. The project is distributed under the included LGPL-2.1 license; see `LICENSE` for the full text. Third-party components retain their own licenses and credits in the source.

## Limitations

Fluxlator does not include Windows or game licenses. It does not guarantee that a particular Windows game will run. Use official game clients and follow each game's rules. This project does not provide anti-cheat evasion or detection bypasses.
