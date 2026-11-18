import SwiftUI
import WidgetKit

/// 금고 화면의 상태와 동작. 안드로이드 HomeScreen.kt 의 openVault / closeVault / 보너스 흐름과 같다.
@MainActor
final class HomeModel: ObservableObject {
    enum Phase { case closed, opening, open, closing }

    let store = HandonStore.shared
    let sfx = Sfx.shared

    static let doorOpenDeg: Double = -112

    @Published var phase: Phase = .closed
    @Published var shownDon: Int
    @Published var streak: Int
    @Published var soundOn: Bool
    @Published var counts: [Int: Int]
    @Published var labels: Set<Category>
    @Published var freshLabel: Category?
    @Published var fortune: Fortune?
    @Published var fortuneIsNew = false
    @Published var fortuneVisible = false
    @Published var bonus: Fortune?
    @Published var bonusIsNew = false

    // 금고 움직임
    @Published var door: Double = 0
    @Published var dial: Double = 0
    @Published var lever: Double = 0
    @Published var lift: Double = 0

    // 효과
    @Published var toast: Toast?
    @Published var flash = false
    @Published var plusOne = 0
    @Published var shineAt: Date?
    @Published var burst: Burst?
    @Published var celebration: Int?
    @Published var barResetToken = 0
    @Published var flipToken = 0

    // 화면
    @Published var dogamOpen = false
    @Published var dogamCategory: Category = .money
    @Published var fakeAd = false

    struct Toast: Identifiable, Equatable { let id = UUID(); let text: String }
    struct Burst: Identifiable, Equatable { let id = UUID(); let at: UnitPoint; let count: Int }

    init() {
        let s = HandonStore.shared
        shownDon = s.totalDon()
        streak = s.streak()
        soundOn = s.soundOn
        counts = s.fortuneCounts()
        labels = s.completedCategories()
        fortune = s.todayFortune()
        bonus = s.bonusFortune()
        Sfx.shared.enabled = s.soundOn
    }

    var collected: Int { counts.keys.filter { Fortunes.of($0) != nil }.count }

    /// 날짜가 바뀌었거나 앱으로 돌아왔을 때
    func reload() {
        guard phase == .closed else { return }
        shownDon = store.totalDon()
        streak = store.streak()
        counts = store.fortuneCounts()
        labels = store.completedCategories()
        fortune = store.todayFortune()
        bonus = store.bonusFortune()
        fortuneIsNew = false
        bonusIsNew = false
    }

    func toggleSound() {
        soundOn.toggle()
        store.soundOn = soundOn
        sfx.enabled = soundOn
    }

    private func show(_ text: String) { toast = Toast(text: text) }

    private func sleep(_ ms: Int) async { try? await Task.sleep(nanoseconds: UInt64(ms) * 1_000_000) }

    /* ---------- 열기 ---------- */

    func openVault() {
        guard phase == .closed else { return }
        phase = .opening
        Task { await runOpen() }
    }

    private func runOpen() async {
        let ticking = Task {
            for i in 0..<19 {
                if Task.isCancelled { break }
                sfx.play(.tick, volume: 0.55, jitter: true)
                if i % 3 == 0 { Haptics.tick() }
                await sleep(70)
            }
        }
        withAnimation(.easeOut(duration: 0.35)) { lever = 1 }
        withAnimation(.easeInOut(duration: 0.51)) { dial = 330 }
        await sleep(510)
        withAnimation(.easeInOut(duration: 0.41)) { dial = 150 }
        await sleep(410)
        withAnimation(.easeInOut(duration: 0.43)) { dial = 420 }
        await sleep(430)
        ticking.cancel()

        sfx.play(.clunk)
        Haptics.thud()
        withAnimation(.timingCurve(0.35, 0.75, 0.2, 1, duration: 0.95)) { door = Self.doorOpenDeg }
        await sleep(650)
        withAnimation(.spring(response: 0.42, dampingFraction: 0.55)) { lift = 1 }
        await sleep(500)

        let before = store.totalDon()
        let isNew = store.attendToday()
        let after = store.totalDon()
        streak = store.streak()
        counts = store.fortuneCounts()
        fortune = store.todayFortune()
        fortuneIsNew = isNew && (fortune.map { counts[$0.id] == 1 } ?? false)
        let labelsAfter = store.completedCategories()
        let earned = Category.allCases.first { labelsAfter.contains($0) && !labels.contains($0) }
        withAnimation(.easeOut(duration: 0.3)) { fortuneVisible = true }
        phase = .open

        guard isNew else { return }
        sfx.play(.chime)
        Haptics.confirm()
        flashOnce()
        plusOne += 1
        shineAt = Date()
        burst = Burst(at: .center, count: 12)
        withAnimation(.easeOut(duration: 0.7)) { shownDon = after }
        refreshWidgets()

        let upgraded = tierOf(after) != tierOf(before) ? tierOf(after) : nil
        if isNyangMilestone(after) {
            await sleep(1100)
            sfx.play(.fanfare)
            Haptics.confirm()
            celebration = after
            if let earned {
                freshLabel = earned
                labels = labelsAfter
            }
        } else {
            show(upgraded.map { "\($0.name)로 업그레이드됐어요!" } ?? "금 1돈 입고! 총 \(donLabel(after))")
            if let earned {
                await sleep(1700)
                freshLabel = earned
                labels = labelsAfter
                sfx.play(.chime)
                Haptics.confirm()
                show("\(earned.label) 문구를 다 모았어요! 금고에 라벨을 붙였어요")
            }
            if !store.notificationAsked {
                store.notificationAsked = true
                await sleep(1600)
                Reminders.shared.ask()
            }
        }
        Reminders.shared.reschedule()
    }

    private func flashOnce() {
        withAnimation(.easeOut(duration: 0.15)) { flash = true }
        Task {
            await sleep(160)
            withAnimation(.easeIn(duration: 0.85)) { flash = false }
        }
    }

    /* ---------- 닫기 (금괴 넣기) ---------- */

    func closeVault() {
        guard phase == .open else { return }
        phase = .closing
        Task {
            barResetToken += 1
            withAnimation(.easeInOut(duration: 0.38)) { lift = 0 }
            await sleep(380)
            withAnimation(.easeInOut(duration: 0.85)) { door = 0 }
            await sleep(850)
            sfx.play(.clunk)
            Haptics.thud()
            lever = 0
            dial = 0
            phase = .closed
        }
    }

    /* ---------- 골드바 ---------- */

    func tapBar(at p: UnitPoint) {
        guard phase == .open else { return }
        burst = Burst(at: p, count: 9)
        sfx.play(.ting)
        Haptics.tick()
        shineAt = Date()
    }

    func barFlipped() {
        sfx.play(.flip)
        Haptics.tick()
        Task {
            await sleep(380)
            shineAt = Date()
        }
    }

    /* ---------- 광고 보고 문구 하나 더 ---------- */

    func askBonus() {
        if store.debugFakeAd {
            fakeAd = true
            return
        }
        Ads.shared.show { [weak self] result in
            Task { @MainActor in
                guard let self else { return }
                switch result {
                case .reward: self.grantBonus()
                case .closed: break
                case .unavailable: self.show("광고를 불러오지 못했어요. 잠시 후 다시 눌러 주세요")
                }
            }
        }
    }

    func grantBonus() {
        fakeAd = false
        guard let f = store.takeBonus() else { return }
        counts = store.fortuneCounts()
        withAnimation(.easeOut(duration: 0.3)) {
            bonus = f
            bonusIsNew = counts[f.id] == 1
        }
        sfx.play(.chime)
        Haptics.confirm()
        let labelsAfter = store.completedCategories()
        if let earned = Category.allCases.first(where: { labelsAfter.contains($0) && !labels.contains($0) }) {
            freshLabel = earned
            labels = labelsAfter
            show("\(earned.label) 문구를 다 모았어요! 금고에 라벨을 붙였어요")
        } else {
            show(bonusIsNew ? "새 문구를 도감에 담았어요" : "문구를 하나 더 받았어요")
        }
        refreshWidgets()
    }

    /* ---------- 위젯 ---------- */

    func refreshWidgets() {
        WidgetBar.save(don: store.totalDon())
        WidgetCenter.shared.reloadAllTimelines()
    }
}
