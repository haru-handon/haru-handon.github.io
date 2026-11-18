import Foundation
import UserNotifications

/// 매일 아침 8시 "오늘의 금 1돈이 도착했어요" 알림.
/// 반복 알림 대신 앞으로 7일치를 예약하고, 앱을 열 때마다 다시 예약한다 (오늘 이미 열었으면 오늘 것은 뺀다).
final class Reminders: NSObject, UNUserNotificationCenterDelegate {
    static let shared = Reminders()

    private let hour = 8
    private let days = 7
    private let prefix = "daily-gold-"

    func ask() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
            if granted { DispatchQueue.main.async { self.reschedule() } }
        }
    }

    func reschedule() {
        let center = UNUserNotificationCenter.current()
        let openedToday = HandonStore.shared.attendedToday()
        center.getNotificationSettings { settings in
            guard settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional else { return }
            center.removePendingNotificationRequests(withIdentifiers: (0...self.days).map { self.prefix + String($0) })
            let cal = Calendar.current
            let now = Date()
            let content = UNMutableNotificationContent()
            content.title = "하루 한돈"
            content.body = "오늘의 금 1돈이 도착했어요. 금고를 열어 보세요."
            content.sound = .default
            for i in 0...self.days {
                guard let day = cal.date(byAdding: .day, value: i, to: now),
                      let fire = cal.date(bySettingHour: self.hour, minute: 0, second: 0, of: day) else { continue }
                if i == 0 && (openedToday || fire <= now) { continue }
                if fire <= now { continue }
                let comps = cal.dateComponents([.year, .month, .day, .hour, .minute], from: fire)
                let trigger = UNCalendarNotificationTrigger(dateMatching: comps, repeats: false)
                center.add(UNNotificationRequest(identifier: self.prefix + String(i), content: content, trigger: trigger))
            }
        }
    }

    // 앱을 보고 있을 때는 알림을 띄우지 않는다
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([])
    }
}
