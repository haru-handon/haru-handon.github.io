package com.ohsang.util.haruhandon.data

import java.time.LocalDate
import kotlin.random.Random

/** 문구 카테고리. 한 카테고리를 다 모으면 금고에 그 카테고리의 장식이 생긴다. */
enum class Category(val label: String, val decor: String, val decorHint: String) {
    MONEY("재물", "엽전 장식", "금고 꼭대기에 금 엽전이 얹혀요"),
    LUCK("행운", "福 다이얼", "다이얼 손잡이가 금빛 福으로 바뀌어요"),
    BOND("인연", "매듭 노리개", "손잡이에 붉은 매듭 노리개가 달려요"),
    STEADY("성실", "금 징 테두리", "문 테두리를 따라 금 징이 박혀요"),
    EASE("여유", "구름 장석", "문 네 모서리에 구름 모양 금 장석이 붙어요"),
}

/** id는 저장 키라서 한 번 정하면 바꾸지 않는다 (새 문구는 뒤에 이어 붙인다). */
data class Fortune(val id: Int, val category: Category, val text: String)

/** 오늘의 재물운 문구. 금고를 처음 연 날마다 모든 문구 중 하나를 똑같은 확률로 뽑는다. */
object Fortunes {

    val ALL = listOf(
        Fortune(0, Category.MONEY, "오늘 들어온 복은 내일 두 배로 돌아옵니다."),
        Fortune(1, Category.STEADY, "한 돈 한 돈 모여 큰 금괴가 됩니다. 오늘도 한 돈 채웠어요."),
        Fortune(2, Category.LUCK, "뜻밖의 곳에서 좋은 소식이 들려오는 날이에요."),
        Fortune(3, Category.MONEY, "지갑이 두둑해지는 기운이 함께합니다."),
        Fortune(4, Category.MONEY, "오늘 아낀 만 원이 내일의 금이 됩니다."),
        Fortune(5, Category.BOND, "웃는 얼굴에 재물이 따라오는 하루예요."),
        Fortune(6, Category.STEADY, "오래 미뤄둔 일을 처리하면 운이 트입니다."),
        Fortune(7, Category.BOND, "귀인이 가까이에 있어요. 먼저 인사를 건네보세요."),
        Fortune(8, Category.STEADY, "금은 서두르지 않아도 빛납니다. 오늘도 차근차근."),
        Fortune(9, Category.MONEY, "돈 들어올 문이 활짝 열리는 날입니다."),
        Fortune(10, Category.LUCK, "오늘의 선택이 좋은 결과로 이어져요."),
        Fortune(11, Category.EASE, "건강이 가장 큰 재산이에요. 물 한 잔 더 드세요."),
        Fortune(12, Category.LUCK, "생각지 못한 용돈 같은 행운이 찾아옵니다."),
        Fortune(13, Category.STEADY, "꾸준함이 가장 큰 재테크예요. 오늘도 출석하셨네요."),
        Fortune(14, Category.BOND, "오늘 만난 사람에게 복을 나눠주세요. 두 배로 돌아옵니다."),
        Fortune(15, Category.LUCK, "계획했던 일이 술술 풀리는 하루예요."),
        Fortune(16, Category.EASE, "금고 문을 연 당신, 오늘 하루도 부자입니다."),
        Fortune(17, Category.LUCK, "작은 행운이 연달아 찾아오는 날이에요."),
        Fortune(18, Category.EASE, "마음이 넉넉하면 주머니도 넉넉해집니다."),
        Fortune(19, Category.EASE, "사고 싶은 것보다 이미 가진 것을 세어보는 날이에요."),
        Fortune(20, Category.LUCK, "좋은 기운이 동쪽에서 들어옵니다."),
        Fortune(21, Category.MONEY, "행복하세요. 그리고 돈 많이 버세요."),
        Fortune(22, Category.LUCK, "금괴처럼 반짝이는 하루가 될 거예요."),
        Fortune(23, Category.STEADY, "오늘의 수고가 곧 보상으로 돌아옵니다."),
        Fortune(24, Category.MONEY, "잊고 있던 돈이 주머니에서 나올지도 몰라요."),
        Fortune(25, Category.BOND, "오늘 한 약속은 좋은 인연으로 이어집니다."),
        Fortune(26, Category.EASE, "아침을 든든히 먹으면 운도 든든해져요."),
        Fortune(27, Category.MONEY, "통장에 작은 기쁨이 찍히는 날이에요."),
        Fortune(28, Category.BOND, "남을 칭찬하면 복이 돌아오는 하루예요."),
        Fortune(29, Category.STEADY, "서랍 정리를 하면 막혔던 운이 풀립니다."),
        Fortune(30, Category.MONEY, "오늘은 쓰는 것보다 모으는 재미가 큰 날이에요."),
        Fortune(31, Category.BOND, "반가운 연락이 기다리고 있어요."),
        Fortune(32, Category.EASE, "느긋한 마음이 큰돈을 지켜줍니다."),
        Fortune(33, Category.STEADY, "오늘 배운 것 하나가 나중에 큰 자산이 됩니다."),
        Fortune(34, Category.LUCK, "금빛 기운이 당신 곁을 맴도는 날이에요."),
        Fortune(35, Category.BOND, "작은 친절이 큰 행운으로 돌아옵니다."),
        Fortune(36, Category.STEADY, "하고 싶던 일을 시작하기 좋은 날이에요."),
        Fortune(37, Category.BOND, "오늘은 가족에게 따뜻한 말 한마디가 복이 됩니다."),
        Fortune(38, Category.MONEY, "뜻밖의 할인이나 공짜가 찾아올지도 몰라요."),
        Fortune(39, Category.MONEY, "마음먹은 저축 목표에 한 걸음 가까워져요."),
        Fortune(40, Category.LUCK, "걱정했던 일이 생각보다 쉽게 풀립니다."),
        Fortune(41, Category.LUCK, "오늘의 행운 색은 금색이에요."),
        Fortune(42, Category.BOND, "좋은 사람과 밥 한 끼 하면 운이 커져요."),
        Fortune(43, Category.EASE, "가벼운 산책이 좋은 아이디어를 데려옵니다."),
        Fortune(44, Category.EASE, "손에 쥔 것을 소중히 여기면 더 많이 들어와요."),
        Fortune(45, Category.BOND, "오늘은 미소 한 번이 금 한 돈 값이에요."),
        Fortune(46, Category.STEADY, "차곡차곡 쌓은 것이 빛을 보는 날이에요."),
        Fortune(47, Category.STEADY, "운이 좋은 날은 스스로 만드는 거예요. 오늘이 그날입니다."),
        Fortune(48, Category.LUCK, "기분 좋은 소식이 오후에 찾아옵니다."),
        Fortune(49, Category.MONEY, "돈이 돈을 부르는 날, 지갑을 정리해 보세요."),
        Fortune(50, Category.BOND, "고마운 사람에게 연락하면 복이 이어져요."),
        Fortune(51, Category.EASE, "오늘 하루 무사히 보낸 것만으로도 큰 재산이에요."),
        Fortune(52, Category.STEADY, "작게 시작한 일이 크게 자라납니다."),
        Fortune(53, Category.MONEY, "현명한 선택으로 지출을 줄이는 날이에요."),
        Fortune(54, Category.BOND, "오늘은 당신이 누군가의 귀인이 됩니다."),
        Fortune(55, Category.LUCK, "반짝이는 기회가 눈앞에 있어요. 놓치지 마세요."),
        Fortune(56, Category.EASE, "마음에 여유가 생기면 재물도 머뭅니다."),
        Fortune(57, Category.EASE, "오늘 웃은 만큼 내일 금이 쌓여요."),
        Fortune(58, Category.STEADY, "복은 부지런한 사람을 먼저 찾아갑니다."),
        Fortune(59, Category.EASE, "한 돈의 무게만큼 마음도 든든한 하루 되세요."),
    )

    private val byId = ALL.associateBy { it.id }

    fun of(id: Int): Fortune? = byId[id]

    fun inCategory(c: Category): List<Fortune> = ALL.filter { it.category == c }

    /** 모든 문구가 같은 확률 (중복해서 나올 수 있다) */
    fun draw(random: Random = Random.Default): Fortune = ALL[random.nextInt(ALL.size)]

    /** 도감 이전 버전에서 그날 보여주던 문구 (기존 출석 기록을 도감으로 옮길 때만 쓴다) */
    fun legacyForDate(date: LocalDate): Fortune {
        val index = Math.floorMod(date.toEpochDay() * 37, ALL.size.toLong()).toInt()
        return ALL[index]
    }
}
