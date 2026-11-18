import SwiftUI

extension Color {
    init(hex: UInt32, alpha: Double = 1) {
        self.init(.sRGB,
                  red: Double((hex >> 16) & 0xFF) / 255,
                  green: Double((hex >> 8) & 0xFF) / 255,
                  blue: Double(hex & 0xFF) / 255,
                  opacity: alpha)
    }
}

/// 어두운 강철 금고 + 금빛 (안드로이드 Theme.kt 와 같은 색)
enum Palette {
    static let ink = Color(hex: 0x101113)
    static let inkLift = Color(hex: 0x23262B)
    static let panel = Color(hex: 0x1B1E22)
    static let line = Color(hex: 0x2C3036)
    static let text = Color(hex: 0xEDE7D7)
    static let muted = Color(hex: 0x9C9585)
    static let faint = Color(hex: 0x6D685D)

    static let goldHi = Color(hex: 0xFBE7A6)
    static let gold = Color(hex: 0xDCAE42)
    static let goldLo = Color(hex: 0x8A6316)

    /// 금고 단계별 테두리·문 색: 철제, 황동, 금빛, 황금
    static let rim: [Color] = [Color(hex: 0x5B626B), Color(hex: 0xA9823D), Color(hex: 0xDCAE42), Color(hex: 0xFBE7A6)]
    static let doorHi: [Color] = [Color(hex: 0x4B5159), Color(hex: 0x4F4A40), Color(hex: 0x5A4C2C), Color(hex: 0x7A6331)]
    static let doorLo: [Color] = [Color(hex: 0x24282D), Color(hex: 0x26231D), Color(hex: 0x2A2213), Color(hex: 0x3A2C10)]
}

extension Font {
    /// 제목·각인용 명조 (나눔명조, 없으면 시스템 serif)
    static func serif(_ size: CGFloat, bold: Bool = false) -> Font {
        if UIFont(name: bold ? "NanumMyeongjoBold" : "NanumMyeongjoExtraBold", size: size) != nil {
            return .custom(bold ? "NanumMyeongjoBold" : "NanumMyeongjoExtraBold", fixedSize: size)
        }
        return .system(size: size, weight: .heavy, design: .serif)
    }
}
