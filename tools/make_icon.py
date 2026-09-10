# -*- coding: utf-8 -*-
"""Render the Squishflow launcher icon.

The icon is the companion itself, drawn with the same palette and the same
radial gradient the app uses, so the thing on the home screen is recognisably
the thing inside the app.

Everything is generated rather than hand-drawn, which keeps the whole icon set
reproducible: rerun this after any palette change instead of re-exporting by
hand from a design tool.

    python tools/make_icon.py

Outputs the 1024x1024 store icon, the Android adaptive foreground and
background layers, and the legacy mipmap densities.
"""
import math
import os
from PIL import Image, ImageDraw, ImageFilter

# Palette, matching Theme.kt.
GROUND = (12, 14, 13)
BODY_LIGHT = (255, 158, 138)
BODY_MID = (255, 128, 108)
BODY_DEEP = (185, 71, 59)
FACE = (21, 23, 19)

SUPERSAMPLE = 4


# --- A minimal port of SquishyPhysics, so the icon is the real silhouette -----
#
# The launcher icon should not be a circle with a face. What makes the companion
# recognisable is that it deforms, so the outline here is produced by the same
# soft-body model the app runs: a ring of coupled radial samples, pressed once
# and integrated to the moment of deepest deformation.

POINTS = 26
TAU = 2 * math.pi


def squished_ring(press_angle=2.25, depth=0.40, spread=0.58,
                  stiffness=118.0, damping=7.1, coupling=46.0,
                  max_disp=0.42, hold_frames=10, settle_frames=1, dt=1.0 / 60.0):
    """Return POINTS radii, in units of the base radius.

    A thumb resting on the body calls press() once per frame, so holding is
    simulated the same way. The icon is captured at the peak of that hold rather
    than after the body has settled, because a settled body is just a circle.
    """
    disp = [0.0] * POINTS
    vel = [0.0] * POINTS

    def angle_of(i):
        return TAU * i / POINTS

    def angular_distance(a, b):
        d = (a - b) % TAU
        if d > math.pi:
            d -= TAU
        return abs(d)

    two_sigma_sq = 2 * spread * spread

    def press():
        for i in range(POINTS):
            w = math.exp(-(angular_distance(angle_of(i), press_angle) ** 2) / two_sigma_sq)
            disp[i] += (-depth * w - disp[i]) * 0.35 * w

    def integrate():
        lap = [
            disp[(i - 1) % POINTS] + disp[(i + 1) % POINTS] - 2 * disp[i]
            for i in range(POINTS)
        ]
        for i in range(POINTS):
            acc = -stiffness * disp[i] - damping * vel[i] + coupling * lap[i]
            vel[i] += acc * dt
            disp[i] += vel[i] * dt
        mean = sum(disp) / POINTS
        for i in range(POINTS):
            disp[i] = max(-max_disp, min(max_disp, disp[i] - mean))

    for _ in range(hold_frames):
        press()
        integrate()
    for _ in range(settle_frames):
        integrate()

    return [1.0 + d for d in disp]


def catmull_rom(points, steps=14):
    """Smooth closed curve through every sample, as the renderer does."""
    n = len(points)
    out = []
    for i in range(n):
        p0 = points[(i - 1) % n]
        p1 = points[i]
        p2 = points[(i + 1) % n]
        p3 = points[(i + 2) % n]
        c1 = (p1[0] + (p2[0] - p0[0]) / 6.0, p1[1] + (p2[1] - p0[1]) / 6.0)
        c2 = (p2[0] - (p3[0] - p1[0]) / 6.0, p2[1] - (p3[1] - p1[1]) / 6.0)
        for s in range(steps):
            t = s / float(steps)
            u = 1 - t
            out.append((
                u ** 3 * p1[0] + 3 * u * u * t * c1[0] + 3 * u * t * t * c2[0] + t ** 3 * p2[0],
                u ** 3 * p1[1] + 3 * u * u * t * c1[1] + 3 * u * t * t * c2[1] + t ** 3 * p2[1],
            ))
    return out


def silhouette(cx, cy, radius, squash_y=0.97):
    radii = squished_ring()
    pts = []
    for i, r in enumerate(radii):
        a = TAU * i / POINTS
        pts.append((cx + math.sin(a) * r * radius,
                    cy - math.cos(a) * r * radius * squash_y))
    return catmull_rom(pts)



def radial_body(size, cx, cy, radius):
    """The body as an RGBA layer: a radial gradient clipped to a soft blob."""
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    pixels = layer.load()

    # Light source sits up and to the left, as it does in SquishyCanvas.
    lx, ly = cx - radius * 0.34, cy - radius * 0.40
    falloff = radius * 1.55

    left, top = int(cx - radius * 1.3), int(cy - radius * 1.3)
    right, bottom = int(cx + radius * 1.3), int(cy + radius * 1.3)

    for y in range(max(0, top), min(size, bottom)):
        for x in range(max(0, left), min(size, right)):
            t = math.hypot(x - lx, y - ly) / falloff
            t = min(1.0, max(0.0, t))
            if t < 0.5:
                k = t / 0.5
                c = tuple(
                    int(BODY_LIGHT[i] + (BODY_MID[i] - BODY_LIGHT[i]) * k) for i in range(3)
                )
            else:
                k = (t - 0.5) / 0.5
                c = tuple(
                    int(BODY_MID[i] + (BODY_DEEP[i] - BODY_MID[i]) * k) for i in range(3)
                )
            pixels[x, y] = c + (255,)

    # Clip to the real soft-body outline at its moment of deepest deformation.
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).polygon(silhouette(cx, cy, radius), fill=255)
    layer.putalpha(mask)
    return layer, mask


def draw_face(draw, cx, cy, radius):
    eye_r = radius * 0.115
    eye_y = cy - radius * 0.08
    gap = radius * 0.30
    for sign in (-1, 1):
        ex = cx + sign * gap
        draw.ellipse([ex - eye_r, eye_y - eye_r, ex + eye_r, eye_y + eye_r], fill=FACE)
        # Catchlight, up and left to match the gradient.
        s = eye_r * 0.32
        draw.ellipse(
            [ex - eye_r * 0.30 - s, eye_y - eye_r * 0.34 - s,
             ex - eye_r * 0.30 + s, eye_y - eye_r * 0.34 + s],
            fill=(255, 255, 255, 235),
        )

    # A calm smile: an arc, not a full circle.
    mw, mh = radius * 0.34, radius * 0.26
    my = cy + radius * 0.13
    draw.arc(
        [cx - mw / 2, my - mh / 2, cx + mw / 2, my + mh / 2],
        start=18, end=162, fill=FACE, width=int(radius * 0.075),
    )


def gloss(size, cx, cy, radius):
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    gw, gh = radius * 0.62, radius * 0.30
    gx, gy = cx - radius * 0.30, cy - radius * 0.52
    d.ellipse([gx - gw / 2, gy - gh / 2, gx + gw / 2, gy + gh / 2], fill=(255, 255, 255, 96))
    return layer.filter(ImageFilter.GaussianBlur(radius * 0.055))


def render(size, with_ground=True, body_scale=0.72):
    s = size * SUPERSAMPLE
    canvas = Image.new("RGBA", (s, s), GROUND + (255,) if with_ground else (0, 0, 0, 0))

    cx, cy = s / 2, s / 2 + s * 0.012
    radius = s * body_scale / 2

    body, mask = radial_body(s, cx, cy, radius)

    # Contact shadow, so the body sits on the ground rather than floating.
    if with_ground:
        shadow = Image.new("RGBA", (s, s), (0, 0, 0, 0))
        ImageDraw.Draw(shadow).ellipse(
            [cx - radius * 0.78, cy + radius * 0.80,
             cx + radius * 0.78, cy + radius * 1.02],
            fill=(0, 0, 0, 130),
        )
        canvas = Image.alpha_composite(canvas, shadow.filter(ImageFilter.GaussianBlur(s * 0.018)))

    canvas = Image.alpha_composite(canvas, body)

    highlight = gloss(s, cx, cy, radius)
    highlight.putalpha(Image.composite(highlight.getchannel("A"), Image.new("L", (s, s), 0), mask))
    canvas = Image.alpha_composite(canvas, highlight)

    face = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    draw_face(ImageDraw.Draw(face), cx, cy, radius)
    canvas = Image.alpha_composite(canvas, face)

    return canvas.resize((size, size), Image.LANCZOS)


def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    res = os.path.join(root, "androidApp", "src", "androidMain", "res")

    store = os.path.join(root, "androidApp", "src", "androidMain", "ic_launcher-playstore.png")
    render(1024).convert("RGB").save(store)
    print("store icon      ->", os.path.relpath(store, root))

    submission = os.path.join(root, "evidence", "app_icon_1024.png")
    render(1024).convert("RGB").save(submission)
    print("devpost icon    ->", os.path.relpath(submission, root))

    # Adaptive icon: Android masks the outer third, so the body is drawn smaller
    # on a transparent foreground and the ground becomes its own layer.
    for density, px in [("mdpi", 108), ("hdpi", 162), ("xhdpi", 216),
                        ("xxhdpi", 324), ("xxxhdpi", 432)]:
        folder = os.path.join(res, "mipmap-%s" % density)
        os.makedirs(folder, exist_ok=True)
        # Android masks a 108dp foreground down to a ~72dp visible circle with a
        # 66dp safe zone. 0.60 fills that safe zone; smaller values leave the body
        # looking lost inside the mask.
        render(px, with_ground=False, body_scale=0.60).save(
            os.path.join(folder, "ic_launcher_foreground.webp"), "WEBP", lossless=True
        )

    for density, px in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96),
                        ("xxhdpi", 144), ("xxxhdpi", 192)]:
        folder = os.path.join(res, "mipmap-%s" % density)
        os.makedirs(folder, exist_ok=True)
        icon = render(px)
        icon.save(os.path.join(folder, "ic_launcher.webp"), "WEBP", lossless=True)

        round_mask = Image.new("L", (px, px), 0)
        ImageDraw.Draw(round_mask).ellipse([0, 0, px - 1, px - 1], fill=255)
        rounded = icon.copy()
        rounded.putalpha(round_mask)
        rounded.save(os.path.join(folder, "ic_launcher_round.webp"), "WEBP", lossless=True)

    print("mipmaps         -> androidApp/src/androidMain/res/mipmap-*")


if __name__ == "__main__":
    main()
