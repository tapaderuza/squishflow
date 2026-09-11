#!/usr/bin/env python3
"""Walk the app on an iOS simulator and photograph every main screen.

Runs in CI on a hosted Mac, where nobody can tap. Navigation goes through the
accessibility tree via idb: each step finds an element by its label and taps its
centre, so the walkthrough survives layout changes that would break coordinate
scripts, and it exercises the same labels a screen-reader user depends on.

    python3 tools/ios_walkthrough.py <udid> <bundle-id> <output-dir>
"""
import json
import subprocess
import sys
import time

UDID, BUNDLE, OUT = sys.argv[1], sys.argv[2], sys.argv[3]


def idb(*args, capture=False):
    # --udid is a per-subcommand option in idb, not a global one.
    cmd = ["idb", *args, "--udid", UDID]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        # Surface the reason: a swallowed stderr costs a full CI round trip.
        print("idb failed:", " ".join(cmd), file=sys.stderr)
        print(result.stderr.strip(), file=sys.stderr)
        raise SystemExit(result.returncode)
    return result.stdout if capture else None


def shot(name):
    subprocess.run(["xcrun", "simctl", "io", UDID, "screenshot", f"{OUT}/{name}.png"], check=True)
    print("  shot", name)


def elements():
    raw = idb("ui", "describe-all", "--json", capture=True)
    # idb emits one JSON object per line in some versions and an array in others.
    try:
        data = json.loads(raw)
        return data if isinstance(data, list) else [data]
    except json.JSONDecodeError:
        return [json.loads(line) for line in raw.splitlines() if line.strip()]


def find(label_part, attempts=12, pause=1.0):
    """Wait for an element whose label or title contains [label_part]."""
    for _ in range(attempts):
        for el in elements():
            haystack = " ".join(
                str(el.get(k, "")) for k in ("AXLabel", "AXValue", "title", "AXUniqueId")
            )
            if label_part.lower() in haystack.lower() and el.get("frame"):
                return el
        time.sleep(pause)
    raise SystemExit(f"never found an element containing {label_part!r}")


def tap(label_part, times=1, pause=1.2):
    el = find(label_part)
    f = el["frame"]
    x = f["x"] + f["width"] / 2
    y = f["y"] + f["height"] / 2
    for _ in range(times):
        idb("ui", "tap", str(x), str(y))
        time.sleep(pause)
    print(f"  tapped {label_part!r} x{times} at ({x:.0f},{y:.0f})")


def type_text(text):
    idb("ui", "text", text)
    time.sleep(0.6)


print("launching")
subprocess.run(["xcrun", "simctl", "terminate", UDID, BUNDLE], check=False)
subprocess.run(["xcrun", "simctl", "launch", UDID, BUNDLE, "-squishy"], check=True)
time.sleep(7)
shot("01-welcome")

print("meeting squishy")
tap("Squishy, your focus companion", times=3, pause=1.6)
time.sleep(1.5)
shot("02-welcome-settled")
tap("Now tell it what to protect")
time.sleep(2)

print("choosing a distraction category")
shot("03-distractions")
tap("Social")
tap("Meet Squishy")
time.sleep(2.5)

# The protection prompt may or may not appear depending on the platform actual.
for el in elements():
    if "timer only" in str(el.get("AXLabel", "")).lower():
        tap("timer only")
        time.sleep(2)
        break

print("planning")
shot("04-planner")
tap("e.g. Finish")
type_text("Finish the client deck and study statistics")
time.sleep(0.8)
tap("Create my mission")
time.sleep(3)
shot("05-plan")

print("starting the first block")
tap("Start the first block")
time.sleep(3.5)
shot("06-focus")

print("squeezing")
el = find("Squishy, your focus companion")
f = el["frame"]
cx, cy = f["x"] + f["width"] / 2, f["y"] + f["height"] / 2
idb("ui", "swipe", str(cx - 90), str(cy - 30), str(cx + 80), str(cy + 60), "--duration", "0.9")
time.sleep(0.15)
shot("07-focus-deformed")
time.sleep(2)

print("the shelf")
tap("blocks")
time.sleep(2.5)
shot("08-journey")

print("done")
