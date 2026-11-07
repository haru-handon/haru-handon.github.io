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

ls -la "$OUT"
