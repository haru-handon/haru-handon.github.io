import Foundation

/// 문구 카테고리. 한 카테고리를 다 모으면 금고 문에 그 이름의 라벨이 붙는다. (안드로이드와 같은 순서·id)
enum Category: Int, CaseIterable, Identifiable {
    case money, luck, bond, steady, ease

    var id: Int { rawValue }

    var label: String {
        switch self {
        case .money: return "재물"
        case .luck: return "행운"
        case .bond: return "인연"
        case .steady: return "성실"
        case .ease: return "여유"
        }
    }
}

struct Fortune: Identifiable, Equatable {
    let id: Int
    let category: Category
    let text: String
}

/// 오늘의 재물운 문구 60개. 금고를 처음 연 날마다 모두 같은 확률로 하나를 뽑는다 (중복 가능).
/// id는 저장 키라서 바꾸지 않는다. android/.../data/Fortunes.kt 와 같은 목록.
enum Fortunes {
    static let all: [Fortune] = [
        Fortune(id: 0, category: .money, text: "오늘 들어온 복은 내일 두 배로 돌아옵니다."),
        Fortune(id: 1, category: .steady, text: "한 돈 한 돈 모여 큰 금괴가 됩니다. 오늘도 한 돈 채웠어요."),
        Fortune(id: 2, category: .luck, text: "뜻밖의 곳에서 좋은 소식이 들려오는 날이에요."),
        Fortune(id: 3, category: .money, text: "지갑이 두둑해지는 기운이 함께합니다."),
        Fortune(id: 4, category: .money, text: "오늘 아낀 만 원이 내일의 금이 됩니다."),
        Fortune(id: 5, category: .bond, text: "웃는 얼굴에 재물이 따라오는 하루예요."),
        Fortune(id: 6, category: .steady, text: "오래 미뤄둔 일을 처리하면 운이 트입니다."),
        Fortune(id: 7, category: .bond, text: "귀인이 가까이에 있어요. 먼저 인사를 건네보세요."),
        Fortune(id: 8, category: .steady, text: "금은 서두르지 않아도 빛납니다. 오늘도 차근차근."),
        Fortune(id: 9, category: .money, text: "돈 들어올 문이 활짝 열리는 날입니다."),
        Fortune(id: 10, category: .luck, text: "오늘의 선택이 좋은 결과로 이어져요."),
        Fortune(id: 11, category: .ease, text: "건강이 가장 큰 재산이에요. 물 한 잔 더 드세요."),
        Fortune(id: 12, category: .luck, text: "생각지 못한 용돈 같은 행운이 찾아옵니다."),
        Fortune(id: 13, category: .steady, text: "꾸준함이 가장 큰 재테크예요. 오늘도 출석하셨네요."),
        Fortune(id: 14, category: .bond, text: "오늘 만난 사람에게 복을 나눠주세요. 두 배로 돌아옵니다."),
        Fortune(id: 15, category: .luck, text: "계획했던 일이 술술 풀리는 하루예요."),
        Fortune(id: 16, category: .ease, text: "금고 문을 연 당신, 오늘 하루도 부자입니다."),
        Fortune(id: 17, category: .luck, text: "작은 행운이 연달아 찾아오는 날이에요."),
        Fortune(id: 18, category: .ease, text: "마음이 넉넉하면 주머니도 넉넉해집니다."),
        Fortune(id: 19, category: .ease, text: "사고 싶은 것보다 이미 가진 것을 세어보는 날이에요."),
        Fortune(id: 20, category: .luck, text: "좋은 기운이 동쪽에서 들어옵니다."),
        Fortune(id: 21, category: .money, text: "행복하세요. 그리고 돈 많이 버세요."),
        Fortune(id: 22, category: .luck, text: "금괴처럼 반짝이는 하루가 될 거예요."),
        Fortune(id: 23, category: .steady, text: "오늘의 수고가 곧 보상으로 돌아옵니다."),
        Fortune(id: 24, category: .money, text: "잊고 있던 돈이 주머니에서 나올지도 몰라요."),
        Fortune(id: 25, category: .bond, text: "오늘 한 약속은 좋은 인연으로 이어집니다."),
        Fortune(id: 26, category: .ease, text: "아침을 든든히 먹으면 운도 든든해져요."),
        Fortune(id: 27, category: .money, text: "통장에 작은 기쁨이 찍히는 날이에요."),
        Fortune(id: 28, category: .bond, text: "남을 칭찬하면 복이 돌아오는 하루예요."),
        Fortune(id: 29, category: .steady, text: "서랍 정리를 하면 막혔던 운이 풀립니다."),
        Fortune(id: 30, category: .money, text: "오늘은 쓰는 것보다 모으는 재미가 큰 날이에요."),
        Fortune(id: 31, category: .bond, text: "반가운 연락이 기다리고 있어요."),
        Fortune(id: 32, category: .ease, text: "느긋한 마음이 큰돈을 지켜줍니다."),
        Fortune(id: 33, category: .steady, text: "오늘 배운 것 하나가 나중에 큰 자산이 됩니다."),
        Fortune(id: 34, category: .luck, text: "금빛 기운이 당신 곁을 맴도는 날이에요."),
        Fortune(id: 35, category: .bond, text: "작은 친절이 큰 행운으로 돌아옵니다."),
        Fortune(id: 36, category: .steady, text: "하고 싶던 일을 시작하기 좋은 날이에요."),
        Fortune(id: 37, category: .bond, text: "오늘은 가족에게 따뜻한 말 한마디가 복이 됩니다."),
        Fortune(id: 38, category: .money, text: "뜻밖의 할인이나 공짜가 찾아올지도 몰라요."),
        Fortune(id: 39, category: .money, text: "마음먹은 저축 목표에 한 걸음 가까워져요."),
        Fortune(id: 40, category: .luck, text: "걱정했던 일이 생각보다 쉽게 풀립니다."),
        Fortune(id: 41, category: .luck, text: "오늘의 행운 색은 금색이에요."),
        Fortune(id: 42, category: .bond, text: "좋은 사람과 밥 한 끼 하면 운이 커져요."),
        Fortune(id: 43, category: .ease, text: "가벼운 산책이 좋은 아이디어를 데려옵니다."),
        Fortune(id: 44, category: .ease, text: "손에 쥔 것을 소중히 여기면 더 많이 들어와요."),
        Fortune(id: 45, category: .bond, text: "오늘은 미소 한 번이 금 한 돈 값이에요."),
        Fortune(id: 46, category: .steady, text: "차곡차곡 쌓은 것이 빛을 보는 날이에요."),
        Fortune(id: 47, category: .steady, text: "운이 좋은 날은 스스로 만드는 거예요. 오늘이 그날입니다."),
        Fortune(id: 48, category: .luck, text: "기분 좋은 소식이 오후에 찾아옵니다."),
        Fortune(id: 49, category: .money, text: "돈이 돈을 부르는 날, 지갑을 정리해 보세요."),
        Fortune(id: 50, category: .bond, text: "고마운 사람에게 연락하면 복이 이어져요."),
        Fortune(id: 51, category: .ease, text: "오늘 하루 무사히 보낸 것만으로도 큰 재산이에요."),
        Fortune(id: 52, category: .steady, text: "작게 시작한 일이 크게 자라납니다."),
        Fortune(id: 53, category: .money, text: "현명한 선택으로 지출을 줄이는 날이에요."),
        Fortune(id: 54, category: .bond, text: "오늘은 당신이 누군가의 귀인이 됩니다."),
        Fortune(id: 55, category: .luck, text: "반짝이는 기회가 눈앞에 있어요. 놓치지 마세요."),
        Fortune(id: 56, category: .ease, text: "마음에 여유가 생기면 재물도 머뭅니다."),
        Fortune(id: 57, category: .ease, text: "오늘 웃은 만큼 내일 금이 쌓여요."),
        Fortune(id: 58, category: .steady, text: "복은 부지런한 사람을 먼저 찾아갑니다."),
        Fortune(id: 59, category: .ease, text: "한 돈의 무게만큼 마음도 든든한 하루 되세요."),
    ]

    private static let byId: [Int: Fortune] = Dictionary(uniqueKeysWithValues: all.map { ($0.id, $0) })

    static func of(_ id: Int) -> Fortune? { byId[id] }

    static func inCategory(_ c: Category) -> [Fortune] { all.filter { $0.category == c } }

    static func draw() -> Fortune { all.randomElement()! }
}
