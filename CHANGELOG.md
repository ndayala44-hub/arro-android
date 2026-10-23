# Changelog

All notable changes to the Arro Android template. Versions follow `versionName` in `app/build.gradle.kts`.

## 1.0.0 (23 October 2026)

First release.

- 96 screens and states in Kotlin and Jetpack Compose: onboarding and registration with SMS code, passcode and biometrics; identity verification with camera checks and a selfie; sending money with live quotes, a 30-minute rate hold, delivery methods, recipients and payment methods; tracking with holds, cancellations and refunds; recipients with name checks; cards (with scanning) and banks; a multi-currency wallet with conversions, activity, PDF receipts and PDF/CSV statements; rates, charts and rate alerts with automatic sending; support centre, chat and disputes; account, security and settings.
- Lock Rate: random and on-demand rate offers, rates locked on the backend for 10 minutes, expiry notices and server-side checks when a transfer is created.
- Loyalty and rewards: points on delivered transfers, fee-credit redemption, referral rewards shown separately, opened from Home and Account.
- Home screen widgets: Quick Send, Balance and Rate, resizable, with light and dark themes.
- Light and dark themes with one set of colour tokens.
- All screen text in `res/values/strings.xml`, ready for translation.
- Simulated backend with one public API (`ArroBackend`), saved data with upgrades, live exchange rates from ExchangeRate-API, sample account and Demo controls.
- Sample account settings in `gradle.properties`; release signing through `keystore.properties`.
- Targets Android 16 (API 36); runs on Android 8.0 (API 26) and newer.
- 30 unit tests, a screenshot tour for store images, and GitHub Actions workflows.
