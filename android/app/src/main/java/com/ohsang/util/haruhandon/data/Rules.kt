package com.ohsang.util.haruhandon.data

import java.math.BigDecimal

/** 금 1돈 = 3.75g, 10돈 = 1냥 */
const val GRAM_PER_DON = "3.75"
const val DON_PER_NYANG = 10

data class Tier(val min: Int, val name: String, val short: String)

/** 금고 단계: 0돈 철제 → 1냥 황동 → 3냥 금빛 → 10냥 황금 대금고 */
val TIERS = listOf(
    Tier(0, "철제 금고", "철제"),
    Tier(10, "황동 금고", "1냥"),
    Tier(30, "금빛 금고", "3냥"),
    Tier(100, "황금 대금고", "10냥"),
)

fun tierOf(don: Int): Tier = TIERS.last { don >= it.min }

fun nextTier(don: Int): Tier? = TIERS.firstOrNull { it.min > don }

fun tierIndex(don: Int): Int = TIERS.indexOf(tierOf(don))

/** 13 → "1냥 3돈", 10 → "1냥", 7 → "7돈" */
fun donLabel(don: Int): String {
    val nyang = don / DON_PER_NYANG
    val rest = don % DON_PER_NYANG
    return when {
        nyang == 0 -> "${rest}돈"
        rest == 0 -> "${nyang}냥"
        else -> "${nyang}냥 ${rest}돈"
    }
}

/** 13 → "48.75g", 12 → "45g" */
fun gramLabel(don: Int): String =
    BigDecimal(GRAM_PER_DON).multiply(BigDecimal(don)).stripTrailingZeros().toPlainString() + "g"

fun serialLabel(don: Int): String = "No. HD" + don.toString().padStart(6, '0')

fun isNyangMilestone(don: Int): Boolean = don > 0 && don % DON_PER_NYANG == 0
