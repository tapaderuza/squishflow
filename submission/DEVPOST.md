# Devpost submission — Squishflow

Everything below is ready to paste. Fields follow the Devpost form order.

---

## Project name

Squishflow

## Tagline (one line)

A focus timer you can squeeze. One Kotlin codebase, a soft-body companion you can actually feel, and a Pro tier you can earn instead of buy.

---

## Description

### The problem

Every focus app asks you to be disciplined at exactly the moment you have no discipline left. They give you a countdown and a guilt trip. Neither helps a hand that wants to reach for a phone.

### What Squishflow does

Squishflow puts a soft body between you and the work.

You tell it what you want to finish. It hands back blocks short enough to believe — the first one deliberately shorter, because starting is the expensive part of focus, not continuing. Then, while you sit with the tension, you push your thumb into Squishy and it pushes back.

**The squishy is a physics model, not an animation.** The silhouette is a ring of 26 coupled radial samples with real stiffness, damping and neighbour coupling. A press opens a dent exactly under your finger, the wave travels around the surface, and volume conservation means denting one side bulges the other. It has haptics tied to pressure and a synthesised voice: no audio files ship, and every material's squelch is derived from the same three numbers that drive its shape — stiffness sets the pitch, damping the length, coupling the wetness — so a stress ball snaps high and dry and a bubble rings. Left alone, it wakes up, blinks on an irregular rhythm, looks around, and occasionally stretches.

**It does the thing it asks of you.** Over a focus block the body softens and stops wobbling. The same drag gives about a fifth further at minute twenty-four than at minute one. Then, in the last sixty seconds, it breaks the pattern: the breath quickens and the line changes to *Almost banked.* A session visibly has depth, not just a countdown.

**It has a position on streaks.** Come back after three days away and it says *Been a while. Nothing to catch up on.* That is the moment a streak app shows a broken chain; this one says the gap does not count — and that sentence, like every line the app says, is held to rules by unit tests: no exclamation marks, no failure language, nothing longer than one line.

### Monetisation: focus is the currency

There are five bodies. **Every one can be earned with focused minutes. Pro buys them now.**

A focus app that locks its rewards behind a card is working against the thing it claims to want — it profits when you pay, not when you concentrate. Making focused minutes the free currency means the app only gets more rewarding the more it actually works. The subscription is a shortcut for people who would rather not wait, and the reason there are no ads and no analytics on anyone, paying or not. The one Pro benefit that is not cosmetic is utility: free protects three apps behind Squishy, Pro protects every one — and that limit lives in product logic with a test, not in paywall copy.

Two consequences, both unit-tested: a lapsed subscription keeps everything you earned, and a locked body shows how close you are rather than a padlock. And because only one body is free at the start, tapping a locked one hands it to you for six seconds before the paywall appears. A list of names cannot communicate what dense foam feels like.

The materials are not reskins. A test asserts that a stress ball settles in fewer frames than jelly and a water balloon in more, and another that it sounds higher and shorter. If two of them ever came to rest at the same rate, the entitlement would be selling paint. The shelf plays each body's voice when you tap it, locked or not: the difference is heard before it is paid for.

The plans screen is drawn in the app's own style over RevenueCat's SDK: three plain rows with the yearly price explained per month, a store trial said in plain words when one exists, Restore purchases, privacy and terms beside the button, and no countdown or "most popular" badge. A purchase unlocks on the spot. The release build is minified, signed from the environment and refuses the Test Store key, so the distance from here to charging real money is two store accounts and two keys — `docs/MONETIZATION_LAUNCH.md` is the checklist.

### Built with

Kotlin Multiplatform and Compose Multiplatform. The UI, timer, planner, physics and audio synthesis are all in `commonMain`; the platform source sets hold only what genuinely differs — haptics, persistence, app selection. RevenueCat's KMP SDK handles offerings, purchase, restore and the Pro entitlement; the plans screen itself is drawn in the app's own style.

It runs on Android and iPhone from one codebase. The iOS build lives in GitHub Actions because the only Mac available runs Monterey: a hosted Mac compiles the shared Kotlin for Apple, builds the Xcode project, boots a simulator and photographs the app on every push. The shared Kotlin compiled for iOS on the first attempt.

### Design decisions worth knowing

- **Hue carries state.** Coral is tension, sage is focus, lavender is interruption — across the whole product. Materials therefore never repaint the body; they differ by finish.
- **Motion has a grammar.** Going deeper rises, going back falls, overlays travel their own axis, and an interruption arrives with no motion at all, because being pulled out of a distraction should feel like a stop.
- **The first screen is the product.** No permission request, no list of apps. One body and nothing to read before you touch it.
- **Reduced motion is honoured.** The body still deforms under a finger — that is the interaction — but nothing moves that you did not move.
- **The copy is tested.** No exclamation marks, no failure or streak language, nothing longer than one line. The rules are assertions.

### Honesty

The planner is deterministic and on-device. It does not learn and there is no model behind it. The interface says "adaptive focus", never "AI".

---

## Built with (tags)

kotlin, kotlin-multiplatform, compose-multiplatform, revenuecat, android, ios, swiftui, github-actions

## Category selections

- Next Gen Award
- RevenueCat Design Award
- HAMM Award
- Ship Kotlin Everywhere (JetBrains)

## Links

- Repository: https://github.com/tapaderuza/squishflow
- iOS build proof: https://github.com/tapaderuza/squishflow/actions/workflows/ios.yml

## Files to attach

- `submission/app-icon-1024.png` — app icon, 1024 × 1024
- `submission/screenshots/01…08.png` — eight screenshots at 1179 × 2556, no device frame, in the order a judge should read them
- Demo video — see `submission/VIDEO.md`
