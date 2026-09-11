# Squishflow — from Test Store to real money

Everything in the app is ready to charge; what is missing is store accounts
and two keys. This is the exact order, with what each step costs.

## What is already true in the code

- Purchases, restore and the entitlement go through RevenueCat's KMP SDK; the
  plans screen is ours (`PlansScreen.kt`) and reads prices from the offering.
- Any active entitlement counts as Pro (`RevenueCatManager.isPro`), so the
  dashboard's naming cannot silently break a purchase again.
- Free/Pro is enforced in product logic: every body earnable with focus, Pro
  unlocks them now; free protects three apps, Pro any number
  (`ProtectionAllowance.kt`). A lapsed Pro keeps everything earned.
- A release build refuses the Test Store key and degrades to "everything is
  earnable" instead of crashing; the SDK itself closes the app otherwise.
- The Android release build is minified and shrunk (R8) and signs with the
  upload key from the environment (`androidApp/build.gradle.kts`).
- Privacy, terms and support pages are in `docs/` and linked from the plans
  and journey screens (`Links.kt`). They go live when GitHub Pages is switched
  on for the repository: Settings → Pages → Deploy from branch → `main` /
  `/docs`. URL: https://tapaderuza.github.io/squishflow/

## Accounts (one afternoon, ~120 €)

1. **Google Play Console** — 25 $ once. Identity verification takes 1-3 days.
2. **Apple Developer Program** — 99 $/year. Needed for TestFlight and the App
   Store; also the only way to sign the iOS app for a real phone without
   Sideloadly.
3. **RevenueCat** — free until 2.5 k$/month of tracked revenue. The project
   already exists (Test Store). Add a Play Store app and an App Store app to
   the same project; do not create a new project.

## Products (same identifiers everywhere)

| Package (RevenueCat) | Product ID | Type | Suggested price |
|---|---|---|---|
| `$rc_monthly` | `squishflow_pro_monthly` | subscription, 1 month | 4.99 € |
| `$rc_annual` | `squishflow_pro_yearly` | subscription, 1 year | 29.99 € |
| `$rc_lifetime` | `squishflow_pro_lifetime` | non-consumable | 59.99 € |

- Create the three products in Play Console (Monetise → Products) and in App
  Store Connect (Subscriptions group "Squishflow Pro" + one non-consumable).
- In RevenueCat: Products → import; Entitlements → attach all three to
  `Squish Pro`; Offerings → `default` with the three packages above. The
  plans screen preselects the yearly package and explains its monthly price.
- A 7-day free trial on the yearly plan is worth adding at the store level;
  the SDK surfaces it and nothing in the app needs to change.

## Keys

- Android: put `revenuecat.android.key=goog_…` in `local.properties` (never
  committed) or export `REVENUECAT_ANDROID_KEY` in CI. `BuildConfig` picks it
  up; nothing in Kotlin changes.
- iOS: set `REVENUECAT_IOS_KEY=appl_…` in `iosApp/Configuration/Config.xcconfig`
  (or an untracked `Local.xcconfig` included from it). `Info.plist` carries it
  to the app.

## Android release

```bash
export SQUISHFLOW_KEYSTORE=/path/to/upload.jks
export SQUISHFLOW_KEYSTORE_PASSWORD=… SQUISHFLOW_KEY_ALIAS=upload SQUISHFLOW_KEY_PASSWORD=…
./gradlew :androidApp:bundleRelease
```

Output: `androidApp/build/outputs/bundle/release/androidApp-release.aab`.
Bump `app.versionCode` in `gradle.properties` for every upload.

First upload goes to **Internal testing**. Before anything public:

- [ ] Make one sandbox purchase per package with a licence-tester account and
      confirm the body unlocks on the spot and after a cold start.
- [ ] Cancel from Play and confirm the app keeps earned bodies and protected apps.
- [ ] Tap Restore on a fresh install and confirm Pro comes back.
- [ ] Accessibility service: Play requires the prominent disclosure the app
      already shows, and a declaration form in Console (App content →
      Accessibility). Explain: detects foreground app to offer a pause;
      never reads screen content.
- [ ] Data safety form: no data collected except purchase history via
      RevenueCat; no data shared; no analytics.

## iOS release

- Xcode on a Mac that runs the current Xcode (the Monterey machine cannot;
  CI's macOS runner can archive but cannot upload without your certificates).
- Sign with the Apple Developer team, archive, upload to TestFlight.
- App Review: Family Controls needs a distribution entitlement request; the
  current build ships "timer only" and does not use it, so nothing to request.
- App Store requires the privacy and terms links near the purchase button —
  they are there — and a "Restore purchases" button — it is there.

## After launch, the two numbers that matter

RevenueCat's charts give both for free: **trial → paid conversion** and
**monthly churn**. The pitch that free protects three apps is a hypothesis;
if conversion is under 2 % after a month, the lever to test first is the
allowance (`FREE_PROTECTED_APPS`), not the price.
