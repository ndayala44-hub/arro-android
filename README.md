# Arro for Android

A working Android build of the Arro remittance app designs: sign-up and login, identity checks with the camera, sending money with live rates, tracking, recipients, cards and banks, multi-currency wallet, rate alerts, statements, support chat, disputes and account settings.

The app runs on its own. Its "backend" is a simulated service inside the app that keeps your data on the phone, moves transfers through real-looking stages over time, and pulls live mid-market exchange rates from the internet when it can. Nothing moves real money.

- Kotlin and Jetpack Compose, one module (`app`)
- Min Android 8.0 (API 26), target Android 15 (API 35)
- Package `app.arro` (debug builds install as `app.arro.debug`, next to any release build)

---

## Build it in GitHub Codespaces (works from a tablet)

1. **Make a repository.** On github.com, create a new private repository and tick "Add a README file".
2. **Open a Codespace.** In the repository, tap **Code → Codespaces → Create codespace on main**. A 4-core machine is enough.
3. **Upload the project.** Drag `Arro-Android.zip` into the Explorer panel on the left (or right-click the Explorer → **Upload…**).
4. **Unpack it.** Open the terminal (**☰ → Terminal → New Terminal**) and run:

   ```bash
   unzip -q Arro-Android.zip && rm Arro-Android.zip
   cp -a arro-android/. . && rm -rf arro-android
   ```

5. **Install the Android SDK** (about 3–5 minutes, once per Codespace):

   ```bash
   bash .devcontainer/setup-android.sh
   ```

   If you later rebuild the Codespace container, this runs by itself.

6. **Build the app** (the first build downloads Gradle and libraries, about 5–8 minutes; later builds take under a minute):

   ```bash
   ./gradlew assembleDebug
   ```

7. **Get the APK.** In the Explorer open `app/build/outputs/apk/debug/`, right-click `app-debug.apk` → **Download**.
8. **Install it on your phone or tablet.** Open the downloaded file. If Android asks, allow installs from that app (Settings → Apps → Special access → Install unknown apps), then tap **Install**.

Run the unit tests with `./gradlew testDebugUnitTest`.

### Or let GitHub build it for you

Commit and push from the Codespace terminal:

```bash
git add -A && git commit -m "Arro Android app" && git push
```

The workflow in `.github/workflows/android.yml` runs the tests and builds the APK on every push. Open the **Actions** tab, pick the latest run, and download **arro-debug-apk** at the bottom of the page.

### On a Windows laptop

Install Android Studio (Ladybug or newer), choose **Open**, pick the `arro-android` folder, let Gradle sync, then press **Run** with a phone connected or an emulator running.

---

## Trying it out

Open the app and either create an account (any UK mobile number works; the SMS code appears as a notification and a banner you can copy) or tap **Log in** and use the sample account:

| | |
|---|---|
| Email | `eric.nshuti@mail.com` |
| Password | `Arro2026!` |
| Passcode | `246810` |

The sample account has a year of transfers, six recipients, two cards, a connected Monzo account, GBP/EUR/USD balances, a rate alert and statements.

### Test triggers

| Do this | What happens |
|---|---|
| Send to a payout number or account ending **000** | Delivery fails, then a full refund runs |
| Add a recipient whose number ends **404** | The name check can't find the account |
| Add a recipient whose number ends **999** | The registered name doesn't match |
| Send more than **£1,000** | The transfer goes on hold until you answer two questions |
| Add a card ending **0002** | The bank declines it at the approval step |
| Add a Mastercard starting **51–53** | It's treated as a credit card (1.75% fee) |
| Your first transfer up to £1,000 | No Arro fee |
| Reset your password | New transfers are paused for 24 hours |

### Demo controls

**Account → Demo controls** lets you:

- switch **Fast mode** on or off (transfers, ID checks and refunds finish in about a minute, or take realistic times)
- make one payout provider "offline" to see the unavailable screen
- make the next ID check ask for a retake
- finish everything that's waiting right now
- lift the 24-hour sending pause
- reload the sample account, or erase everything on the phone

### Things that use the phone's hardware

- **Camera:** ID photo capture with on-device quality checks, a selfie liveness check (ML Kit face detection), card scanning (ML Kit text recognition) and QR codes for Arro-to-Arro payments. On a device with no camera, the ID step offers a sample image and card details can be typed in, so every flow can still be finished.
- **Fingerprint or face unlock:** the system biometric prompt, for unlocking and approving transfers.
- **Notifications:** transfer updates, security codes and rate alerts, with quiet hours.
- **Files:** receipts and statements are real PDFs (statements can also be CSV), saved to Downloads or shared.
- **Contacts:** pick a recipient from your contacts without granting contacts permission.

The app locks after 5 minutes in the background or 5 minutes without a touch, and shows the offline state when the network drops.

**Account → Appearance** switches between Light, Dark and "Same as phone".

---

## How it's put together

```
app/src/main/java/app/arro/
  core/        Plain Kotlin: money, catalogue of countries and providers, models,
               validation, rates, the simulated backend, seed data, support agent
  platform/    Android services: state file, live rates, connectivity, notifications,
               biometrics, sharing, camera, PDF/CSV documents
  ui/          Theme, components, navigation, session drafts, and screens by area:
               auth, kyc, home, send, track, recipients, payments, wallet, rates,
               support, account, system
app/src/test/  Unit tests for the backend (16 tests)
```

- **State:** everything is one JSON document (`filesDir/arro_state.json`), exposed as a `StateFlow` that the screens observe. It survives restarts, and transfer progress is worked out from timestamps, so a transfer keeps moving while the app is closed.
- **Rates:** live mid-market rates from `open.er-api.com` every 5 minutes, with built-in fallback rates when offline. Arro's margin is 0.29% on transfers and 0.27% on conversions.
- **Navigation:** one activity, Compose Navigation, a floating tab bar on the four main tabs.

## Limits to know about

- This is a test build with a simulated backend. Accounts, transfers and balances exist only on the device. To go live you would replace `ArroBackend` with calls to a real API; the screens only talk to it through its public functions.
- Legal text (terms, privacy, regulator details) is placeholder copy marked with brackets.
- Only English is available; other languages show as coming soon.
- Release builds are signed with the debug key so they install for testing. Add your own signing config before publishing.
- The code was compile-checked and the backend tests were run, but the first full Gradle build happens on your machine or in GitHub Actions. If anything fails there, send me the first error from the build output.
