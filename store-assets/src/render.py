"""스토어 아이콘(512)과 그래픽 이미지(1024×500)를 만든다.  python3 store-assets/src/render.py"""
import pathlib, sys
sys.path.insert(0, str(pathlib.Path(__file__).parent))
from bar import DEFS, bar_group
from bar3d import render_bar
import base64, io
from playwright.sync_api import sync_playwright

OUT = pathlib.Path(__file__).resolve().parents[1] / "out"
OUT.mkdir(exist_ok=True)


def data_uri(img):
    buf = io.BytesIO(); img.save(buf, "PNG")
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()


ICON_BAR = data_uri(render_bar(1024, yaw=-22, pitch=-12, fill=0.86))
FEATURE_BAR = data_uri(render_bar(900, yaw=-28, pitch=-10, fill=0.9))

ICON = f"""<!doctype html><html><head><meta charset="utf-8"><style>
html,body{{margin:0;width:512px;height:512px;overflow:hidden;background:#1e2125;position:relative}} svg{{position:absolute;left:0;top:0}}</style></head><body>
<svg width="512" height="512" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg">
<defs>{DEFS}
<radialGradient id="bg" cx=".5" cy=".42" r=".75"><stop offset="0" stop-color="#3a3f46"/><stop offset=".6" stop-color="#22262b"/><stop offset="1" stop-color="#141619"/></radialGradient>
<radialGradient id="halo" cx=".5" cy=".5" r=".5"><stop offset="0" stop-color="#fbe7a6" stop-opacity=".42"/><stop offset="1" stop-color="#fbe7a6" stop-opacity="0"/></radialGradient>
<filter id="sh" x="-30%" y="-30%" width="160%" height="160%"><feDropShadow dx="0" dy="14" stdDeviation="14" flood-color="#000" flood-opacity=".55"/></filter>
</defs>
<rect width="512" height="512" fill="url(#bg)"/>
<circle cx="256" cy="256" r="236" fill="url(#halo)"/>
</svg>
<img src="{ICON_BAR}" style="position:absolute;left:0;top:0;width:512px;height:512px;filter:drop-shadow(0 14px 16px rgba(0,0,0,.55))">
</body></html>"""

FEATURE = f"""<!doctype html><html><head><meta charset="utf-8"><style>
html,body{{margin:0;width:1024px;height:500px;overflow:hidden;background:#101113}}
.wrap{{position:relative;width:1024px;height:500px;
  background:radial-gradient(60% 90% at 26% 52%, #3b3220 0%, #1b1a18 45%, #101113 80%)}}
img.bar{{position:absolute;left:36px;top:25px;width:450px;height:450px;filter:drop-shadow(0 26px 30px rgba(0,0,0,.6)) drop-shadow(0 0 50px rgba(251,231,166,.28))}}
.rays{{position:absolute;left:246px;top:260px;width:900px;height:900px;margin:-450px 0 0 -450px;border-radius:50%;
  background:repeating-conic-gradient(rgba(251,231,166,.07) 0 6deg,transparent 6deg 18deg);
  -webkit-mask:radial-gradient(circle,#000 0 18%,transparent 55%);mask:radial-gradient(circle,#000 0 18%,transparent 55%)}}
.txt{{position:absolute;left:500px;top:0;bottom:0;right:48px;display:flex;flex-direction:column;justify-content:center;gap:14px}}
.eyebrow{{font-family:"Noto Sans CJK KR",sans-serif;font-size:21px;letter-spacing:.22em;color:#dcae42;font-weight:500}}
h1{{margin:0;font-family:"Noto Serif CJK KR",serif;font-weight:900;font-size:104px;line-height:1.02;letter-spacing:-.02em;
  background:linear-gradient(180deg,#fff6d2 8%,#f0c75a 55%,#b0801f 95%);-webkit-background-clip:text;background-clip:text;color:transparent;
  filter:drop-shadow(0 4px 18px rgba(220,174,66,.35))}}
.sub{{font-family:"Noto Sans CJK KR",sans-serif;font-size:26px;color:#ede7d7;font-weight:500}}
.units{{display:flex;gap:22px;margin-top:10px;font-family:"Noto Sans CJK KR",sans-serif;font-size:18px;color:#9c9585}}
.units b{{color:#fbe7a6;font-family:"Noto Serif CJK KR",serif;font-weight:900;font-size:22px;margin-right:6px}}
</style></head><body><div class="wrap">
<div class="rays"></div>
<img class="bar" src="{FEATURE_BAR}">
<div class="txt">
  <div class="eyebrow">행복하세요 · 돈 많이 버세요</div>
  <h1>하루 한돈</h1>
  <div class="sub">매일 쌓이는 행운의 골드바</div>
  <div class="units"><span><b>1돈</b>하루 한 번</span><span><b>1냥</b>10돈 달성 축하</span><span><b>福</b>오늘의 덕담</span></div>
</div></div></body></html>"""

with sync_playwright() as p:
    b = p.chromium.launch(executable_path="/usr/bin/google-chrome", args=["--no-sandbox"])
    for name, html, w, h in [("play-icon-512", ICON, 512, 512), ("play-feature-graphic-1024x500", FEATURE, 1024, 500)]:
        pg = b.new_page(viewport={"width": w, "height": h}, device_scale_factor=1)
        pg.set_content(html)
        pg.wait_for_timeout(400)
        pg.screenshot(path=str(OUT / f"{name}.png"), clip={"x": 0, "y": 0, "width": w, "height": h})
        print("saved", name)
    b.close()
