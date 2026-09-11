# Squishflow product decisions

## Identity

- Public name: **Squishflow**
- Store title: **Squishflow: AI Focus Coach**
- Short name: **Squishflow**
- Tagline: **Turn big goals into focus you can finish.**
- Spanish tagline: **Convierte grandes objetivos en foco que sí puedes terminar.**
- Primary language: English; first localization: Spanish.
- Initial market: Spain, followed by English-speaking markets.
- Android package and iOS bundle: `com.alvaropassalacqua.squishflow`.
- Developer: Álvaro Passalacqua.
- Support: `alvarotorero98@gmail.com`; privacy: `apaspar8@gmail.com`.

Squishflow avoids the already crowded “Squish AI” name while preserving Squishy as the mascot.

## Audience and brand

- Audience: students and knowledge workers, **16–35**.
- Problem: large or vague work creates friction; rigid Pomodoros ignore current capacity.
- Personality: calm, observant, adorable and slightly clumsy; never clinical or judgmental.
- Desired emotion: relief followed by quiet momentum.
- Visual qualities: tactile, calm, distinctive.
- Palette: ink/cream foundation with coral tension, sage focus and lavender interruption.
- Avoid: neon productivity dashboards, red failure states, glassmorphism and excessive gradients.
- References: Endel (calm), Finch (companion), Opal (focus), without copying their UI.
- Premium collections: Kawaii Animals, Comfort Food and Cosmic Blobs.
- Soundscapes: rain on glass, quiet café and soft brown noise.

## Experience

- Default focus durations: 15, 25 and 45 minutes.
- Default break: 5 minutes; 10 after two blocks.
- Free maximum: 25 minutes. Deep Focus (45–60) is Pro.
- Pause: no; allow explicit cancellation with neutral confirmation.
- Calls and system dialogs never count as failure.
- Plans may be edited, reordered and trimmed before starting.
- Reflection: “How did that block feel?” — Easy / Right / Too much.
- Offer Deep Focus after three successful blocks or 90 focused minutes that day.
- Shorten after “Too much” or two early abandonments.
- First paywall: after the third completed block, never before first value.
- Recurring paywall: fourth AI plan that day or a deliberate Pro feature selection.

## Free and Pro

Free: every block length, the full planner, the free body plus every body earned with focused minutes (60 / 180 / 420 / 900 min ladder), and protection for up to three apps.

Pro (monthly, yearly, lifetime): every body now, protection for any number of apps, and nothing else — no ads for anyone either way. A lapsed subscription keeps every body earned and every app already protected. Yearly is preselected; the row states the per-month price and there are no urgency devices.

RevenueCat uses the `default` offering, one entitlement (named `Squish Pro` in the dashboard; the code treats any active entitlement as Pro), and monthly, yearly and lifetime packages. Development currently uses RevenueCat Test Store; platform keys replace it before store release.

## AI architecture

- Free/offline: deterministic local planner and transparent rules.
- Pro: server-side mission planning and weekly review with structured JSON output.
- Mobile apps never contain provider secrets.
- Default infrastructure: Cloudflare Worker in the EU-facing deployment path and OpenAI Responses API.
- Do not retain raw goals by default; send only for the request and discard after response.
- Store derived blocks locally. Weekly review uses numeric aggregates, not raw goal text.
- Fallback to local planning on timeout, quota or offline state.
- Coach tone: serene, direct, concise and guilt-free.
- Languages: English and Spanish initially.

Use a small/cost-efficient current model selected at backend deployment time. The flagship model is unnecessary for this constrained planning schema.

## Privacy

- No mandatory account in MVP.
- Minimum intended age: 16. Do not list in the Kids category.
- Local-first storage and an in-app “Delete all local data” control.
- No third-party behavioral analytics in the first public build.
- RevenueCat receives purchase identifiers required for subscriptions.
- Camera pose processing remains on-device; images are never stored or uploaded.
- Accessibility Service and Family Controls are optional features with a prominent disclosure immediately before authorization.
- Goal text sent to Pro AI requires explicit, contextual consent and is not used for model training by Squishflow.

Legal/privacy drafts require owner review and hosting on public URLs before submission.

## Shipaton

- Team: Álvaro Passalacqua, solo founder.
- Categories: Ship Kotlin Everywhere, Design, HAMM, Build in Public and Grand Prize.
- Peace Prize only after credible social-impact evidence.
- Video: English voice-over with burned-in English subtitles; Spanish localized version optional.
- Initial targets: 300 beta users, 20 Pro customers and €100 MRR by submission.
