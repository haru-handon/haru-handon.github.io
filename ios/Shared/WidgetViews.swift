import SwiftUI
import WidgetKit

/// 위젯에 보여줄 값 (앱이 저장한 기록에서 읽는다)
struct HandonSnapshot {
    let don: Int
    let fortune: String?
    let openedToday: Bool
    let bar: UIImage

    static func load() -> HandonSnapshot {
        let s = HandonStore.shared
        let don = s.totalDon()
        return HandonSnapshot(don: don, fortune: s.todayFortune()?.text, openedToday: s.attendedToday(),
                              bar: WidgetBar.load(don: don))
    }

    static let sample = HandonSnapshot(don: 13, fortune: "반짝이는 기회가 눈앞에 있어요. 놓치지 마세요.", openedToday: true,
                                       bar: GoldBarDrawing.image(don: 13, height: 170, shadow: true, scale: 2))
}

/// 홈 화면 위젯 배경: 어두운 강철
struct WidgetSteelBackground: View {
    var body: some View {
        LinearGradient(colors: [Color(hex: 0x2A2D32), Color(hex: 0x15171A)], startPoint: .topLeading, endPoint: .bottomTrailing)
    }
}

struct HandonWidgetView: View {
    let snap: HandonSnapshot
    let family: WidgetFamily

    var body: some View {
        switch family {
        case .systemSmall:
            // 2×2: 골드바만 크게
            Image(uiImage: snap.bar)
                .resizable()
                .scaledToFit()
                .padding(6)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .systemMedium:
            HStack(spacing: 14) {
                Image(uiImage: snap.bar).resizable().scaledToFit()
                VStack(alignment: .leading, spacing: 6) {
                    Text(snap.openedToday ? "오늘의 재물운" : "하루 한돈")
                        .font(.system(size: 11, weight: .semibold)).foregroundStyle(Palette.gold)
                    Text(snap.fortune ?? "금고를 열면 오늘의 금 1돈과 문구를 받아요.")
                        .font(.serif(15)).foregroundStyle(Palette.text)
                        .lineLimit(4).minimumScaleFactor(0.85)
                    Text(donLabel(snap.don)).font(.system(size: 11)).foregroundStyle(Palette.muted)
                }
                Spacer(minLength: 0)
            }
            .padding(.vertical, 10).padding(.leading, 8).padding(.trailing, 14)
        case .accessoryCircular:
            ZStack {
                AccessoryWidgetBackground()
                VStack(spacing: 1) {
                    MiniBar().frame(width: 18, height: 25)
                    Text(snap.don >= 10 ? "\(snap.don / 10)냥" : "\(snap.don)돈")
                        .font(.system(size: 11, weight: .bold, design: .rounded))
                        .minimumScaleFactor(0.7)
                }
            }
            .widgetAccentable()
        case .accessoryRectangular:
            HStack(spacing: 8) {
                MiniBar().frame(width: 24, height: 34)
                VStack(alignment: .leading, spacing: 1) {
                    Text("하루 한돈 · \(donLabel(snap.don))").font(.system(size: 13, weight: .bold)).widgetAccentable()
                    Text(snap.openedToday ? (snap.fortune ?? "오늘의 금을 받았어요") : "오늘의 금이 도착했어요")
                        .font(.system(size: 12)).lineLimit(2)
                }
                Spacer(minLength: 0)
            }
        default:
            Image(uiImage: snap.bar).resizable().scaledToFit()
        }
    }
}

/// 잠금 화면용 작은 골드바 (단색으로 칠해진다)
struct MiniBar: View {
    var body: some View {
        GeometryReader { g in
            let w = g.size.width, h = g.size.height
            ZStack {
                RoundedRectangle(cornerRadius: w * 0.14).stroke(lineWidth: max(1.2, w * 0.08))
                RoundedRectangle(cornerRadius: w * 0.08).stroke(lineWidth: max(0.6, w * 0.035))
                    .padding(w * 0.16)
                Text("金").font(.system(size: h * 0.36, weight: .black, design: .serif))
            }
        }
    }
}
