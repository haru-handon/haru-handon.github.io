package com.ohsang.util.haruhandon.data

import android.content.Context
import androidx.core.content.edit
import com.ohsang.util.haruhandon.BuildConfig
import java.time.LocalDate

/**
 * 출석 기록을 기기 안에만 저장한다 (SharedPreferences "handon").
 * 파일 이름은 백업 규칙(res/xml)과 맞춰야 한다.
 */
class HandonStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    init {
        migrateToCollection()
    }

    var soundOn: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit { putBoolean(KEY_SOUND, value) }

    var notificationAsked: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_ASKED, false)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFY_ASKED, value) }

    fun today(): LocalDate = LocalDate.now()

    fun days(): Set<String> = prefs.getStringSet(KEY_DAYS, emptySet())?.toSet() ?: emptySet()

    /** 모은 금 = 출석한 날 수 (1일 = 1돈) */
    fun totalDon(): Int = days().size

    fun attendedToday(): Boolean = today().toString() in days()

    /** 오늘 처음 금고를 열었으면 기록하고, 오늘의 문구를 하나 뽑아 도감에 넣은 뒤 true */
    fun attendToday(): Boolean {
        val key = today().toString()
        val current = days()
        if (key in current) return false
        val drawn = debugForced(KEY_DEBUG_NEXT) ?: Fortunes.draw()
        val counts = fortuneCounts().toMutableMap()
        counts[drawn.id] = (counts[drawn.id] ?: 0) + 1
        prefs.edit {
            putStringSet(KEY_DAYS, current + key)
            putString(KEY_COUNTS, encode(counts))
            putString(KEY_TODAY_FORTUNE, "$key|${drawn.id}")
            remove(KEY_DEBUG_NEXT)
        }
        return true
    }

    /* ---------- 문구 도감 ---------- */

    /** 문구 id → 받은 횟수 */
    fun fortuneCounts(): Map<Int, Int> = decode(prefs.getString(KEY_COUNTS, null))

    /** 오늘 뽑은 문구 (오늘 아직 금고를 안 열었으면 null) */
    fun todayFortune(): Fortune? {
        val raw = prefs.getString(KEY_TODAY_FORTUNE, null) ?: return null
        val (date, id) = raw.split('|').takeIf { it.size == 2 } ?: return null
        if (date != today().toString()) return null
        return id.toIntOrNull()?.let { Fortunes.of(it) }
    }

    /** 광고를 보고 하나 더 받은 오늘의 문구 (하루 한 번) */
    fun bonusFortune(): Fortune? {
        val raw = prefs.getString(KEY_BONUS, null) ?: return null
        val (date, id) = raw.split('|').takeIf { it.size == 2 } ?: return null
        if (date != today().toString()) return null
        return id.toIntOrNull()?.let { Fortunes.of(it) }
    }

    /** 오늘 금고를 열었고, 아직 보너스 문구를 안 받았을 때만 */
    fun canTakeBonus(): Boolean = todayFortune() != null && bonusFortune() == null

    /** 보상형 광고를 끝까지 본 뒤: 문구를 하나 더 뽑아 도감에 넣는다. 금은 늘지 않는다. */
    fun takeBonus(): Fortune? {
        if (!canTakeBonus()) return null
        val drawn = debugForced(KEY_DEBUG_BONUS) ?: Fortunes.draw()
        val counts = fortuneCounts().toMutableMap()
        counts[drawn.id] = (counts[drawn.id] ?: 0) + 1
        prefs.edit {
            putString(KEY_COUNTS, encode(counts))
            putString(KEY_BONUS, "${today()}|${drawn.id}")
            remove(KEY_DEBUG_BONUS)
        }
        return drawn
    }

    /** 디버그 캡처용: 실제 광고 대신 자리만 보여준다 */
    fun debugFakeAd(): Boolean = BuildConfig.DEBUG && prefs.getBoolean(KEY_DEBUG_FAKE_AD, false)

    fun collectedCount(): Int = fortuneCounts().keys.count { Fortunes.of(it) != null }

    /** 문구를 다 모은 카테고리 = 금고에 붙은 라벨 */
    fun completedCategories(): Set<Category> {
        val have = fortuneCounts().keys
        return Category.entries.filter { c -> Fortunes.inCategory(c).all { it.id in have } }.toSet()
    }

    /** 도감이 생기기 전 기록이 있으면, 그날 보여주던 문구를 모은 것으로 옮긴다 (한 번만) */
    private fun migrateToCollection() {
        if (prefs.contains(KEY_COUNTS)) return
        val counts = mutableMapOf<Int, Int>()
        days().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.forEach { d ->
            val f = Fortunes.legacyForDate(d)
            counts[f.id] = (counts[f.id] ?: 0) + 1
        }
        prefs.edit {
            putString(KEY_COUNTS, encode(counts))
            val t = today()
            if (t.toString() in days()) putString(KEY_TODAY_FORTUNE, "$t|${Fortunes.legacyForDate(t).id}")
        }
    }

    /** 디버그 빌드에서 캡처할 때만: 다음에 뽑힐 문구를 정해 둔다 */
    private fun debugForced(key: String): Fortune? {
        if (!BuildConfig.DEBUG) return null
        val id = prefs.getInt(key, -1)
        return if (id >= 0) Fortunes.of(id) else null
    }

    private fun encode(m: Map<Int, Int>) = m.entries.joinToString(",") { "${it.key}=${it.value}" }

    private fun decode(s: String?): Map<Int, Int> =
        s.orEmpty().split(',').mapNotNull { part ->
            val kv = part.split('=')
            val k = kv.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val v = kv.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            k to v
        }.toMap()

    fun streak(): Int {
        val set = days()
        var d = today()
        if (d.toString() !in set) d = d.minusDays(1)
        var n = 0
        while (d.toString() in set) {
            n++
            d = d.minusDays(1)
        }
        return n
    }


    companion object {
        const val FILE = "handon"
        private const val KEY_DAYS = "attended_days"
        private const val KEY_SOUND = "sound_on"
        private const val KEY_NOTIFY_ASKED = "notify_asked"
        private const val KEY_COUNTS = "fortune_counts"
        private const val KEY_TODAY_FORTUNE = "today_fortune"
        private const val KEY_DEBUG_NEXT = "debug_next_fortune"
        private const val KEY_BONUS = "bonus_fortune"
        private const val KEY_DEBUG_BONUS = "debug_next_bonus"
        private const val KEY_DEBUG_FAKE_AD = "debug_fake_ad"
    }
}
