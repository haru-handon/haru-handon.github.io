import Foundation

/// 앱과 위젯이 같이 쓰는 저장소 (App Group)
let appGroupID = "group.com.ohsang.util.haruhandon"

/* ---------- 규칙 (안드로이드 Rules.kt 와 같다) ---------- */

/// 금 1돈 = 3.75g, 10돈 = 1냥
let donPerNyang = 10

struct Tier: Equatable {
    let min: Int
    let name: String
    let short: String
}

/// 금고 단계: 0돈 철제 → 1냥 황동 → 3냥 금빛 → 10냥 황금 대금고
let tiers: [Tier] = [
    Tier(min: 0, name: "철제 금고", short: "철제"),
    Tier(min: 10, name: "황동 금고", short: "1냥"),
    Tier(min: 30, name: "금빛 금고", short: "3냥"),
    Tier(min: 100, name: "황금 대금고", short: "10냥"),
]

func tierOf(_ don: Int) -> Tier { tiers.last { don >= $0.min } ?? tiers[0] }
func nextTier(_ don: Int) -> Tier? { tiers.first { $0.min > don } }
func tierIndex(_ don: Int) -> Int { tiers.firstIndex(of: tierOf(don)) ?? 0 }

/// 13 → "1냥 3돈", 10 → "1냥", 7 → "7돈"
func donLabel(_ don: Int) -> String {
    let nyang = don / donPerNyang
    let rest = don % donPerNyang
    if nyang == 0 { return "\(rest)돈" }
    if rest == 0 { return "\(nyang)냥" }
    return "\(nyang)냥 \(rest)돈"
}

/// 13 → "48.75g", 12 → "45g"
func gramLabel(_ don: Int) -> String {
    let hundredths = don * 375
    let whole = hundredths / 100
    let frac = hundredths % 100
    if frac == 0 { return "\(whole)g" }
    if frac % 10 == 0 { return "\(whole).\(frac / 10)g" }
    return String(format: "%d.%02dg", whole, frac)
}

func serialLabel(_ don: Int) -> String { String(format: "No. HD%06d", don) }

func isNyangMilestone(_ don: Int) -> Bool { don > 0 && don % donPerNyang == 0 }

/* ---------- 저장 ---------- */

/// 출석 날짜, 모은 문구, 설정. 모두 기기 안(App Group UserDefaults)에만 둔다.
final class HandonStore {
    static let shared = HandonStore()

    let defaults: UserDefaults

    init(defaults: UserDefaults? = nil) {
        self.defaults = defaults ?? UserDefaults(suiteName: appGroupID) ?? .standard
    }

    private enum Key {
        static let days = "attended_days"
        static let counts = "fortune_counts"
        static let todayFortune = "today_fortune"
        static let bonus = "bonus_fortune"
        static let soundOn = "sound_on"
        static let notifyAsked = "notify_asked"
        static let debugNext = "debug_next_fortune"
        static let debugBonus = "debug_next_bonus"
        static let debugFakeAd = "debug_fake_ad"
    }

    /* 날짜 (기기의 현지 자정 기준) */

    private static let dayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = .current
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()

    static func key(_ date: Date) -> String {
        dayFormatter.timeZone = .current
        return dayFormatter.string(from: date)
    }

    func todayKey(_ now: Date = Date()) -> String { Self.key(now) }

    func days() -> Set<String> { Set(defaults.stringArray(forKey: Key.days) ?? []) }

    func totalDon() -> Int { days().count }

    func attendedToday() -> Bool { days().contains(todayKey()) }

    /// 오늘(또는 어제)부터 거꾸로 이어진 출석 일수
    func streak() -> Int {
        let set = days()
        let cal = Calendar.current
        var d = Date()
        if !set.contains(Self.key(d)) { d = cal.date(byAdding: .day, value: -1, to: d)! }
        var n = 0
        while set.contains(Self.key(d)) {
            n += 1
            d = cal.date(byAdding: .day, value: -1, to: d)!
        }
        return n
    }

    /// 오늘 처음 금고를 열었으면 기록하고, 오늘의 문구를 하나 뽑아 도감에 넣은 뒤 true
    @discardableResult
    func attendToday() -> Bool {
        let key = todayKey()
        var set = days()
        if set.contains(key) { return false }
        set.insert(key)
        let drawn = forced(Key.debugNext) ?? Fortunes.draw()
        var counts = fortuneCounts()
        counts[drawn.id, default: 0] += 1
        defaults.set(Array(set).sorted(), forKey: Key.days)
        defaults.set(Self.encode(counts), forKey: Key.counts)
        defaults.set("\(key)|\(drawn.id)", forKey: Key.todayFortune)
        defaults.removeObject(forKey: Key.debugNext)
        return true
    }

    /* 문구 도감 */

    func fortuneCounts() -> [Int: Int] { Self.decode(defaults.string(forKey: Key.counts)) }

    func collectedCount() -> Int { fortuneCounts().keys.filter { Fortunes.of($0) != nil }.count }

    func todayFortune() -> Fortune? { dated(Key.todayFortune) }

    /// 광고를 보고 하나 더 받은 오늘의 문구 (하루 한 번)
    func bonusFortune() -> Fortune? { dated(Key.bonus) }

    func canTakeBonus() -> Bool { todayFortune() != nil && bonusFortune() == nil }

    /// 보상형 광고를 끝까지 본 뒤: 문구를 하나 더 뽑아 도감에 넣는다. 금은 늘지 않는다.
    func takeBonus() -> Fortune? {
        guard canTakeBonus() else { return nil }
        let drawn = forced(Key.debugBonus) ?? Fortunes.draw()
        var counts = fortuneCounts()
        counts[drawn.id, default: 0] += 1
        defaults.set(Self.encode(counts), forKey: Key.counts)
        defaults.set("\(todayKey())|\(drawn.id)", forKey: Key.bonus)
        defaults.removeObject(forKey: Key.debugBonus)
        return drawn
    }

    /// 문구를 다 모은 카테고리 = 금고 문에 붙은 라벨
    func completedCategories() -> Set<Category> {
        let have = Set(fortuneCounts().keys)
        return Set(Category.allCases.filter { c in Fortunes.inCategory(c).allSatisfy { have.contains($0.id) } })
    }

    /* 설정 */

    var soundOn: Bool {
        get { defaults.object(forKey: Key.soundOn) as? Bool ?? true }
        set { defaults.set(newValue, forKey: Key.soundOn) }
    }

    var notificationAsked: Bool {
        get { defaults.bool(forKey: Key.notifyAsked) }
        set { defaults.set(newValue, forKey: Key.notifyAsked) }
    }

    var debugFakeAd: Bool {
        #if DEBUG
        defaults.bool(forKey: Key.debugFakeAd)
        #else
        false
        #endif
    }

    /* 내부 */

    private func dated(_ key: String) -> Fortune? {
        guard let raw = defaults.string(forKey: key) else { return nil }
        let parts = raw.split(separator: "|")
        guard parts.count == 2, String(parts[0]) == todayKey(), let id = Int(parts[1]) else { return nil }
        return Fortunes.of(id)
    }

    private func forced(_ key: String) -> Fortune? {
        #if DEBUG
        guard defaults.object(forKey: key) != nil else { return nil }
        return Fortunes.of(defaults.integer(forKey: key))
        #else
        return nil
        #endif
    }

    static func encode(_ m: [Int: Int]) -> String {
        m.keys.sorted().map { "\($0)=\(m[$0]!)" }.joined(separator: ",")
    }

    static func decode(_ s: String?) -> [Int: Int] {
        var out: [Int: Int] = [:]
        for part in (s ?? "").split(separator: ",") {
            let kv = part.split(separator: "=")
            if kv.count == 2, let k = Int(kv[0]), let v = Int(kv[1]) { out[k] = v }
        }
        return out
    }

    #if DEBUG
    /// 캡처용 상태 만들기 (디버그 빌드 + 환경 변수 HANDON_SEED)
    func seed(_ preset: String) {
        for k in [Key.days, Key.counts, Key.todayFortune, Key.bonus, Key.debugNext, Key.debugBonus] {
            defaults.removeObject(forKey: k)
        }
        defaults.set(true, forKey: Key.notifyAsked)
        defaults.set(true, forKey: Key.debugFakeAd)
        let cal = Calendar.current
        func pastDays(_ n: Int) -> [String] {
            (1...max(n, 1)).prefix(n).map { Self.key(cal.date(byAdding: .day, value: -$0, to: Date())!) }
        }
        switch preset {
        case "dogam":
            defaults.set(pastDays(52), forKey: Key.days)
            defaults.set(
                "0=2,3=1,4=3,9=1,21=2,24=1,27=1,30=2,38=1,39=1,49=1,53=2,2=1,10=2,12=1,15=1,17=1,20=1,22=2,34=1,40=1,41=1,48=1,5=1,7=1,14=2,25=1,28=1,1=1,6=1,8=1,13=1,23=2,29=1,11=1,16=1,18=2,19=1",
                forKey: Key.counts)
            defaults.set(55, forKey: Key.debugNext)
            defaults.set(31, forKey: Key.debugBonus)
        case "nyang":
            defaults.set(pastDays(9), forKey: Key.days)
            defaults.set("0=1,3=1,4=1,9=1,21=1,24=1,27=1,30=1,38=1", forKey: Key.counts)
            defaults.set(39, forKey: Key.debugNext)
        case "full":
            defaults.set(pastDays(60), forKey: Key.days)
            defaults.set(Self.encode(Dictionary(uniqueKeysWithValues: (0..<60).map { ($0, 1) })), forKey: Key.counts)
            defaults.set(0, forKey: Key.debugNext)
        default:
            break
        }
    }
    #endif
}
