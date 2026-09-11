# -*- coding: utf-8 -*-
"""Re-record take 2 — the shelf, a locked body tried, the plans — on the emulator.

    python submission/video/record_take2.py

Assumes the app is installed on the running emulator with a mission and some
history in place, and that the current RevenueCat user is not Pro (otherwise
nothing is locked and there is no trial to film). Timings below are what
build.py's SEGMENTS and GESTURES assume for this take.
"""
import os
import subprocess
import time

import imageio_ffmpeg

ADB = os.path.expanduser(r"~\AppData\Local\Android\Sdk\platform-tools\adb.exe")
PKG = "com.alvaropassalacqua.squishflow"
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "takes", "take2_shelf.mp4")


def adb(*args):
    subprocess.run([ADB, *args], check=False, capture_output=True)


def tap(x, y):
    adb("shell", "input", "tap", str(x), str(y))


def press(x, y, ms=420):
    adb("shell", "input", "swipe", str(x), str(y), str(x + 30), str(y + 40), str(ms))


def main():
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "monkey", "-p", PKG, "-c", "android.intent.category.LAUNCHER", "1")
    time.sleep(3)

    rec = subprocess.Popen(
        [ADB, "shell", "screenrecord", "--time-limit", "32", "--bit-rate", "12000000", "/sdcard/take2.mp4"],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    t0 = time.time()

    def at(seconds):
        delay = t0 + seconds - time.time()
        if delay > 0:
            time.sleep(delay)

    at(4.5);  tap(540, 1440)       # "Jelly ›" opens the shelf
    at(8.0);  tap(540, 1820)       # Water balloon: locked, so a six-second trial
    at(9.5);  press(500, 1000)     # squeeze it while it is in the hand
    at(11.5); press(600, 1080)
    at(13.5); press(520, 1040)
    # ~14.5 the trial ends and the upgrade pitch appears
    at(18.5); tap(540, 2108)       # See Premium plans
    # 19.5-26 the plans, in the app's own clothes
    at(26.0); tap(918, 190)        # Not now
    rec.wait()

    raw = OUT + ".raw.mp4"
    adb("pull", "/sdcard/take2.mp4", raw)
    adb("shell", "rm", "/sdcard/take2.mp4")

    # screenrecord only emits a frame when the screen changes, so a static
    # screen is a gap in the stream, and a segment cut from inside that gap
    # shows nothing until the next change. Fill it to a constant 30 fps.
    subprocess.run(
        [imageio_ffmpeg.get_ffmpeg_exe(), "-y", "-loglevel", "error", "-i", raw,
         "-vf", "fps=30", "-c:v", "libx264", "-preset", "medium", "-crf", "16", "-pix_fmt", "yuv420p", OUT],
        check=True,
    )
    os.remove(raw)
    print("->", os.path.relpath(OUT))


if __name__ == "__main__":
    main()
