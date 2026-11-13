# Multi-room monitoring

Status: proposal. Nothing here is built yet.

A school with several classrooms may want one person (a head teacher, the office) to see every room at once: which rooms are listening, how loud each one is right now, and the history of each. They may also want to pause and resume rooms remotely. This document proposes how to do that securely, without a server, and without costing battery.

## Constraints from the current app

- **The app has no internet permission** (`AndroidManifest.xml`), and the README promises that "nothing leaves your phone."
- **Android 14 won't let an app start using the microphone in the background.** `ListeningController.start()` has to be called while the app is on screen.
- **Pausing stops the listening service completely** (`ListeningService.pauseAndStop()`).

All three shape the design below.

## 1. Where it lives in the app

This is a feature for a minority of advanced users, so it gets no new tab and most users never see it.

- **Turning it on:** two entries go in Settings, under a collapsed "Several rooms" section:
  - "Let another device watch this room": this phone is a classroom device.
  - "Watch other rooms": this phone is a monitor.
- **Once at least one room is paired,** the monitor gets a small "rooms" icon in Home's top bar. It opens a **Rooms dashboard**: a grid of tiles, each showing the room name, a small character face for its mood, the dB level, its state (listening / paused / offline, with "last seen"), and a pause/resume button. There is also "Pause all" for assemblies and exams.
- **History reuses `HistoryScreen` as is,** with a room picker at the top ("This device ▾ / Room 2B / Library"). The ViewModel is pointed at a data source for that room. Nothing new to design or translate.
- **"Monitor only" mode** is optional, for a head teacher's or office tablet that doesn't listen itself. The dashboard replaces Home. It's a Settings switch, not an onboarding question.

## 2. How data moves: local network only, no server

Classroom devices talk directly to monitors over the school's Wi-Fi. There is no cloud and no accounts. This keeps most of the privacy promise and needs no backend.

**The biggest risk is that school Wi-Fi often blocks devices from talking to each other.** Guest and BYOD networks commonly use "client isolation", which blocks both device-to-device traffic and discovery. Bluetooth or Wi-Fi Direct can't reach across a building, so they aren't a fallback. If the target schools' Wi-Fi works this way, the only option is a relay server that passes along end-to-end encrypted data. That is left for a later phase. **Test this on a real school network before building anything.**

## 3. Pairing and security

- **Identity:** each install creates its own key pair in Android Keystore (EC P-256) and a self-signed certificate. The private key never leaves the device.
- **Pairing by QR code, done in the classroom:**
  1. The classroom device shows a QR code. It contains the device ID, the certificate fingerprint, a random 128-bit single-use secret that expires after about 2 minutes, and its current IP and port.
  2. The monitor scans the code and connects over TLS 1.3, trusting only the certificate in the QR code. It proves it knows the secret and sends its own certificate.
  3. The classroom device asks for confirmation on screen: *"Allow 'Head teacher's phone' to watch this room?"* Consent happens in the room itself.
  4. From then on, every connection uses TLS 1.3 with both sides pinned to each other's certificates. No passwords, no certificate authority.
- **Permissions per pairing:** the classroom device decides whether the monitor can only watch, or also pause and resume. Either side can unpair, which deletes the pinned key.
- **Discovery:** classroom devices announce themselves on the local network (mDNS service `_quietbuddy._tcp`) with only a random ID, never the room name. Monitors try the last known address first and use discovery only if that fails.
- **Commands are a short fixed list:** `status`, `minutes since X`, `pause`, `resume`. No free-form requests, and only private network addresses are allowed.

## 4. Battery

- **Classroom device:** the only new cost is an idle listening socket, which costs almost nothing, plus the system's own mDNS announcements. It never streams audio and never sends anything unless asked. A foreground service should keep network access during Doze, but that needs checking on real devices. These devices are usually plugged in anyway.
- **Monitor:** no work at all in the background.
  - It opens connections only while the dashboard or a room's history is on screen.
  - While open, each classroom device pushes an update when the mood changes, or every 5 seconds or so with the dB level.
  - Connections close as soon as the user leaves the screen.
- **History sync** fetches only new data: per-minute rows (`LevelMinuteEntity`) newer than the monitor's last sync. A full school day is a few KB. The monitor keeps a copy in a `remote_level_minute` table keyed by room, so a room's history is still visible when its device is off. Classroom devices already keep 35 days, so a monitor that's been offline for a week still catches up.
- **Deliberately left out:** "Room 3 has been too loud for 5 minutes" alerts on the monitor. They would need a connection that stays open in the background, which is the one costly part. If schools ask for it, it can come later as an opt-in.

## 5. Turning rooms on and off remotely

Because of the Android 14 rule, **a monitor can never start the microphone on a stopped classroom device.** So "pause from the monitor" can't use today's `pauseAndStop()`. It needs a soft pause:

- Recording stops, but the service and its connection stay up.
- The notification says "Paused by a monitor".
- Resume works because the microphone service is still running.

If someone stopped the device locally, the dashboard shows "Stopped on the device" and can't start it. Automatic school-hours schedules hit the same limit for the same reason.

## Open decisions

1. **Adding the network permission changes the privacy promise.** It would become: "uses the network only to talk to devices you paired, on your own local network; no servers." That means updating the README, the privacy policy and the Play data-safety form. The alternative is a separate build or companion app, which is more work.
2. **Local network only vs. an encrypted relay** depends on what the target schools' Wi-Fi allows.
3. **Remote control scope:** is watching plus pause/resume enough for version 1, or do schools also need to change a room's noise limit remotely?

## Suggested order

1. Pairing and the Rooms dashboard (live status only).
2. Remote history with the room picker in `HistoryScreen`.
3. Soft pause and remote pause/resume.
