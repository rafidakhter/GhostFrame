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