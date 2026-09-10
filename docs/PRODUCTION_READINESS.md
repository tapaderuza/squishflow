# Squishflow — production and Shipaton gate

Status on 2026-08-13: **NO-GO for public production; viable MVP candidate.**

This gate applies the Agency Agents startup workflow: prioritize the smallest winning scope, require evidence for every release claim, and run a final Reality Checker before submission.

## P0 — required to publish and remain eligible

- [ ] Register for Shipaton and complete the participant form.
- [ ] Open Google Play Console and Apple Developer accounts.
- [ ] Publish the first public release between 2026-08-01 and 2026-09-30.
- [ ] Replace RevenueCat Test Store keys with `goog_...` and `appl_...` keys.
- [ ] Create and approve monthly/annual products in both stores; map them to `default` and `squish_pro`.
- [ ] Test purchase, cancellation, expiration, restore and seven-day trial on store sandboxes.
- [ ] Sign an Android AAB and archive an iOS release from a Mac.
- [ ] Host privacy policy, terms and support pages at stable public HTTPS URLs and link them inside the app.
- [ ] Add an in-app data deletion/reset action and AI data-sharing consent before sending goal text.
- [ ] Either obtain Apple's Family Controls distribution entitlement or remove Family Controls and its extensions from the release target.
- [ ] Either obtain Google Play approval for Accessibility Service with prominent disclosure or remove the service from the release build.
- [ ] Remove camera/burpee from the MVP release or complete permission, privacy and real-device verification.
- [x] Replace placeholder Lottie rendering with the shared Compose-native Squishy motion system.
- [ ] Resolve Kotlin metadata/lint incompatibility instead of accepting a release that prints compiler errors.
- [ ] Run full Android and iOS real-device regression tests.

## P1 — required for a credible winning submission

- [ ] Deploy the server-side AI planner; never ship an AI provider key in the app.
- [ ] Validate structured AI output, timeouts, retries, moderation and a local fallback.
- [ ] Make adaptation evidence-based: session outcomes change the next plan and the reason is visible.
- [ ] Enforce Free limits and Pro entitlements in product logic, not only in paywall copy.
- [ ] Add restore purchases and subscription-management/Customer Center access.
- [ ] Add reduced-motion behavior, semantic labels, TalkBack/VoiceOver and dynamic-type tests.
- [ ] Measure cold start, animation frame stability, memory and battery use.
- [ ] Produce a distinctive 1024×1024 icon and final store screenshots at 1179×2556.
- [ ] Record a two-minute device demo whose first 120 seconds show pitch, product, RevenueCat and target categories.
- [ ] Create a Devpost page with every category-specific field completed.
- [ ] Recruit at least 30 beta users and record activation, completion, D1/D7 retention and conversion.
- [ ] Start a repeatable build-in-public series with public dates, decisions and measured learning.

## Deliberate category focus

1. **Ship Kotlin Everywhere:** strongest technical fit; prove shared UI/logic on both devices.
2. **RevenueCat Design Award:** possible only after final Squishy motion, haptics and accessibility.
3. **HAMM Award:** requires a real, tested monetization funnel and measured conversion.
4. **#BuildInPublic:** requires public evidence accumulated during the event, not a retrospective post.
5. **Grand Prize:** do not claim readiness without real growth momentum after release.

Do not target Peace Prize, Best Game or unrelated sponsor categories in the initial submission. Focus improves the two-minute story.

## Evidence folder contract

Store dated evidence under `evidence/release/`:

- Android and iOS screen recordings
- purchase and restore receipts with private values redacted
- accessibility test notes
- device/OS matrix and pass/fail results
- store listing screenshots
- AI failure/fallback tests
- anonymized funnel metrics
- links/captures of build-in-public posts

The final Reality Checker may return GO only when every P0 item has evidence and no open severity-1 defect remains.
