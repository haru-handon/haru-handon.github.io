# 하루 한돈 iOS

SwiftUI + WidgetKit 네이티브 앱. 규칙·문구·색은 안드로이드(`../android`)와 같다.

- 번들 ID: `com.ohsang.util.haruhandon` / 위젯 `com.ohsang.util.haruhandon.widget`
- App Group: `group.com.ohsang.util.haruhandon` (위젯이 금 무게·오늘의 문구·골드바 그림을 읽음)
- 프로젝트: `project.yml` (XcodeGen). Mac 없이 GitHub Actions에서 만든다.
- 효과음: 안드로이드 `res/raw/*.wav` 를 그대로 묶음
- 글꼴: 나눔명조 ExtraBold/Bold (OFL) — 한자(金·福)는 iOS 기본 히라기노 명조
- 광고: AdMob 보상형 "문구 하나 더 받기" (Secrets `ADMOB_IOS_APP_ID`, `ADMOB_IOS_REWARDED_ID`)
- 위젯: 홈 작게(골드바만)·중간(골드바+문구), 잠금 화면 원형·직사각형

## 워크플로
- **iOS 시뮬레이터 화면** (`ios-shots.yml`): 서명 없이 빌드해 장면별 화면을 `ios-shots` 브랜치에 올림. 디버그 빌드의 `HANDON_SEED`·`HANDON_SCRIPT`·`HANDON_WIDGETSHOT` 환경 변수로 장면을 만든다.
- **iOS 빌드 → TestFlight 업로드** (`ios-testflight.yml`): Secrets `APPLE_TEAM_ID`, `ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_KEY_P8_BASE64` 필요.
