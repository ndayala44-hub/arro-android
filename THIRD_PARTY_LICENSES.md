# Third-party licences

Arro is built on open-source libraries from Google, JetBrains and others. You don't download any of them by hand: Gradle fetches them from Google's Maven repository and Maven Central when you build. Their licences allow use in commercial apps. If you publish the app, list them in your app's or website's open-source notices. The documentation (Credits and licences) has the same list.

## Libraries in the app

| Library | Version | Licence |
|---|---|---|
| Kotlin standard library | 2.0.21 | Apache License 2.0 |
| kotlinx.coroutines (core, android) | 1.9.0 | Apache License 2.0 |
| kotlinx.serialization (json) | 1.7.3 | Apache License 2.0 |
| AndroidX Core KTX | 1.15.0 | Apache License 2.0 |
| AndroidX Core SplashScreen | 1.0.1 | Apache License 2.0 |
| AndroidX Activity Compose | 1.9.3 | Apache License 2.0 |
| AndroidX Fragment KTX | 1.8.5 | Apache License 2.0 |
| AndroidX Lifecycle (runtime KTX, runtime Compose) | 2.8.7 | Apache License 2.0 |
| AndroidX Navigation Compose | 2.8.5 | Apache License 2.0 |
| Jetpack Compose (UI, foundation, Material 3, tooling) | BOM 2024.12.01 | Apache License 2.0 |
| AndroidX Biometric | 1.1.0 | Apache License 2.0 |
| CameraX (core, camera2, lifecycle, view) | 1.4.1 | Apache License 2.0 |
| ZXing core (QR codes) | 3.5.3 | Apache License 2.0 |
| Google ML Kit text recognition | 16.0.1 | ML Kit Terms of Service |
| Google ML Kit face detection | 16.1.7 | ML Kit Terms of Service |
| Google ML Kit barcode scanning | 17.3.0 | ML Kit Terms of Service |

ML Kit is free to use but is not open source. It's covered by Google's ML Kit terms (developers.google.com/ml-kit/terms). Its models run on the device; Google may collect usage information about ML Kit, so mention it in your privacy policy.

## Build and test tools (not in the app)

| Tool | Version | Licence |
|---|---|---|
| Gradle (wrapper) | 8.11.1 | Apache License 2.0 |
| Android Gradle Plugin | 8.7.3 | Apache License 2.0 |
| JUnit | 4.13.2 | Eclipse Public License 1.0 |
| AndroidX Test (core, runner, ext JUnit) | 1.6.1 / 1.6.2 / 1.2.1 | Apache License 2.0 |

## Fonts

**Outfit**, by the Outfit Project Authors, under the SIL Open Font License 1.1. You can use, bundle and sell it in apps. The full licence is in `licenses/Outfit-OFL.txt`; keep it with the font files.

## Icons, illustrations, flags and logo

All icons, illustrations, country flags, the Arro logo and the brand mark were made for this item and are included under your Envato licence. No stock images, icon packs or photos are used.

National flags are public symbols; the flag drawings are original.

## Exchange-rate data

Live mid-market rates come from ExchangeRate-API's free open-access endpoint (`open.er-api.com`). Its terms require a visible credit ("Rates By Exchange Rate API", linked to exchangerate-api.com), which the app shows on the Home, Rates and Lock a rate screens; allow commercial use and caching; ask for at most about one request an hour; and don't allow you to redistribute the data. For a live service, use a rates provider you have a contract with.

## Names in the sample data

The sample data mentions real banks, card brands and mobile-money networks by name, as plain text, so the app looks realistic. Arro isn't affiliated with any of them, and no logos are included. Replace them with your own partners.

## Licence texts

- Apache License 2.0: https://www.apache.org/licenses/LICENSE-2.0
- SIL Open Font License 1.1: `licenses/Outfit-OFL.txt`
- Eclipse Public License 1.0 (JUnit, tests only): https://www.eclipse.org/legal/epl-v10.html
- ML Kit Terms of Service: https://developers.google.com/ml-kit/terms
- ExchangeRate-API terms: https://www.exchangerate-api.com/terms
