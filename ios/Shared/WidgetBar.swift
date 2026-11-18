import UIKit

/// 위젯에 쓰는 골드바 그림. 앱이 명조 글꼴로 그려 App Group에 저장하고, 위젯은 그 파일을 읽는다.
enum WidgetBar {
    static let fileName = "widget-bar.png"

    static var url: URL? {
        FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: appGroupID)?.appendingPathComponent(fileName)
    }

    /// 앱에서: 현재 무게의 골드바를 그려 저장
    static func save(don: Int) {
        let img = GoldBarDrawing.image(don: don, height: 170, pitch: -8, shadow: true, scale: 3)
        guard let url, let data = img.pngData() else { return }
        try? data.write(to: url, options: .atomic)
        HandonStore.shared.defaults.set(don, forKey: "widget_bar_don")
    }

    /// 위젯에서: 저장된 그림이 지금 무게와 같으면 그것을, 아니면 직접 그린다 (시스템 글꼴)
    static func load(don: Int) -> UIImage {
        if HandonStore.shared.defaults.integer(forKey: "widget_bar_don") == don,
           let url, let data = try? Data(contentsOf: url), let img = UIImage(data: data) {
            return img
        }
        return GoldBarDrawing.image(don: don, height: 170, pitch: -8, shadow: true, scale: 2)
    }
}
