# Squish Shipaton 2026 strategy

## Product claim

> We didn't build another Pomodoro. We built a companion that turns a vague goal into focus blocks you can actually complete.

Until a real server-backed model is deployed, say **adaptive on-device planner**, not “learns how you focus”. Squishy reflects session state; it never diagnoses mental state.

## Design contract

- The mission is visible and reviewable before the timer starts.
- Squishy communicates state using form, motion, label, and color.
- Failure language is neutral; returning matters more than perfect streaks.
- Reflection takes one tap and visibly changes the next recommendation.
- Reduced-motion and screen-reader behavior are release gates.

## Monetization

Free: base Squishy, manual sessions, seven-day history, and three adaptive plans per day. Pro: unlimited planning/adaptation, full insights, Deep Focus, soundscapes, and personality packs.

Show the paywall only after value: third completed session, fourth adaptive plan, old history, or a selected Pro cosmetic. Never interrupt an active session or celebration. Offer monthly and annual plans through `squish_pro`.

## Metrics

North Star: completed focus sessions per weekly active user.

Events (never attach free-form goal text): `mission_created`, `mission_accepted`, `focus_started`, `focus_completed`, `focus_abandoned`, `session_rated`, `recommendation_accepted`, `recommendation_changed`, `paywall_viewed`, `purchase_started`, `purchase_completed`, and `purchase_restored`.

Initial targets: activation >55%, completion >65%, time-to-first-focus <30 seconds, and crash-free sessions >99%.

## Two-minute demo

1. Hook: “A goal is usually too large to start.”
2. Type a client presentation + exam goal.
3. Reveal and approve the generated mission.
4. Start block one; Squishy shifts from tense coral to calm sage.
5. Trigger a protected-app conscious pause and choose to return.
6. Complete the accelerated session and rate it “too much.”
7. Reveal the shorter next recommendation with its explanation.
8. Show RevenueCat Pro unlocking unlimited adaptive plans and personality packs.
9. End on Android and iOS: “One shared Kotlin companion. Two stores.”

The first two minutes must show the product in use and name Design, HAMM, Kotlin Everywhere, and Build in Public.

## Build in public

Publish decisions and measured learning: goal decomposition, Squishy states, duration experiments, respectful paywall, KMP architecture, and anonymized metrics. Use `#Shipaton` and `#BuildInPublic`.

## Hard release gate

- Real RevenueCat keys, offerings, purchase and restore tested.
- Signed Android AAB and App Store/TestFlight build.
- Mission/session persistence across process death.
- Real-device background, interruption and clock tests.
- TalkBack/VoiceOver, dynamic text, contrast and reduced motion.
- Privacy policy covering Accessibility, camera, purchases and analytics.
- Current screenshots and complete Android/iOS walkthrough.
