# -*- coding: utf-8 -*-
"""The narration, one cue per beat.

Each cue carries the second it starts and the line spoken. Silence is
deliberate: the squish has its own sound and the narration gets out of its way.
The tone matches the app's — no exclamation marks, short sentences, no streak
or hustle language — because a demo that shouts for a product built on calm
would argue with itself.
"""

VOICE = "en-GB-SoniaNeural"
RATE = "-8%"

CUES = [
    # (start_seconds, text)
    (7.0,  "Every focus app asks you to be disciplined at exactly the moment you have none left. "
           "Squishflow puts something soft between you and the work."),

    (17.0, "You say what you want to finish. It hands back blocks short enough to believe. "
           "The first one is shorter on purpose. Starting is the expensive part."),

    (30.0, "This is not an animation. It is a physics model. Twenty-six points, real stiffness, "
           "real damping. Press one side, and the other side bulges."),

    (48.0, "And over a block, it relaxes. Softer at minute twenty-four than at minute one. "
           "It does the thing it asks of you."),

    (59.0, "Five bodies. Every one can be earned with focused minutes. Pro just means you do not wait. "
           "Tap a locked one and you get six seconds to feel it before anyone mentions money."),

    (76.0, "Earn one, and it is yours. Even if you cancel."),

    (85.0, "One Kotlin codebase. The same companion on Android and iPhone. "
           "The iOS build runs in the cloud on every push, because the only Mac we had could not."),

    (100.0, "No audio files ship. The squelch is synthesised. The copy has no exclamation marks, by test. "
            "The icon is the physics."),

    (111.0, "Squishflow. Focus should not take force."),
]

# Captions shown on the left of the frame, timed to the beats. Kept to a few
# words each: they label what is on screen, they do not narrate it twice.
CAPTIONS = [
    (0.0,   7.0,   ""),
    (7.0,   17.0,  "A focus timer\nyou can squeeze"),
    (17.0,  30.0,  "A goal becomes\nblocks you can believe"),
    (30.0,  48.0,  "A soft-body model.\nNot an animation."),
    (48.0,  59.0,  "It relaxes\nas you focus"),
    (59.0,  76.0,  "Every body\ncan be earned"),
    (76.0,  85.0,  "Yours to keep"),
    (85.0,  100.0, "One codebase.\nAndroid and iPhone."),
    (100.0, 111.0, "Synthesised voice.\nTested copy.\nPhysics icon."),
    (111.0, 120.0, "github.com/tapaderuza/squishflow"),
]
