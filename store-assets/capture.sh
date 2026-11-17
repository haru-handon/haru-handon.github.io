#!/usr/bin/env bash
# 에뮬레이터에서 실제 앱 화면을 1080×1920으로 캡처한다 (스토어 스크린샷 원본)
set -euo pipefail
PKG=com.ohsang.util.haruhandon
APK=android/app/build/outputs/apk/debug/app-debug.apk
OUT=store-assets/raw
mkdir -p "$OUT"

adb shell wm size 1080x1920
adb shell wm density 420
adb shell settings put system screen_off_timeout 1800000
adb shell svc power stayon true

# 상태 표시줄을 깔끔하게 (8:00, 배터리 가득, 알림 아이콘 없음)
adb shell settings put global sysui_demo_allowed 1
demo() { adb shell am broadcast -a com.android.systemui.demo -e command "$@" >/dev/null; }
demo enter
demo clock -e hhmm 0800
demo battery -e level 100 -e plugged false
demo network -e wifi show -e level 4 -e mobile show -e datatype none -e level 4
demo notifications -e visible false

# 에뮬레이터 런처의 '응답 없음' 창이 화면을 가리지 않게
adb shell settings put global hide_error_dialogs 1
adb shell settings put global anr_show_background 0
sleep 25
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1 || true
adb shell input keyevent KEYCODE_HOME

adb install -r "$APK"
adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS || true

TODAY=$(adb shell date +%F | tr -d '\r')
echo "emulator today: $TODAY"

# 어제까지 연속 N일 출석한 상태로 맞춘다
seed() {
  local n=$1
  {
    echo "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>"
    echo "<map>"
    echo '    <boolean name="notify_asked" value="true" />'
    echo '    <boolean name="sound_on" value="true" />'
    echo '    <set name="attended_days">'
    for i in $(seq 1 "$n"); do
      echo "        <string>$(date -d "$TODAY -$i day" +%F)</string>"
    done
    echo '    </set>'
    echo "</map>"
  } > /tmp/handon.xml
  adb shell am force-stop "$PKG"
  adb push /tmp/handon.xml /data/local/tmp/handon.xml >/dev/null
  adb shell chmod 644 /data/local/tmp/handon.xml
  adb shell "run-as $PKG sh -c 'mkdir -p shared_prefs && cp /data/local/tmp/handon.xml shared_prefs/handon.xml'"
}

# 문구 도감: 출석 N일 + 모은 문구(id=횟수,...) + 다음에 뽑힐 문구
seed_dogam() {
  local n=$1 counts=$2 next=$3
  {
    echo "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>"
    echo "<map>"
    echo '    <boolean name="notify_asked" value="true" />'
    echo '    <boolean name="sound_on" value="true" />'
    echo "    <string name=\"fortune_counts\">$counts</string>"
    echo "    <int name=\"debug_next_fortune\" value=\"$next\" />"
    echo '    <set name="attended_days">'
    for i in $(seq 1 "$n"); do
      echo "        <string>$(date -d "$TODAY -$i day" +%F)</string>"
    done
    echo '    </set>'
    echo "</map>"
  } > /tmp/handon.xml
  adb shell am force-stop "$PKG"
  adb push /tmp/handon.xml /data/local/tmp/handon.xml >/dev/null
  adb shell chmod 644 /data/local/tmp/handon.xml
  adb shell "run-as $PKG sh -c 'mkdir -p shared_prefs && cp /data/local/tmp/handon.xml shared_prefs/handon.xml'"
}

# 화면에서 글자를 찾아 그 가운데를 누른다
tap_text() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  local xy
  xy=$(adb shell cat /sdcard/ui.xml | python3 -c "
import sys,re
t=sys.stdin.read()
for m in re.finditer(r'text=\"([^\"]*)\"[^>]*?bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]', t):
    if sys.argv[1] in m.group(1):
        a,b,c,d=map(int,m.groups()[1:]); print((a+c)//2,(b+d)//2); break
" "$1")
  echo "tap '$1' at $xy"
  [ -n "$xy" ] && adb shell input tap $xy
}

launch() {
  adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1 || true
  adb shell am start -W -n "$PKG/.MainActivity" >/dev/null
  sleep 5
  adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1 || true
  sleep 1
}

shot() {
  adb exec-out screencap -p > "$OUT/$1.png"
  echo "saved $1"
}

CX=540
CY=830

set +e

# 12~17: 문구 도감 — 재물 라벨은 이미 있고, 행운은 하나만 남은 상태에서 그 하나를 뽑는다
DOGAM="0=2,3=1,4=3,9=1,21=2,24=1,27=1,30=2,38=1,39=1,49=1,53=2,2=1,10=2,12=1,15=1,17=1,20=1,22=2,34=1,40=1,41=1,48=1,5=1,7=1,14=2,25=1,28=1,1=1,6=1,8=1,13=1,23=2,29=1,11=1,16=1,18=2,19=1"
seed_dogam 52 "$DOGAM" 55
launch
shot 12-dogam-closed
adb shell input tap $CX $CY
sleep 4.0
shot 13-dogam-open
sleep 2.4
shot 14-dogam-label
sleep 2
tap_text "금고 보관"
sleep 3
shot 18-dogam-after
tap_text "문구 도감"
sleep 1.5
shot 15-dogam-book
tap_text "인연"
sleep 1
shot 16-dogam-book2
# 이미 모은 문구가 또 나온 날
seed_dogam 52 "$DOGAM" 4
launch
adb shell input tap $CX $CY
sleep 6
shot 17-dogam-dup
# 다섯 카테고리를 다 모아 장식이 모두 달린 금고
ALL=$(python3 -c "print(','.join(f'{i}=1' for i in range(60)))")
seed_dogam 60 "$ALL" 0
launch
shot 19-dogam-full

# 1~2: 12일 모은 상태에서 오늘 열기 → 1냥 3돈
seed 12
launch
shot 01-closed
adb shell input tap $CX $CY
sleep 7
shot 02-open

# 3: 9일 모은 상태에서 열기 → 1냥 달성 축하
seed 9
launch
adb shell input tap $CX $CY
sleep 7.5
shot 04-nyang

# 4: 뒷면 (천천히 끌어서 돌리기)
seed 12
launch
adb shell input tap $CX $CY
sleep 7
adb shell input swipe 420 $CY 880 $CY 1500
sleep 3
shot 03-back

# 5~6: 돌리는 도중 (손을 떼지 않은 채로) 두께 확인
seed 12
launch
adb shell input tap $CX $CY
sleep 7
adb shell input motionevent DOWN 420 $CY
for x in 450 480 510 540 570 600; do adb shell input motionevent MOVE $x $CY; done
sleep 0.6
shot 05-turning
for x in 640 680 720 760; do adb shell input motionevent MOVE $x $CY; done
sleep 0.6
shot 06-edge
adb shell input motionevent UP 760 $CY

# 7: 홈 화면 위젯 (디버그 빌드 전용 미리보기 화면)
seed 12
adb shell appwidget grantbind --package "$PKG" --user 0 || adb shell cmd appwidget grantbind --package "$PKG" --user 0 || true
adb shell am force-stop "$PKG"
adb shell am start -W -n "$PKG/.WidgetPreviewActivity" >/dev/null
sleep 9
shot 07-widgets

ls -la "$OUT"
# rerun 1794078871
