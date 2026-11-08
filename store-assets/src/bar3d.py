"""앱(GoldBar.kt)과 같은 방식으로 두께 있는 골드바를 원근 투영해 PNG로 만든다.

앞면 그림(bar.py의 front)은 Chrome으로 평면 렌더링한 뒤 원근 변형으로 입히고,
둥근 테두리를 따라 이어진 옆면은 면 방향에 따라 빛을 계산해 칠한다.
"""
import io, math, pathlib, sys
import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from bar import DEFS, front

W, H = 200.0, 300.0
THICK = 30.0
CAM_DIST = 900.0
EDGE_DARK = np.array([0x57, 0x38, 0x05], float)
EDGE_LIGHT = np.array([0xED, 0xC1, 0x52], float)
SPEC = np.array([0xFF, 0xF4, 0xCF], float)


def _norm(v):
    v = np.array(v, float)
    return v / np.linalg.norm(v)


L = _norm([-0.45, -0.65, 0.62])
HV = _norm([L[0], L[1], L[2] + 1])


def outline(seg=10, r=14.0):
    hw, hh = W / 2, H / 2
    corners = [((-hw + r, -hh + r), 180), ((hw - r, -hh + r), 270), ((hw - r, hh - r), 0), ((-hw + r, hh - r), 90)]
    pts = []
    for (cx, cy), start in corners:
        for k in range(seg + 1):
            a = math.radians(start + 90 * k / seg)
            pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


class Slab:
    def __init__(self, yaw, pitch, scale, origin):
        self.cy, self.sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
        self.cp, self.sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
        self.s, self.o = scale, origin

    def rot(self, x, y, z):
        x1 = x * self.cy + z * self.sy
        z1 = -x * self.sy + z * self.cy
        return np.array([x1, y * self.cp - z1 * self.sp, y * self.sp + z1 * self.cp])

    def project(self, x, y, z):
        p = self.rot(x, y, z)
        k = CAM_DIST / (CAM_DIST - p[2])
        return (self.o[0] + p[0] * k * self.s, self.o[1] + p[1] * k * self.s)

    def faces(self, n, p):
        n = self.rot(*n); p = self.rot(*p)
        return float(n @ (np.array([0, 0, CAM_DIST]) - p)) > 0

    def edge_color(self, n):
        n = self.rot(*n)
        diff = max(0.0, float(n @ L))
        spec = max(0.0, float(n @ HV)) ** 8
        t = min(1.0, 0.18 + 0.82 * diff)
        c = EDGE_DARK + (EDGE_LIGHT - EDGE_DARK) * t
        c = c + (SPEC - c) * min(1.0, spec * 0.4)
        return tuple(int(round(v)) for v in c) + (255,)


_face_cache = {}


def face_png(weight, grams, serial, px_w=1000):
    """앞면을 투명 배경 PNG로 (Chrome 렌더링)."""
    key = (weight, grams, serial, px_w)
    if key in _face_cache:
        return _face_cache[key]
    from playwright.sync_api import sync_playwright
    px_h = int(px_w * H / W)
    html = (f'<!doctype html><html><head><meta charset="utf-8"><style>html,body{{margin:0;background:transparent}}</style></head><body>'
            f'<svg width="{px_w}" height="{px_h}" viewBox="0 0 200 300" xmlns="http://www.w3.org/2000/svg"><defs>{DEFS}</defs>'
            f'{front(weight=weight, grams=grams, serial=serial)}</svg></body></html>')
    with sync_playwright() as p:
        b = p.chromium.launch(executable_path="/usr/bin/google-chrome", args=["--no-sandbox"])
        pg = b.new_page(viewport={"width": px_w, "height": px_h})
        pg.set_content(html); pg.wait_for_timeout(300)
        data = pg.screenshot(omit_background=True, clip={"x": 0, "y": 0, "width": px_w, "height": px_h})
        b.close()
    img = Image.open(io.BytesIO(data)).convert("RGBA")
    _face_cache[key] = img
    return img


def _persp_coeffs(dst, src):
    """출력 좌표(dst) → 입력 좌표(src) 원근 변환 계수 (PIL PERSPECTIVE)."""
    A, B = [], []
    for (x, y), (u, v) in zip(dst, src):
        A.append([x, y, 1, 0, 0, 0, -u * x, -u * y]); B.append(u)
        A.append([0, 0, 0, x, y, 1, -v * x, -v * y]); B.append(v)
    return np.linalg.solve(np.array(A, float), np.array(B, float)).tolist()


def render_bar(size_px, yaw=-22, pitch=-12, fill=0.86, weight="1냥 3돈", grams="48.75g",
               serial="No. HD000013", ss=3):
    """정사각 size_px 캔버스 가운데에 골드바를 그린 RGBA 이미지. fill = 골드바 높이 비율."""
    S = size_px * ss
    scale = S * fill / (H * 1.08)
    slab = Slab(yaw, pitch, scale, (S / 2, S / 2))
    half = THICK / 2

    # 실제로 그려질 범위를 먼저 구해서 가운데로 맞춘다
    pts = []
    for (x, y) in outline():
        pts += [slab.project(x, y, half), slab.project(x, y, -half)]
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    dx = S / 2 - (min(xs) + max(xs)) / 2
    dy = S / 2 - (min(ys) + max(ys)) / 2
    slab.o = (S / 2 + dx, S / 2 + dy)

    canvas = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(canvas)
    ol = outline()
    n = len(ol)
    for i in range(n):
        (x0, y0), (x1, y1) = ol[i], ol[(i + 1) % n]
        tx, ty = x1 - x0, y1 - y0
        ln = math.hypot(tx, ty)
        if ln < 1e-3:
            continue
        nx, ny = ty / ln, -tx / ln
        if not slab.faces((nx, ny, 0), ((x0 + x1) / 2, (y0 + y1) / 2, 0)):
            continue
        quad = [slab.project(x0, y0, half), slab.project(x1, y1, half),
                slab.project(x1, y1, -half), slab.project(x0, y0, -half)]
        col = slab.edge_color((nx, ny, 0))
        d.polygon(quad, fill=col, outline=col)

    show_front = slab.faces((0, 0, 1), (0, 0, half))
    z = half if show_front else -half
    xl = -W / 2 if show_front else W / 2
    xr = -xl
    dst = [slab.project(xl, -H / 2, z), slab.project(xr, -H / 2, z),
           slab.project(xr, H / 2, z), slab.project(xl, H / 2, z)]
    face = face_png(weight, grams, serial)
    fw, fh = face.size
    src = [(0, 0), (fw, 0), (fw, fh), (0, fh)]
    warped = face.transform((S, S), Image.PERSPECTIVE, _persp_coeffs(dst, src), Image.BICUBIC)
    canvas.alpha_composite(warped)
    return canvas.resize((size_px, size_px), Image.LANCZOS)


if __name__ == "__main__":
    out = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else "/tmp/bar3d.png")
    render_bar(600).save(out)
    print("saved", out)
