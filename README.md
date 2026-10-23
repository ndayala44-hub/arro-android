# Arro: Money Transfer and Remittance App Template for Android

A complete, native Android app for sending money abroad, written in Kotlin and Jetpack Compose, ready to rebrand and connect to your own backend.

- 96 screens and states, light and dark themes, three home screen widgets
- Registration with SMS code, passcode and biometrics; identity checks with the camera
- Sending money with live quotes and a rate lock, delivery methods, recipients, cards and banks
- Tracking with security holds, cancellations and refunds; a multi-currency wallet; PDF receipts and statements
- Rates and alerts, Lock Rate offers, loyalty points and referral rewards, support chat and disputes
- All screen text in `strings.xml`; one place for colours and fonts
- A simulated backend with one API (`ArroBackend`), so every button works before you connect a server

> **Arro is a front-end template with a simulated backend.** Nothing moves real money. To go live you connect your own servers, payment provider, identity-verification provider and payout partners. See the Backend and API guide in the documentation.

## Requirements

- Android Studio Meerkat (2024.3.1) or newer, with JDK 17
- Android SDK Platform 36 and Build-Tools 35.0.0
- Internet for the first build
- Runs on Android 8.0 (API 26) to Android 16 (API 36)

Versions used: Gradle 8.11.1 (wrapper), Android Gradle Plugin 8.9.1, Kotlin 2.0.21, Compose BOM 2024.12.01.

## Quick start

1. Open this folder in Android Studio (**File › Open**) and let Gradle sync.
2. Press **Run** with a phone or emulator.
3. Tap **I already have an account › Fill in sample account**, or log in with `eric.nshuti@mail.com` / `Arro2026!` (passcode `246810`). These are sample settings in `gradle.properties`, not real credentials.

From a terminal:

```bash
./gradlew assembleDebug        # debug APK
./gradlew testDebugUnitTest    # unit tests
./gradlew bundleRelease        # app bundle for Google Play (sign it first, see below)
```

## Before you publish

1. Change `applicationId` in `app/build.gradle.kts` and `app_name` in `app/src/main/res/values/strings.xml`.
2. Replace the logo, launcher icon and colours (see Customization in the documentation).
3. Turn off the sample account: `arro.sample.enabled=false` in `gradle.properties`.
4. Sign releases: copy `keystore.properties.example` to `keystore.properties` and add your key.
5. Connect your backend and providers in place of the simulated ones.

## Project layout

```
app/src/main/java/app/arro/
  core/       Models, money, rates, catalogue, rules and the simulated backend (plain Kotlin)
  platform/   Storage, live rates, connectivity, notifications, biometrics, camera, PDF/CSV
  ui/         Theme, components, navigation and screens by area
  widget/     Home screen widgets
app/src/main/res/        Strings, colours, fonts, icons, illustrations, widget layouts
app/src/test/            30 unit tests for the backend
app/src/androidTest/     Screenshot tour (drives the app on a device and saves screenshots)
.github/workflows/       Build and release-assets workflows
licenses/                Font licence
```

## More

- Full documentation: `Documentation/index.html` in the download (also as a PDF)
- Changes: `CHANGELOG.md`
- Third-party licences: `THIRD_PARTY_LICENSES.md`
- Lock Rate, rewards and widgets in detail: `docs/LOCK_RATE_LOYALTY_WIDGETS.md`
