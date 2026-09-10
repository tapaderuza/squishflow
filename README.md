# Squish — Your AI Focus Companion

Squish turns a free-form goal into a small, reviewable focus mission. Its companion reflects the current session, and the next duration adapts to a one-tap reflection.

## Current vertical slice

`Goal → Mission plan → Review → Focus block → Reflection → Adapted next block`

- Kotlin Multiplatform and Compose Multiplatform shared UI/logic.
- Monotonic timer with `TENSE`, `RELAXING`, and `COMPRESSED` states.
- Offline local planner with a boundary for a future server-backed model.
- Explainable adaptation: easy `+5m`, right `same`, too much `-5m` (10–60m).
- RevenueCat KMP paywall and `squish_pro` entitlement boundary.
- Android conscious-pause protection and on-device pose-based Rescue Mode.

The current planner is deterministic and on-device. Do not claim that it learns or uses a generative model. A remote AI provider must be called through a backend; API secrets must never ship in mobile binaries.

## Build and test

```powershell
.\gradlew.bat :shared:allTests :androidApp:assembleDebug --console=plain
```

Before store submission, replace the RevenueCat placeholders, configure offerings, sign release builds, and run sandbox purchases on physical Android and iOS devices.

## Shipaton focus

Primary categories: Ship Kotlin Everywhere, RevenueCat Design Award, HAMM Award, and #BuildInPublic. See [`docs/SHIPATON_STRATEGY.md`](docs/SHIPATON_STRATEGY.md).
