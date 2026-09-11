# -*- coding: utf-8 -*-
"""Render the narration cues in script.py to narration/cueNN.mp3.

    python submission/video/narrate.py          # every cue
    python submission/video/narrate.py 7        # one cue, after editing its text

Uses a neural voice through edge-tts; the voice and rate live in script.py so
the sound of the demo is decided in one place.
"""
import asyncio
import os
import sys

import edge_tts

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from script import CUES, RATE, VOICE  # noqa: E402

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "narration")


async def render(index):
    text = CUES[index][1]
    path = os.path.join(OUT, f"cue{index:02d}.mp3")
    await edge_tts.Communicate(text, VOICE, rate=RATE).save(path)
    print("cue", index, "->", os.path.relpath(path))


async def main(indices):
    os.makedirs(OUT, exist_ok=True)
    for i in indices:
        await render(i)


if __name__ == "__main__":
    wanted = [int(a) for a in sys.argv[1:]] or list(range(len(CUES)))
    asyncio.run(main(wanted))
