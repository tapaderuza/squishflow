#!/usr/bin/env python3
"""Print the UDID of the best available iPhone simulator.

Prefers a Pro model so captures come from a device shape the store listing will
use, and falls back to any iPhone. Reads `xcrun simctl list devices --json` from
stdin so the caller controls the invocation.
"""
import json
import sys

devices = json.load(sys.stdin)["devices"]

available = [
    device
    for runtime in devices.values()
    for device in runtime
    if device.get("isAvailable") and "iPhone" in device["name"]
]

if not available:
    sys.exit("no available iPhone simulator")

preferred = [d for d in available if "Pro" in d["name"]] or available
# Newest runtime tends to sort last by name; pick the last Pro we saw.
print(preferred[-1]["udid"])
