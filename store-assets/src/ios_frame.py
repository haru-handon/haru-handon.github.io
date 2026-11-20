"""iOS 시뮬레이터 원본(store-assets/raw-ios)에 문구를 얹어 App Store 6.9인치 스크린샷(1290×2796)을 만든다.
python3 store-assets/src/ios_frame.py  →  ios/store/screenshots/*.png (ios-store-info 워크플로가 올린다)"""
import pathlib
from playwright.sync_api import sync_playwright
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[2]
RAW = ROOT / "store-assets" / "raw-ios"
OUT = ROOT / "ios" / "store" / "screenshots"
OUT.mkdir(parents=True, exist_ok=True)
W, H = 1290, 2796

SHOTS = [
    ("01", ["i01-closed"], "하루 한 번, 금고를 여세요", "다이얼을 돌리면 묵직한 금고 문이 열려요"),
    ("02", ["i00-open-3"], "열 때마다 금 1돈", "골드바 각인에 무게가 차곡차곡 쌓여요"),
    ("03", ["i09-back"], "뒷면에는 福", "잡고 돌리면 행복하세요, 돈 많이 버세요"),
    ("04", ["i07-dogam"], "행운 문구 60개 도감", "다 모은 카테고리는 금고 문에 라벨로"),
    ("05", ["i10-nyang"], "10돈이 모이면 1냥", "금빛 축하와 함께 금고도 업그레이드돼요"),
    ("06", ["widget-home", "widget-lock"], "홈·잠금 화면 위젯", "골드바와 오늘의 문구를 바로 확인해요"),
]

STYLE = f"""html,body{{margin:0;width:{W}px;height:{H}px;overflow:hidden;background:#101113}}
.wrap{{position:relative;width:{W}px;height:{H}px;
  background:radial-gradient(70% 45% at 50% 62%, #3a311f 0%, #191816 55%, #101113 85%)}}
.cap{{position:absolute;left:0;right:0;top:190px;text-align:center}}
h1{{margin:0;font-family:"Noto Serif CJK KR",serif;font-weight:900;font-size:104px;line-height:1.15;
  background:linear-gradient(180deg,#fff6d2 10%,#f0c75a 60%,#c08e28 100%);-webkit-background-clip:text;background-clip:text;color:transparent}}
p{{margin:30px 0 0;font-family:"Noto Sans CJK KR",sans-serif;font-weight:500;font-size:50px;color:#ede7d7;opacity:.88}}
.phone{{position:absolute;left:50%;top:590px;width:960px;height:2087px;transform:translateX(-50%);
  border-radius:120px;padding:18px;background:#050506;
  box-shadow:0 0 0 3px #2c3036, 0 50px 100px rgba(0,0,0,.7), 0 0 170px rgba(220,174,66,.18)}}
.phone img{{display:block;width:100%;height:100%;border-radius:104px;object-fit:cover}}
.wid{{position:absolute;left:50%;top:560px;transform:translateX(-50%);display:flex;flex-direction:column;gap:44px;align-items:center}}
.wid img{{display:block;border-radius:48px;box-shadow:0 0 0 3px #2c3036, 0 40px 80px rgba(0,0,0,.6)}}
"""

def page(head, sub, body):
    return f'<!doctype html><html><head><meta charset="utf-8"><style>{STYLE}</style></head><body><div class="wrap"><div class="cap"><h1>{head}</h1><p>{sub}</p></div>{body}</div></body></html>'

with sync_playwright() as pw:
    b = pw.chromium.launch(executable_path="/usr/bin/google-chrome", args=["--no-sandbox", "--allow-file-access-from-files"])
    pg = b.new_page(viewport={"width": W, "height": H}, device_scale_factor=1)
    for num, raws, head, sub in SHOTS:
        srcs = [RAW / f"{r}.png" for r in raws]
        if not all(s.exists() for s in srcs):
            print("missing", raws); continue
        if len(srcs) == 1:
            body = f'<div class="phone"><img src="{srcs[0].as_uri()}"></div>'
        else:
            body = (f'<div class="wid"><img src="{srcs[0].as_uri()}" style="width:820px">'
                    f'<img src="{srcs[1].as_uri()}" style="width:900px"></div>')
        tmp = OUT / f"_{num}.html"
        tmp.write_text(page(head, sub, body), encoding="utf-8")
        pg.goto(tmp.as_uri())
        pg.wait_for_timeout(400)
        out = OUT / f"{num}.png"
        pg.screenshot(path=str(out), clip={"x": 0, "y": 0, "width": W, "height": H})
        tmp.unlink()
        Image.open(out).convert("RGB").save(out)  # App Store는 알파 채널 없는 PNG
        print("saved", out.name)
    b.close()
