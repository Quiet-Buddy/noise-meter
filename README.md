# Quiet Buddy
[![CI](https://github.com/Quiet-Buddy/noise-meter/actions/workflows/ci.yml/badge.svg)](https://github.com/Quiet-Buddy/noise-meter/actions/workflows/ci.yml)

**A little companion that listens to how loud your space is, and lets you know with a friendly face.**

Quiet Buddy is an Android app that turns your phone into a gentle noise monitor. Instead of showing you a confusing number, it shows a small character, your Buddy, whose mood changes with the noise around it. When the room is calm, Buddy is happy. When it gets too loud, Buddy gets worried, sad, and eventually covers its ears.

It's meant for places where noise matters: a classroom, a home with young children, a shared flat, a quiet study corner, or a home office.

## Download

Test builds are published on the [Releases page](https://github.com/Quiet-Buddy/noise-meter/releases/latest). Quiet Buddy requires Android 10 or newer.

More about the project at [quiet-buddy.github.io](https://quiet-buddy.github.io).

---

## How it works

1. **You place your phone** somewhere in the room.
2. **Buddy listens** through the microphone and keeps track of the noise level.
3. **Buddy's mood changes** to match:

| Buddy feels | What it means |
|---|---|
| 😊 Happy | Nice and calm |
| 🙂 Fine | A normal, comfortable level |
| 😟 Worried | Getting noisy |
| 😢 Sad | Too loud |
| 🙉 Covering ears | Very loud |

Buddy doesn't flip moods at every little sound. It waits a moment to be sure the noise has really changed, so it stays steady and doesn't flicker.

## What you can do with it

- **Pick a noise limit** that suits the place: a quiet-time setting, an everyday setting, or a more relaxed playtime setting.
- **Look back at history** to see how noisy the day was and when the loudest moments happened.
- **Take on challenges** to try to keep things quiet.
- **Keep it running in the background.** A small notification shows Buddy's current mood, and you can pause listening from it at any time.
- **Use it in English, Spanish or Basque (euskara).** It follows your phone's language.

## Your privacy

Privacy is a core part of the design.

- **Nothing is recorded.** Buddy measures how loud things are, but it does not save audio or store what anyone says.
- **Nothing leaves your phone.** The app has no internet access at all, so your noise history stays on your device.
- **No account, no sign-up, no ads.**

The app asks for microphone permission so it can measure noise, and notification permission so it can show Buddy's mood while it runs in the background.

## A note on accuracy

Quiet Buddy gives a good *general idea* of how loud a space is. It is **not a certified sound level meter**, and phone microphones differ from one model to another. Don't rely on it for legal, medical, or workplace safety measurements. The app includes a calibration option to improve accuracy on your own phone.

## What you need

An Android phone running Android 10 or newer.

## Want to help?

Ideas, feedback and bug reports are welcome. Please [open an issue](https://github.com/Quiet-Buddy/noise-meter/issues) and tell us what you think.

If you're a developer and want to build the app yourself, it's a standard Android Studio project. Open the folder in Android Studio and run it.

## Licence

Copyright (C) 2026 Quiet Buddy contributors.

Quiet Buddy is free software: you can redistribute it and/or modify it under the terms of the [GNU General Public License](LICENSE) as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; see the licence for details.

The licence covers the code. It does not grant permission to use the Quiet Buddy name or icon for modified versions: if you publish a fork, please give it a different name and icon.

## Building

- Requires JDK 17.
- Debug build: `./gradlew assembleDebug`
- Unit tests: `./gradlew testDebugUnitTest`
- Play Store bundle: `./gradlew bundleRelease` (tag builds fail without signing secrets)

Release signing reads these environment variables: `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. In CI they come from repository secrets: `ANDROID_KEYSTORE_BASE64` (base64 of the `.jks` file), `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD`.
