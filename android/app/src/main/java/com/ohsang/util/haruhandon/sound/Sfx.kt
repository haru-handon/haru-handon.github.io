package com.ohsang.util.haruhandon.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import com.ohsang.util.haruhandon.R
import kotlin.random.Random

enum class Sound(val res: Int) {
    TICK(R.raw.sfx_tick),
    CLUNK(R.raw.sfx_clunk),
    TING(R.raw.sfx_ting),
    CHIME(R.raw.sfx_chime),
    FANFARE(R.raw.sfx_fanfare),
    FLIP(R.raw.sfx_flip),
}

/** 짧은 효과음. 무음 모드에서는 시스템 볼륨을 따른다. */
class Sfx(context: Context) {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids: Map<Sound, Int> = Sound.entries.associateWith { pool.load(context, it.res, 1) }

    var enabled: Boolean = true

    fun play(sound: Sound, volume: Float = 1f, jitter: Boolean = false) {
        if (!enabled) return
        val id = ids[sound] ?: return
        val rate = if (jitter) 0.92f + Random.nextFloat() * 0.18f else 1f
        pool.play(id, volume, volume, 1, 0, rate)
    }

    fun release() = pool.release()
}

/** 진동. 기기 설정의 터치 진동을 따른다. */
class Haptics(private val view: View) {
    fun tick() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun thud() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun confirm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
}
