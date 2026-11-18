#if DEBUG
import SwiftUI
import UIKit
import WidgetKit

/// 디버그 빌드 + HANDON_WIDGETSHOT=1 일 때만: 위젯 화면을 PNG로 그려 Documents/widget-*.png 에 저장한다.
/// (시뮬레이터 홈 화면에 위젯을 직접 올릴 수 없어서, 같은 SwiftUI 화면을 그대로 그려 확인용으로 쓴다)
enum WidgetShots {
    @MainActor
    static func renderIfRequested() {
        guard ProcessInfo.processInfo.environment["HANDON_WIDGETSHOT"] == "1" else { return }
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let opened = HandonSnapshot(don: 53, fortune: "반짝이는 기회가 눈앞에 있어요. 놓치지 마세요.", openedToday: true,
                                    bar: GoldBarDrawing.image(don: 53, height: 170, shadow: true, scale: 3))
        let waiting = HandonSnapshot(don: 13, fortune: nil, openedToday: false,
                                     bar: GoldBarDrawing.image(don: 13, height: 170, shadow: true, scale: 3))

        func save<V: View>(_ v: V, _ name: String) {
            let r = ImageRenderer(content: v)
            r.scale = 3
            if let png = r.uiImage?.pngData() { try? png.write(to: docs.appendingPathComponent("widget-\(name).png")) }
        }

        // 홈 화면처럼: 배경화면 위에 작게(2×2) 두 개 + 중간 하나
        let home = VStack(spacing: 22) {
            HStack(spacing: 22) {
                tile(opened, .systemSmall, CGSize(width: 170, height: 170))
                tile(waiting, .systemSmall, CGSize(width: 170, height: 170))
            }
            tile(opened, .systemMedium, CGSize(width: 364, height: 170))
            tile(waiting, .systemMedium, CGSize(width: 364, height: 170))
        }
        .padding(24)
        .background(LinearGradient(colors: [Color(hex: 0x3B4A63), Color(hex: 0x1F2736), Color(hex: 0x141822)],
                                   startPoint: .top, endPoint: .bottom))
        save(home, "home")

        // 잠금 화면 모양: 원형·직사각형
        let lock = VStack(spacing: 6) {
            Text("11월 18일 수요일").font(.system(size: 17, weight: .semibold))
            Text("9:41").font(.system(size: 92, weight: .bold, design: .rounded))
            HStack(spacing: 14) {
                HandonWidgetView(snap: opened, family: .accessoryCircular).frame(width: 72, height: 72)
                HandonWidgetView(snap: opened, family: .accessoryRectangular).frame(width: 170, height: 72)
            }
            HStack(spacing: 14) {
                HandonWidgetView(snap: waiting, family: .accessoryCircular).frame(width: 72, height: 72)
                HandonWidgetView(snap: waiting, family: .accessoryRectangular).frame(width: 170, height: 72)
            }
            .padding(.top, 6)
        }
        .foregroundStyle(.white)
        .frame(width: 390, height: 380)
        .background(LinearGradient(colors: [Color(hex: 0x2B3A67), Color(hex: 0x6B4E8A), Color(hex: 0xC97B84)],
                                   startPoint: .top, endPoint: .bottom))
        .environment(\.colorScheme, .dark)
        save(lock, "lock")
    }

    private static func tile(_ snap: HandonSnapshot, _ family: WidgetFamily, _ size: CGSize) -> some View {
        HandonWidgetView(snap: snap, family: family)
            .frame(width: size.width, height: size.height)
            .background(WidgetSteelBackground())
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
    }
}
#endif
