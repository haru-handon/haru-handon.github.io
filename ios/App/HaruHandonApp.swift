import SwiftUI
import UserNotifications

@main
struct HaruHandonApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var delegate
    @StateObject private var model: HomeModel
    @Environment(\.scenePhase) private var scenePhase

    init() {
        #if DEBUG
        DebugScript.prepare()
        #endif
        _model = StateObject(wrappedValue: HomeModel())
    }

    var body: some Scene {
        WindowGroup {
            HomeView(model: model)
                .statusBarHidden(false)
                .onAppear {
                    model.refreshWidgets()
                    #if DEBUG
                    DebugScript.run(model)
                    WidgetShots.renderIfRequested()
                    #endif
                }
                .onChange(of: scenePhase) { phase in
                    if phase == .active {
                        model.reload()
                        Reminders.shared.reschedule()
                    }
                }
        }
    }
}

final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        UNUserNotificationCenter.current().delegate = Reminders.shared
        Ads.shared.start()
        return true
    }
}

#if DEBUG
/// 디버그 빌드 전용: 시뮬레이터에서 장면을 만들어 화면을 찍기 위한 순서
/// HANDON_SEED = dogam | nyang | full,  HANDON_SCRIPT = open | bonus | dogam | flip
enum DebugScript {
    static func prepare() {
        let env = ProcessInfo.processInfo.environment
        if let seed = env["HANDON_SEED"] { HandonStore.shared.seed(seed) }
    }

    @MainActor
    static func run(_ m: HomeModel) {
        guard let script = ProcessInfo.processInfo.environment["HANDON_SCRIPT"] else { return }
        Task { @MainActor in
            func wait(_ s: Double) async { try? await Task.sleep(nanoseconds: UInt64(s * 1_000_000_000)) }
            await wait(2)
            switch script {
            case "open":
                m.openVault()
            case "bonus":
                m.openVault()
                await wait(7)
                m.askBonus()
                await wait(8)
                m.closeVault()
            case "dogam":
                m.dogamOpen = true
                await wait(4)
                m.dogamCategory = .bond
            case "flip":
                m.openVault()
                await wait(5)
                m.flipToken += 1
            default:
                break
            }
        }
    }
}
#endif
