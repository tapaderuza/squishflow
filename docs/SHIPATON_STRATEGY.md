# Squishflow — Shipaton 2026 strategy

Rewritten 12 September 2026 after reading the 2024 and 2025 results, the 2026
rules and RevenueCat's own preparation guide. Sources at the end.

## What the last two years reward

- **Small and single-purpose beat sprawling.** RevenueCat says it in its prep
  guide, and the winners agree: Momental (KMP 1st, 2025) is a one-tap
  meditation timer; Flowmino (Design 1st, 2024) is time blocks plus Screen
  Time. Nobody won with a platform.
- **Design winners are "delightful to look at and use": animation, gesture,
  haptics.** Flowmino won on "gentle animations throughout, combined with
  fantastic use of haptics". Dayloop (2025) on precision engineering and a
  privacy-first stance. This is exactly Squishflow's lane: a physics body,
  a synthesised voice, pressure-tied haptics, a copy voice held by tests.
- **HAMM winners have a *story* about money, not just a paywall.** Vector
  Guard's "1:50 justice model" (each premium funds 50 free accounts) won
  2025; the criteria ask for clarity, diversity of streams, and a reason the
  model is *ours*. Squishflow's story: focus is the currency, every body is
  earnable, Pro is the shortcut plus one utility (protect every app), and
  the release refuses a fake store.
- **Context-aware paywall messaging** was singled out for SkillMe (Design
  2nd, 2025). The pitch now opens with the sentence the moment deserves.
- **The video should show the purchase moment prominently** (prep guide).
  Ours shows the shelf, the six-second trial and the plans at 1:00–1:16.
- **Most losses come from missing the store deadline, not weak apps.**

## The hard requirement most categories share

Every category except Next Gen needs the app **published on the App Store,
Google Play or the Samsung Galaxy Store between 1 August and 30 September
2026**, plus a free trial or a promo code for judges.

Where that leaves us on 12 September, 18 days out:

| Route | Cost | Feasible by 30 Sep? | Why |
|---|---|---|---|
| **Next Gen (student)** | 0 | **Yes, today** | Video + public repo. Already complete. |
| **App Store via CI** | 99 $/yr | **Yes, if enrolment starts now** | Enrolment 1–3 days (ID check in the Apple Developer app on the iPhone), first review 1–3 days. `ios-release.yml` archives and uploads with cloud signing; no capable Mac needed. |
| Google Play | 25 $ | **No** | New personal accounts must run a closed test with 12 testers for 14 continuous days before production. Start it anyway for a later real launch. |
| Samsung Galaxy Store | 0 | Unclear | Counts as "published" in the rules, but RevenueCat has no Samsung billing; purchases would need Web Billing and Samsung's policy on external payment is untested. Not worth the risk. |

**Decision:** Next Gen is the floor. The App Store is the one door that opens
HAMM, Design and the Grand Prize in time. Kotlin Multiplatform Reach needs
both stores, so it is out for 2026 unless Play's tester gate is already
running.

## Category map

| Category | Fit | What the submission must say |
|---|---|---|
| Next Gen | Primary | Student built, open source, video. |
| Design Award | Strong | Physics body, per-material voice, wake-up / stretch / last minute, gesture + haptics, tested copy. Needs the App Store release. |
| HAMM | Strong story | Focus is the currency; Pro = now + protect every app; three price points; release refuses the Test Store; trial surfaced in plain words; restore, privacy, terms. Needs the release. |
| Grand Prize | Long shot | Traction during the event. Only if the App Store release lands early and something is posted about it. |
| Ship Kotlin Everywhere | Out this year | Requires both stores live. Say in the text that the codebase is one and the Android build is store-ready. |

## Video (2:00) — what is in it now and why

0:00 hook · 0:07 the problem · 0:17 planner · 0:30 the physics body ·
0:48 it settles over a block · 0:59 shelf, six-second trial, pitch, plans
(the purchase moment) · 1:16 earned body, "yours to keep" · 1:25 one
codebase, iPhone and Android · 1:40 synthesised voices per material ·
1:52 the name.

Regenerate with `submission/video/narrate.py` (cues) → `record_take2.py`
(shelf take, on the emulator) → `build.py`.

## How people buy it

In-app, through the store they installed from: monthly, yearly (preselected,
per-month price explained, a store trial shown when configured) or
lifetime. The plans screen is the app's own; RevenueCat handles offerings,
purchase, restore and the entitlement. Nothing is sold outside the stores;
nothing is gated that was earned. `docs/MONETIZATION_LAUNCH.md` is the
step-by-step for accounts, products and keys.

## Sources

- RevenueCat, "Shipaton 2025 Winners": https://www.revenuecat.com/blog/company/shipaton-2025-winners
- RevenueCat, "2024 Ship-a-ton Winners": https://www.revenuecat.com/blog/company/2024-ship-a-ton-winners
- Shipaton 2026 rules on Devpost: https://revenuecat-shipaton-2026.devpost.com/rules
- RevenueCat Codelabs, "Shipaton 2026 Preparation Guide": https://revenuecat.github.io/codelabs/shipaton-2026-prep.html
- Flowmino on Devpost (Design Award 2024): https://devpost.com/software/flowmino
