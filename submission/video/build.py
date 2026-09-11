# -*- coding: utf-8 -*-
"""Assemble the two-minute Devpost video from the recorded takes.

Everything is generated: the takes come from the emulator driven over adb, the
narration from a neural voice, and the squishes are the app's own synthesised
samples placed at the instant of each gesture — Android's screen recorder
captures no audio, so this is also the only way the sound gets in at all.

    python submission/video/build.py

Layout is one dark canvas (the app's own ground) with the phone right of centre
and a few words of caption on the left. No music: the product is built on calm,
and a demo that scores itself would argue with it.
"""
import os
import subprocess
import sys

import imageio_ffmpeg

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from script import CAPTIONS, CUES  # noqa: E402

FF = imageio_ffmpeg.get_ffmpeg_exe()
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
TAKES = os.path.join(HERE, "takes")
WORK = os.path.join(HERE, "work")
OUT = os.path.join(HERE, "squishflow-demo.mp4")

W, H = 1920, 1080
GROUND = "0x0C0E0D"
INK = "0xF2F0E9"
MUTED = "0x969991"
SAGE = "0x8DD6AA"
FONT = "C\\:/Windows/Fonts/segoeuil.ttf"
FONT_BOLD = "C\\:/Windows/Fonts/seguisb.ttf"

# Phone footage is 1080x2400. At 1000px tall it keeps the app's type legible on a
# laptop screen while leaving the left third free for captions.
PHONE_H = 1000
PHONE_W = 450
PHONE_X = 1130
PHONE_Y = (H - PHONE_H) // 2

T1 = os.path.join(TAKES, "take1_main.mp4")
T2 = os.path.join(TAKES, "take2_shelf.mp4")
T3 = os.path.join(TAKES, "take3_unlock.mp4")
IOS = os.path.join(ROOT, "evidence", "ios", "ios-idle.mp4")
TRANSITIONS = os.path.join(ROOT, "evidence", "video", "03_transitions.mp4")
ICON = os.path.join(ROOT, "evidence", "app_icon_1024.png")
# The app's own voices, written by the unit tests. Each material has its own,
# so a gesture on the water balloon must not be dubbed with jelly.
AUDIO = os.path.join(ROOT, "shared", "build", "audio-preview")
VOICES = ["jelly", "water-balloon", "mochi"]

# The timeline. (video_start, duration, source, source_in). Sources are trimmed
# to length and concatenated in order; the sum of durations must be 120.
SEGMENTS = [
    (0,   8,  T1, 0.0),    # welcome, idle: it blinks
    (8,   9,  T1, 8.5),    # three squeezes, settles to sage
    (17,  13, T1, 31.0),   # planner: typing, the plan appears
    (30,  18, T1, 42.5),   # start the block, squeeze, drag, drag
    (48,  11, T1, 60.0),   # the settled body under a slow deep drag
    (59,  17, T2, 5.0),    # the shelf, a locked body tried, the pitch, the plans
    (76,  9,  T3, 6.5),    # EARNED: Mochi. Hold it. Block complete.
    # 85-100 is the two-platform beat, built separately below
    (100, 11, None, 0.0),  # montage, built separately below
    (111, 9,  T3, 29.0),   # at rest, holding the Mochi
]

# Gestures in the final timeline, for the squish samples: (second, material).
# Each press gets a squeeze and, a beat later, a release.
GESTURES = [
    (8.6, "jelly"), (11.5, "jelly"), (14.6, "jelly"),            # the three welcome squeezes
    (38.0, "jelly"), (40.7, "jelly"), (43.4, "jelly"), (46.1, "jelly"),  # in-session drags
    (49.6, "jelly"), (52.0, "jelly"), (56.0, "jelly"),           # the slow deep drag and a tap
    (63.5, "water-balloon"), (65.5, "water-balloon"), (67.5, "water-balloon"),  # trying the water balloon
    (80.5, "mochi"),                                            # holding the Mochi
]


def run(*args):
    subprocess.run([FF, "-y", "-loglevel", "error", *args], check=True)


def phone_on_canvas(src, start, dur, out):
    """One segment: the phone footage placed on the ground with rounded corners."""
    run(
        "-ss", f"{start:.2f}", "-t", f"{dur:.2f}", "-i", src,
        "-f", "lavfi", "-t", f"{dur:.2f}", "-i", f"color=c={GROUND}:s={W}x{H}:r=30",
        "-filter_complex",
        f"[0:v]scale={PHONE_W}:{PHONE_H}:flags=lanczos,format=rgba,"
        f"geq=r='r(X,Y)':g='g(X,Y)':b='b(X,Y)':"
        f"a='if(lt(abs(X-{PHONE_W}/2),{PHONE_W}/2-38)+lt(abs(Y-{PHONE_H}/2),{PHONE_H}/2-38)"
        f"+lt(hypot(abs(X-{PHONE_W}/2)-({PHONE_W}/2-38),abs(Y-{PHONE_H}/2)-({PHONE_H}/2-38)),38),255,0)'[ph];"
        f"[1:v][ph]overlay={PHONE_X}:{PHONE_Y}:format=auto,fps=30,format=yuv420p[v]",
        "-map", "[v]", "-an", "-c:v", "libx264", "-preset", "medium", "-crf", "18", out,
    )


def two_phones(out, dur=15.0):
    """Android and iPhone side by side, the same screen on both, both at rest."""
    run("-ss", "0", "-t", "8", "-i", T1, "-c", "copy", os.path.join(WORK, "android_idle.mp4"))
    run(
        "-stream_loop", "-1", "-t", f"{dur:.2f}", "-i", os.path.join(WORK, "android_idle.mp4"),
        "-stream_loop", "-1", "-t", f"{dur:.2f}", "-i", IOS,
        "-f", "lavfi", "-t", f"{dur:.2f}", "-i", f"color=c={GROUND}:s={W}x{H}:r=30",
        "-filter_complex",
        f"[0:v]scale=-2:880:flags=lanczos[a];"
        f"[1:v]scale=-2:880:flags=lanczos[b];"
        f"[2:v][a]overlay=(W/2)-w-40:(H-h)/2[c];"
        f"[c][b]overlay=(W/2)+40:(H-h)/2,fps=30,format=yuv420p[v]",
        "-map", "[v]", "-an", "-c:v", "libx264", "-preset", "medium", "-crf", "18", out,
    )


def montage(out):
    """Icon, then the transitions, then the companion at rest. Eleven seconds."""
    icon = os.path.join(WORK, "m_icon.mp4")
    trans = os.path.join(WORK, "m_trans.mp4")
    rest = os.path.join(WORK, "m_rest.mp4")
    run(
        "-loop", "1", "-t", "3", "-i", ICON,
        "-f", "lavfi", "-t", "3", "-i", f"color=c={GROUND}:s={W}x{H}:r=30",
        "-filter_complex",
        "[0:v]scale=560:560:flags=lanczos,format=rgba,"
        "geq=r='r(X,Y)':g='g(X,Y)':b='b(X,Y)':a='if(lt(hypot(X-280,Y-280),280),255,0)'[i];"
        "[1:v][i]overlay=(W-w)/2:(H-h)/2,fps=30,format=yuv420p[v]",
        "-map", "[v]", "-an", "-c:v", "libx264", "-crf", "18", icon,
    )
    phone_on_canvas(TRANSITIONS, 26.0, 5.0, trans)
    phone_on_canvas(T3, 25.0, 3.0, rest)
    with open(os.path.join(WORK, "montage.txt"), "w") as f:
        for p in (icon, trans, rest):
            f.write(f"file '{p}'\n")
    run("-f", "concat", "-safe", "0", "-i", os.path.join(WORK, "montage.txt"), "-c", "copy", out)


def caption_filter():
    parts = []
    for start, end, text in CAPTIONS:
        if not text:
            continue
        lines = text.split("\n")
        big = start >= 111.0
        for i, line in enumerate(lines):
            esc = line.replace("'", "\\'").replace(":", "\\:").replace(",", "\\,")
            size = 34 if big else 54
            y = 500 + i * (size + 14) - (len(lines) - 1) * (size + 14) // 2
            font = FONT_BOLD if big else FONT
            color = MUTED if big else INK
            parts.append(
                f"drawtext=fontfile='{font}':text='{esc}':fontsize={size}:fontcolor={color}:"
                f"x=150:y={y}:enable='between(t,{start},{end})':"
                f"alpha='if(lt(t,{start}+0.6),(t-{start})/0.6,if(gt(t,{end}-0.6),({end}-t)/0.6,1))'"
            )
    # A hairline in the accent, under the caption block, for the whole run.
    parts.append(f"drawbox=x=150:y=595:w=72:h=2:color={SAGE}@0.9:t=fill:enable='gte(t,7)'")
    return ",".join(parts)


def audio_filter(n_cues):
    """Narration and squishes on one bus, each delayed to its moment."""
    chains, labels = [], []
    for i, (t, _) in enumerate(CUES):
        chains.append(f"[{1 + i}:a]adelay={int(t * 1000)}|{int(t * 1000)},volume=1.0[n{i}]")
        labels.append(f"[n{i}]")
    first_voice = 1 + n_cues
    for k, (t, material) in enumerate(GESTURES):
        ms = int(t * 1000)
        # Inputs after the cues come in pairs per voice: squeeze, then release.
        sq = first_voice + 2 * VOICES.index(material)
        rl = sq + 1
        chains.append(f"[{sq}:a]adelay={ms}|{ms},volume=1.15[s{k}]")
        chains.append(f"[{rl}:a]adelay={ms + 850}|{ms + 850},volume=0.9[r{k}]")
        labels.extend([f"[s{k}]", f"[r{k}]"])
    mix = "".join(labels) + f"amix=inputs={len(labels)}:normalize=0:dropout_transition=0,alimiter=limit=0.95[a]"
    return ";".join(chains + [mix])


def main():
    os.makedirs(WORK, exist_ok=True)
    pieces = []
    for idx, (t, dur, src, s_in) in enumerate(SEGMENTS):
        out = os.path.join(WORK, f"seg{idx:02d}.mp4")
        if src is None:
            montage(out)
        else:
            phone_on_canvas(src, s_in, dur, out)
        pieces.append((t, out))
        if t == 76:
            two = os.path.join(WORK, "seg_two.mp4")
            two_phones(two)
            pieces.append((85, two))
        print("segment", idx, "at", t)

    pieces.sort()
    with open(os.path.join(WORK, "concat.txt"), "w") as f:
        for _, p in pieces:
            f.write(f"file '{p}'\n")
    silent = os.path.join(WORK, "video_silent.mp4")
    run("-f", "concat", "-safe", "0", "-i", os.path.join(WORK, "concat.txt"), "-c", "copy", silent)

    captioned = os.path.join(WORK, "video_captioned.mp4")
    run("-i", silent, "-vf", caption_filter(), "-an", "-c:v", "libx264", "-preset", "medium", "-crf", "18", captioned)

    inputs = ["-i", captioned]
    for i in range(len(CUES)):
        inputs += ["-i", os.path.join(HERE, "narration", f"cue{i:02d}.mp3")]
    for voice in VOICES:
        inputs += ["-i", os.path.join(AUDIO, f"squeeze-{voice}.wav"), "-i", os.path.join(AUDIO, f"release-{voice}.wav")]
    run(
        *inputs,
        "-filter_complex", audio_filter(len(CUES)),
        "-map", "0:v", "-map", "[a]", "-t", "120",
        "-c:v", "copy", "-c:a", "aac", "-b:a", "192k", "-movflags", "+faststart", OUT,
    )
    print("->", os.path.relpath(OUT, ROOT))


if __name__ == "__main__":
    main()
