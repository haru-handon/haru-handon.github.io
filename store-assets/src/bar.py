"""앱 속 압인 골드바와 같은 모양을 SVG로 그린다 (200×300 설계 좌표, 앱의 GoldBar.kt와 같은 값)."""
import math

DEFS = """
<linearGradient id="gPlate" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="200" y2="300">
  <stop offset="0" stop-color="#fff2c2"/><stop offset=".16" stop-color="#ebc25a"/><stop offset=".3" stop-color="#b7871f"/>
  <stop offset=".44" stop-color="#f6dc8c"/><stop offset=".58" stop-color="#d7a538"/><stop offset=".76" stop-color="#9a6b14"/>
  <stop offset=".9" stop-color="#d9ac45"/><stop offset="1" stop-color="#f3d27a"/></linearGradient>
<linearGradient id="gField" gradientUnits="userSpaceOnUse" x1="0" y1="300" x2="200" y2="0">
  <stop offset="0" stop-color="#b98523"/><stop offset=".3" stop-color="#e7bd55"/><stop offset=".5" stop-color="#fbe5a0"/>
  <stop offset=".68" stop-color="#e2b44a"/><stop offset="1" stop-color="#a7771b"/></linearGradient>
<linearGradient id="gBevel" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="200" y2="300">
  <stop offset="0" stop-color="#fffae6"/><stop offset=".5" stop-color="#d9a940"/><stop offset="1" stop-color="#6e4a0b"/></linearGradient>
<linearGradient id="gBevelIn" gradientUnits="userSpaceOnUse" x1="200" y1="300" x2="0" y2="0">
  <stop offset="0" stop-color="#fff6d2"/><stop offset=".5" stop-color="#c8952e"/><stop offset="1" stop-color="#5f3f08"/></linearGradient>
<linearGradient id="gSideDark" x1="0" y1="0" x2="1" y2="0">
  <stop offset="0" stop-color="#a36f14"/><stop offset=".5" stop-color="#8a5c0e"/><stop offset="1" stop-color="#5e3d06"/></linearGradient>
<linearGradient id="gBottom" x1="0" y1="0" x2="0" y2="1">
  <stop offset="0" stop-color="#a8741a"/><stop offset="1" stop-color="#5e3d06"/></linearGradient>
<linearGradient id="gRefl" x1="0" y1="0" x2="1" y2="0">
  <stop offset="0" stop-color="#fff" stop-opacity="0"/><stop offset=".45" stop-color="#fff" stop-opacity=".5"/>
  <stop offset=".55" stop-color="#fff" stop-opacity=".5"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient>
<clipPath id="plateClip"><rect width="200" height="300" rx="14"/></clipPath>
"""

SERIF = "Noto Serif CJK KR, Noto Serif KR, serif"
SANS = "Noto Sans CJK KR, Noto Sans KR, sans-serif"
MONO = "DejaVu Sans Mono, monospace"


def engraved(x, y, text, size, family, weight, spacing=0):
    out = []
    for dx, dy, fill, op in [(0.7, 0.7, "#6b4708", .55), (-0.5, -0.5, "#fff8dc", .85), (0, 0, "#a87818", 1)]:
        out.append(
            f'<text x="{x+dx}" y="{y+dy}" text-anchor="middle" font-family="{family}" font-weight="{weight}" '
            f'font-size="{size}" letter-spacing="{spacing}" fill="{fill}" opacity="{op}">{text}</text>')
    return "".join(out)


def engraved_shape(tag, attrs):
    out = []
    for dx, dy, col, op in [(0.7, 0.7, "#6b4708", .55), (-0.5, -0.5, "#fff8dc", .85), (0, 0, "#a87818", 1)]:
        out.append(f'<{tag} {attrs} transform="translate({dx} {dy})" stroke="{col}" opacity="{op}" fill="none"/>')
    return "".join(out)


def front(weight="1냥 3돈", grams="48.75g", serial="No. HD000013", refl_x=80):
    s = []
    s.append('<rect width="200" height="300" rx="14" fill="url(#gPlate)"/>')
    s.append('<rect x="1" y="1" width="198" height="298" rx="13.5" fill="none" stroke="#fff7d8" stroke-opacity=".6"/>')
    s.append('<rect x="11" y="11" width="178" height="278" rx="8" fill="none" stroke="url(#gBevel)" stroke-width="3"/>')
    s.append('<rect x="14" y="14" width="172" height="272" rx="6" fill="url(#gField)" stroke="url(#gBevelIn)" stroke-width="1.6"/>')
    s.append(engraved_shape("circle", 'cx="100" cy="58" r="25" stroke-width="2"'))
    s.append(engraved_shape("circle", 'cx="100" cy="58" r="20" stroke-width=".8"'))
    s.append(engraved(100, 67, "金", 25, SERIF, 900))
    s.append(engraved(100, 108, "FINE GOLD", 10, SANS, 700, 4))
    s.append(engraved(100, 143, "999.9", 31, SERIF, 900, 1))
    s.append(engraved_shape("line", 'x1="50" x2="150" y1="157" y2="157" stroke-width="1.2"'))
    s.append(engraved(100, 191, weight, 25, SERIF, 900))
    s.append(engraved(100, 211, grams, 10.5, SANS, 500, 1.5))
    s.append(engraved(100, 248, "하루 한돈", 10, SERIF, 700, 3))
    s.append(engraved(100, 266, serial, 8, MONO, 500, 1.2))
    s.append(f'<g clip-path="url(#plateClip)"><g transform="skewX(-22)">'
             f'<rect x="{refl_x-40}" y="-40" width="54" height="380" fill="url(#gRefl)"/>'
             f'<rect x="{refl_x+22}" y="-40" width="16" height="380" fill="url(#gRefl)" opacity=".7"/></g></g>')
    return "".join(s)


def bar_group(yaw_deg=-14, thick=22, bottom=10, **kw):
    """앱과 같은 방식: 앞면은 |cos|만큼 좁아지고, 옆면은 T·|sin| 폭으로 붙는다."""
    c = math.cos(math.radians(yaw_deg)); sn = math.sin(math.radians(yaw_deg))
    cx = 100
    face_cx = cx + thick / 2 * sn
    half = 100 * abs(c)
    side_w = thick * abs(sn)
    g = []
    if side_w > 0.3:
        side_cx = cx + (100 * c if sn < 0 else -100 * c)
        l = side_cx - side_w / 2
        g.append(f'<rect x="{l}" y="0" width="{side_w}" height="{300+bottom}" rx="{min(side_w/2,12)}" fill="url(#gSideDark)"/>')
    g.append(f'<rect x="{face_cx-half}" y="{300-14}" width="{half*2}" height="{14+bottom}" rx="{min(14*abs(c), half)}" ry="14" fill="url(#gBottom)"/>')
    g.append(f'<g transform="translate({face_cx-half} 0) scale({abs(c)} 1)">{front(**kw)}</g>')
    return "".join(g)
