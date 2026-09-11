# -*- coding: utf-8 -*-
"""Produce the screenshots Devpost asks for: 1179 x 2556, no device frame.

The captures on disk come from a Pixel 6 (1080 x 2400) and an iPhone 16 Pro
(1320 x 2868). Neither is the requested size, and a plain resize would change the
aspect ratio. Each source is centre-cropped to the target aspect first — the
loss is a few dozen pixels of margin — and only then scaled, so nothing on
screen is stretched.

    python tools/make_submission_shots.py
"""
import os
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "submission", "screenshots")
TARGET = (1179, 2556)

# Order matters: Devpost shows them in sequence, so this is the story a judge
# reads before pressing play on the video.
SHOTS = [
    ("evidence/screen_welcome.png", "01-meet-squishy.png"),
    ("evidence/screen_deform.png", "02-it-pushes-back.png"),
    ("evidence/screen_plan.png", "03-a-goal-becomes-blocks.png"),
    ("evidence/screen_focus.png", "04-the-focus-screen.png"),
    ("evidence/unlock.png", "05-earning-a-body.png"),
    ("evidence/screen_journey.png", "06-the-shelf.png"),
    ("evidence/plans.png", "07-the-plans.png"),
    ("evidence/ios/ios-welcome.png", "08-same-app-on-iphone.png"),
]


def fit(source: Image.Image) -> Image.Image:
    """Centre-crop to the target aspect, then scale. Never stretches."""
    target_ratio = TARGET[0] / TARGET[1]
    w, h = source.size
    if w / h > target_ratio:
        new_w = int(round(h * target_ratio))
        left = (w - new_w) // 2
        source = source.crop((left, 0, left + new_w, h))
    else:
        new_h = int(round(w / target_ratio))
        top = (h - new_h) // 2
        source = source.crop((0, top, w, top + new_h))
    return source.resize(TARGET, Image.LANCZOS)


def main() -> None:
    os.makedirs(OUT, exist_ok=True)
    for src_rel, name in SHOTS:
        src = os.path.join(ROOT, src_rel)
        if not os.path.exists(src):
            print("missing:", src_rel)
            continue
        image = Image.open(src).convert("RGB")
        fit(image).save(os.path.join(OUT, name), "PNG", optimize=True)
        print("%-32s <- %s" % (name, src_rel))
    print("->", os.path.relpath(OUT, ROOT))


if __name__ == "__main__":
    main()
