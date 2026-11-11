# Play Console answers (suggested)

Draft answers for the Google Play Console forms. Owner to review before submitting.

## Data safety

- Does the app collect or share any user data? **No** (no data collected, no data shared).
- Explanation for reviewers:
  - The app declares no INTERNET permission, so it cannot transmit data off the device.
  - Microphone audio is processed in memory only to compute a decibel level. Audio is never written to storage or sent anywhere.
  - Noise history (decibel readings, timestamps, challenge results) is stored locally on the device only.
  - No accounts, no analytics SDKs, no advertising ID.
- Encryption in transit: not applicable (no network transmission). Data deletion: uninstalling the app removes all local data.

## Foreground service declaration (type: microphone)

- The app runs a foreground service of type `microphone` only when the user explicitly starts listening (Home screen toggle, or the "Keep listening with screen off" setting).
- The service is user-initiated and continuous while the user keeps it on. It shows a persistent notification with Buddy's current mood and a **Pause** action, which stops listening.
- Core user benefit: continuous noise monitoring in a classroom, home or shared space, where the user needs to know if the room gets loud while the phone screen is off.
- Suggested demo video (30 to 60 seconds, screen recording plus narration if possible):
  1. Open the app and tap to start listening. Show the Home screen with Buddy happy.
  2. Make noise (or play a loud sound source) and show Buddy's mood changing to worried, then sad.
  3. Show the persistent notification with the mood and the **Pause** action.
  4. Lock the screen, show the notification still updating, then tap **Pause** and show that listening has stopped.
  5. Optionally show the microphone permission prompt and the Settings toggle that enables background listening.

## Ads

- Does the app contain ads? **No.**

## App access

- Are any parts of the app restricted (login, paywall, etc.)? **No.** All features are available without an account or sign-in. Reviewers do not need credentials.

## Content rating questionnaire (suggested answers)

- Category: Utility / reference tool, no user-generated content.
- Violence: **No** (no violence, cartoon or otherwise).
- Sexual content / profanity / controlled substances / gambling: **No.**
- User interaction: **No.** No chat, no user-to-user communication, no shared content.
- Sharing location: **No.**
- Purchases / in-app products: **No.**
- Personal information shared with others: **No.**
- Expected outcome: a low or lowest-age rating (exact rating depends on the answers and the target audience decision below).

## Target audience (OPEN DECISION for the owner)

Not decided here. Choose one:

- **Ages 13 and older (or adults only).** Standard content rating flow. No Families policy requirements. Simplest review path. Child-oriented wording in the store listing (for example "for young children") would need to be reconsidered, since it can push the app toward the Families policy.
- **Includes children under 13.** The app must comply with the Google Play Families policy: no ads (already the case), no non-compliant SDKs (none present), a privacy policy that covers children, and stricter review of listing claims. Families also require the app to meet the Designed for Families requirements if it is listed there.

Implications to weigh: the current app has no INTERNET permission and no data collection, which makes Families compliance easier. The listing text currently mentions young children as one use case, which the owner should keep or drop based on this decision.

## Category and tags (suggestion)

- Category: **Tools**. Not recommended: Health & Fitness or Productivity, since the app is not a fitness or health tool.
- Tags (if the console offers them): Utilities, Noise, Sound meter, Quiet, Family.

Note: the "Family" tag is only consistent with the Families policy path. Drop it if the owner chooses adults or 13+.

## Graphic assets checklist

- [ ] App icon: 512 x 512 px PNG, 32-bit, up to 1 MB (the adaptive icon source from the repo can be exported).
- [ ] Feature graphic: 1024 x 500 px PNG or JPEG.
- [ ] Phone screenshots: at least 2, 16:9 or 9:16, each side between 320 and 3840 px. Suggested screens:
  1. Home with Buddy happy (calm room).
  2. Home with the loud mood (Buddy worried, sad or covering ears, "Too loud!" band).
  3. History screen (day bars and hour-by-hour view).
  4. Settings screen with noise limit presets and quiet hours.
- [ ] Optional: 7-inch and 10-inch tablet screenshots, if the owner wants tablet listing.
- [ ] Optional: promo video (YouTube URL).
