package com.ohsang.util.haruhandon.data

import android.content.Context
import androidx.core.content.edit
import java.time.LocalDate

/**
 * 출석 기록을 기기 안에만 저장한다 (SharedPreferences "handon").
 * 파일 이름은 백업 규칙(res/xml)과 맞춰야 한다.
 */
class HandonStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** 디버그 빌드 테스트용 날짜 이동 (릴리스에서는 늘 0) */
    var debugOffsetDays: Int
        get() = prefs.getInt(KEY_DEBUG_OFFSET, 0)
        set(value) = prefs.edit { putInt(KEY_DEBUG_OFFSET, value) }

    var soundOn: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit { putBoolean(KEY_SOUND, value) }

    var notificationAsked: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_ASKED, false)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFY_ASKED, value) }

    fun today(): LocalDate = LocalDate.now().plusDays(debugOffsetDays.toLong())

    fun days(): Set<String> = prefs.getStringSet(KEY_DAYS, emptySet())?.toSet() ?: emptySet()

    /** 모은 금 = 출석한 날 수 (1일 = 1돈) */
    fun totalDon(): Int = days().size

    fun attendedToday(): Boolean = today().toString() in days()

    /** 오늘 처음 금고를 열었으면 기록하고 true */
    fun attendToday(): Boolean {
        val key = today().toString()
        val current = days()
        if (key in current) return false
        prefs.edit { putStringSet(KEY_DAYS, current + key) }
        return true
    }

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

    /** 디버그용: 어제까지 연속으로 [count]일 출석한 상태로 맞춤 */
    fun debugFillDays(count: Int) {
        val t = today()
        val set = (1..count).map { t.minusDays(it.toLong()).toString() }.toSet()
        prefs.edit { putStringSet(KEY_DAYS, set) }
    }

    fun debugReset() {
        prefs.edit {
            remove(KEY_DAYS)
            putInt(KEY_DEBUG_OFFSET, 0)
        }
    }

    companion object {
        const val FILE = "handon"
        private const val KEY_DAYS = "attended_days"
        private const val KEY_SOUND = "sound_on"
        private const val KEY_NOTIFY_ASKED = "notify_asked"
        private const val KEY_DEBUG_OFFSET = "debug_offset_days"
    }
}
