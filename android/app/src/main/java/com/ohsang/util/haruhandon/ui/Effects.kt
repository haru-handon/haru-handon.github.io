package com.ohsang.util.haruhandon.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/* ---------- 반짝이 ---------- */

private data class Spark(val origin: Offset, val angle: Float, val dist: Float, val size: Float, val born: Long)

@Stable
class SparkleField {
    private val items = mutableStateListOf<Spark>()
    private var clock by mutableLongStateOf(0L)

    /** [at]은 반짝이 층 안의 픽셀 좌표 */
    fun burst(at: Offset, count: Int, minDist: Float, maxDist: Float, minSize: Float, maxSize: Float) {
        repeat(count) {
            items += Spark(
                origin = at,
                angle = Random.nextFloat() * 6.2832f,
                dist = minDist + Random.nextFloat() * (maxDist - minDist),
                size = minSize + Random.nextFloat() * (maxSize - minSize),
                born = clock,
            )
        }
    }

    @Composable
    fun Layer(modifier: Modifier = Modifier) {
        LaunchedEffect(this) {
            while (true) {
                withFrameNanos { t ->
                    clock = t
                    items.removeAll { t - it.born > LIFE_NS }
                }
            }
        }
        Canvas(modifier) {
            val now = clock
            items.forEach { sp ->
                val p = ((now - sp.born).toFloat() / LIFE_NS).coerceIn(0f, 1f)
                val pos = sp.origin + Offset(cos(sp.angle), sin(sp.angle)) * (sp.dist * p)
                val half = sp.size * (0.2f + 0.8f * p) / 2f
                rotate(90f * p, pivot = pos) {
                    drawPath(star(pos, half), Color(0xFFFFF8DC), alpha = 1f - p)
                }
            }
        }
    }

    private fun star(c: Offset, r: Float) = Path().apply {
        val k = r * 0.2f
        moveTo(c.x, c.y - r)
        lineTo(c.x + k, c.y - k); lineTo(c.x + r, c.y); lineTo(c.x + k, c.y + k)
        lineTo(c.x, c.y + r); lineTo(c.x - k, c.y + k); lineTo(c.x - r, c.y); lineTo(c.x - k, c.y - k)
        close()
    }

    private companion object {
        const val LIFE_NS = 800_000_000L
    }
}

/* ---------- 금빛 번쩍임 ---------- */

@Composable
fun GoldFlash(alpha: Float, modifier: Modifier = Modifier) {
    if (alpha <= 0f) return
    Canvas(modifier) {
        drawRect(
            Brush.radialGradient(
                0f to Color(0xB3FBE7A6), 0.35f to Color(0x26DCAE42), 0.6f to Color.Transparent,
                center = center, radius = size.minDimension * 0.9f,
            ),
            alpha = alpha,
        )
    }
}

/* ---------- +1돈 ---------- */

/** [progress] 0→1 동안 떠오르며 사라진다. 0 미만이면 숨김. */
@Composable
fun PlusOne(progress: Float, modifier: Modifier = Modifier) {
    if (progress < 0f) return
    val alpha = when {
        progress < 0.2f -> progress / 0.2f
        else -> 1f - (progress - 0.2f) / 0.8f
    }
    val dy = if (progress < 0.2f) 20f * (1f - progress / 0.2f) else -30f * (progress - 0.2f) / 0.8f
    Text(
        "+1돈",
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            translationY = dy * density
        },
        style = TextStyle(
            color = Palette.GoldHi,
            fontSize = 28.sp,
            fontFamily = SerifFamily,
            fontWeight = FontWeight.ExtraBold,
            shadow = Shadow(Color(0xB3DCAE42), Offset.Zero, 24f),
        ),
    )
}

/* ---------- 토스트 ---------- */

class ToastMsg(val text: String, val id: Long = System.nanoTime())

@Composable
fun GoldToast(msg: ToastMsg?, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf<ToastMsg?>(null) }
    LaunchedEffect(msg?.id) {
        if (msg == null) return@LaunchedEffect
        shown = msg
        visible = true
        delay(2400)
        visible = false
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 },
    ) {
        Box(
            Modifier
                .background(Color(0xF0141210), RoundedCornerShape(20.dp))
                .border(1.dp, Color(0x80DCAE42), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(shown?.text.orEmpty(), color = Palette.GoldHi, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
