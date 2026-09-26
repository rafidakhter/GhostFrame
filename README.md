# GhostFrame

An Android app that displays a semi-transparent reference photo over other apps.

## Features

- Select a photo using Android’s photo picker.
- Display the photo over other apps.
- Drag and pinch to reposition and resize.
- Adjust opacity from 0–80% with a horizontal slider.
- Interact with apps underneath when repositioning is off.
- Access controls through a floating menu.

## Tech Stack

Kotlin, Jetpack Compose, Android Views, and Coil.

Developed in VS Code using Android Studio’s SDK and Pixel 9 emulator.

## Run Locally

Start an emulator or connect a phone with USB debugging enabled.
From the project root, run:

```bash
./gradlew installDebug
```

Open GhostFrame, choose a photo, and grant **Display over other apps**
permission. Return to GhostFrame and open the overlay.

## Build an APK

```bash
./gradlew assembleDebug
```

Output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Project Structure

- `overlay/domain` — overlay state.
- `overlay/presentation` — state controller.
- `overlay/platform` — windows, views, gestures, and notifications.
- `overlay/data` — photo loading.
- `OverlayService` — coordinates the overlay lifecycle.

## Known Issues

- Repositioning can stop responding after using the opacity panel.
  Investigation is deferred.

## Planned Work

- Image cropping.
- Fix opacity/repositioning interactions.
- Further refactoring and automated tests.

## Visible debug build numbers

The home screen shows `Version 1.0 · Debug build N`. The version comes from
`versionName` in `app/build.gradle.kts`; the debug number increments automatically
whenever Gradle generates the debug APK's resources, including these workflows:

- Android Studio **Run** (debug variant).
- `./gradlew installDebug` to build and install on a connected device/emulator.
- `./gradlew assembleDebug` followed by installing that APK.

The local counter lives in `.debug-build-number`, is ignored by Git, and survives
`./gradlew clean`. It is per checkout, not a shared release version. Failed builds
can consume a number. Gradle sync/help alone does not increment it. Configuration
cache reuse still generates a fresh number for each debug build.

An existing APK retains its embedded number when reinstalled with `adb install`;
run `installDebug` or rebuild first for a new number. Android Studio **Apply Changes**
is not a substitute for a full Run when verifying which APK is installed.
Release builds show the version with `Release`; their version code/name are unchanged.
