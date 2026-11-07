"""에뮬레이터 원본(store-assets/raw)에 문구를 얹어 Play 스크린샷(1080×1920)을 만든다.
python3 store-assets/src/frame.py"""
import pathlib
from playwright.sync_api import sync_playwright

ROOT = pathlib.Path(__file__).resolve().parents[1]
RAW = ROOT / "raw"
OUT = ROOT / "out" / "screenshots"
OUT.mkdir(parents=True, exist_ok=True)

SHOTS = [
    ("01", "01-closed", "하루 한 번, 금고를 여세요", "다이얼을 돌리면 묵직한 금고 문이 열려요"),
    ("02", "02-open", "열 때마다 금 1돈", "골드바 각인에 무게가 차곡차곡 쌓여요"),
    ("03", "03-back", "뒷면에는 福", "잡고 돌리면 행복하세요, 돈 많이 버세요"),
    ("04", "04-nyang", "10돈이 모이면 1냥", "금빛 축하와 함께 금고도 업그레이드돼요"),
]

TEMPLATE = """<!doctype html><html><head><meta charset="utf-8"><style>
html,body{margin:0;width:1080px;height:1920px;overflow:hidden;background:#101113}
.wrap{position:relative;width:1080px;height:1920px;
  background:radial-gradient(70% 45% at 50% 62%, #3a311f 0%, #191816 55%, #101113 85%)}
.cap{position:absolute;left:0;right:0;top:118px;text-align:center}
h1{margin:0;font-family:"Noto Serif CJK KR",serif;font-weight:900;font-size:86px;letter-spacing:-.01em;line-height:1.15;
  background:linear-gradient(180deg,#fff6d2 10%,#f0c75a 60%,#c08e28 100%);-webkit-background-clip:text;background-clip:text;color:transparent}
p{margin:22px 0 0;font-family:"Noto Sans CJK KR",sans-serif;font-weight:500;font-size:40px;color:#ede7d7;opacity:.88}
.phone{position:absolute;left:50%;top:430px;width:812px;height:1444px;transform:translateX(-50%);
  border-radius:58px;padding:14px;background:#050506;
  box-shadow:0 0 0 2px #2c3036, 0 40px 80px rgba(0,0,0,.7), 0 0 140px rgba(220,174,66,.18)}
.phone img{display:block;width:100%;height:100%;border-radius:44px;object-fit:cover}
</style></head><body><div class="wrap">
<div class="cap"><h1>__H__</h1><p>__P__</p></div>
<div class="phone"><img src="__IMG__"></div>
</div></body></html>"""

with sync_playwright() as pw:
    b = pw.chromium.launch(executable_path="/usr/bin/google-chrome", args=["--no-sandbox", "--allow-file-access-from-files"])
    pg = b.new_page(viewport={"width": 1080, "height": 1920}, device_scale_factor=1)
    for num, raw, head, sub in SHOTS:
        src = RAW / f"{raw}.png"
        if not src.exists():
            print("missing", src); continue
        html = TEMPLATE.replace("__H__", head).replace("__P__", sub).replace("__IMG__", src.as_uri())
        tmp = OUT / f"_{num}.html"
        tmp.write_text(html, encoding="utf-8")
        pg.goto(tmp.as_uri())
        pg.wait_for_timeout(300)
        pg.screenshot(path=str(OUT / f"{num}.png"), clip={"x": 0, "y": 0, "width": 1080, "height": 1920})
        tmp.unlink()
        print("saved", num)
    b.close()
