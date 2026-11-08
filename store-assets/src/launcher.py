"""휴대폰 홈 화면 아이콘(적응형 아이콘 전경)을 3D 골드바 PNG로 만든다.  python3 store-assets/src/launcher.py"""
import pathlib, sys
import numpy as np
from PIL import Image

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from bar3d import render_bar

RES = pathlib.Path(__file__).resolve().parents[2] / "android/app/src/main/res"
DENSITIES = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}

base = render_bar(1296, yaw=0, pitch=-14, fill=0.55)

# 은은한 금빛 후광 (108dp 기준 반지름 34dp)
S = base.size[0]
yy, xx = np.mgrid[0:S, 0:S]
r = np.hypot(xx - S / 2, yy - S / 2) / (S * 34 / 108)
a = np.clip(1 - r, 0, 1) ** 1.6 * 0.33 * 255
halo = np.zeros((S, S, 4), np.uint8)
halo[..., 0], halo[..., 1], halo[..., 2] = 0xFB, 0xE7, 0xA6
halo[..., 3] = a.astype(np.uint8)
fg = Image.alpha_composite(Image.fromarray(halo, "RGBA"), base)

for name, px in DENSITIES.items():
    d = RES / f"mipmap-{name}"
    d.mkdir(parents=True, exist_ok=True)
    fg.resize((px, px), Image.LANCZOS).save(d / "ic_launcher_foreground.png", optimize=True)
    print("saved", name, px)
