# The two-minute video

**Delivered:** `submission/video/squishflow-demo.mp4` — 2:00, 1920×1080, 30 fps.

It is generated end to end by `submission/video/build.py`: the takes come from
the emulator driven over adb, the narration from a neural voice
(`en-GB-SoniaNeural`, script in `script.py`), and the squishes are the app's own
synthesised samples placed at the instant of each gesture — Android's screen
recorder captures no audio, so this is also the only way the sound gets in.

To change a line, a caption or a cue time, edit `script.py` and rerun
`build.py`. To re-record a take, the adb sequences are in the session notes;
the three takes cover onboarding-to-focus, the shelf and trial, and the unlock.

**Upload it to YouTube as unlisted** and paste the link into the Devpost form.

---

## The original shot list

Kept for reference; the delivered cut follows it.



- **Record on a physical Android phone**, screen-recorded. Not the emulator: the
  physics runs at real frame rate on hardware and looks noticeably better.
- **No talking head, no narration** over the squish. Let the sound of the app
  carry. Voiceover only for the two structural beats marked below.
- **Turn the phone's media volume up** before recording, so the synthesised
  squish is audible in the capture.
- Landscape framing of a portrait phone wastes half the frame. Record portrait,
  then place it on a plain dark background (#0C0E0D, the app's ground) when you
  edit, so the video and the app share one surface.

## Shot list

| Time | Shot | Why it is here |
| --- | --- | --- |
| 0:00 – 0:08 | Cold open on the welcome screen. Wait two seconds. **Let it blink.** Then press your thumb into it — slowly — and let go. Do it twice more. | The whole product in eight seconds, with no words. A judge who watches nothing else has seen the thing. |
| 0:08 – 0:15 | The welcome resolves to sage. Tap through. | The state colour changing is the first design decision on screen. |
| 0:15 – 0:30 | Type a real goal: *"Finish the client deck and study statistics"*. Show the plan. Point out nothing; just let the block titles read. | Shows the planner does not repeat the goal, and that the first block is shorter. |
| 0:30 – 0:50 | Start a block. Squeeze the body a few times mid-session. Drag it. **This is the money shot — spend time here.** | Haptics, sound, deformation, the ring filling. Everything the design award cites. |
| 0:50 – 1:00 | *Voiceover beat 1:* "Every squishy can be earned with focus. Pro just means you don't wait." Open the shelf from the header. Scroll it. | The monetisation model, stated once, plainly. |
| 1:00 – 1:12 | Tap a locked material. **Show the six-second trial** — squeeze it during the trial. Let the paywall appear. | Try-before-you-buy is the most distinctive thing in the model. |
| 1:12 – 1:25 | Cut to a block completing (have one pre-recorded finishing). The reflection screen, then — if you have footage — an unlock celebration. | The emotional peak, and the reward loop closing. |
| 1:25 – 1:40 | *Voiceover beat 2:* "One Kotlin codebase." Cut to the iPhone capture from CI (`evidence/ios/ios-welcome.png` or the idle video). Then the GitHub Actions run, green. | The Kotlin Multiplatform proof, with receipts rather than a claim. |
| 1:40 – 1:52 | Fast montage: the icon, the journey screen, a blink, a drag, the transitions between screens. No cuts shorter than 1.5s. | Density without chaos. |
| 1:52 – 2:00 | Hold on the welcome screen, body at rest. Repo URL as a caption. Fade. | End where you began. |

## What to have ready before you press record

1. A **fresh install** so the welcome screen appears (Settings → Apps → Squishflow → Clear storage).
2. **Seeded focus** if you want the shelf to show earned bodies rather than all-locked. Run one 5-minute block on camera, or seed it beforehand.
3. **One block that finishes** — start a 5-minute one before you begin recording the rest, so you can cut to its completion.
4. **Media volume up. Ringer not on silent.** The app defers to the silent switch by design.

## What to avoid

- Do not show the rescue mode with the camera. It is half-finished and the
  video should not point at it.
- Do not show the Accessibility permission flow. It is a system dialog and it
  is boring; the "timer only" path is the one to take on camera.
- Do not speed anything up. The physics is the product; sped-up physics looks
  like a bug.
