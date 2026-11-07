package com.ohsang.util.haruhandon.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ohsang.util.haruhandon.R
import com.ohsang.util.haruhandon.data.gramLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** 10돈마다 나오는 냥 달성 축하 */
data class CelebrationInfo(val don: Int, val upgradedTo: String?)

private class Falling(val x: Float, val width: Float, val delayMs: Long, val durationMs: Int, val spin: Float)

@Composable
fun NyangCelebration(info: CelebrationInfo, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val nyang = info.don / 10
    val rays = rememberInfiniteTransition(label = "rays")
    val rot by rays.animateFloat(0f, 360f, infiniteRepeatable(tween(14_000, easing = LinearEasing)), label = "raysRot")

    val fade = remember { Animatable(0f) }
    val pop = remember { Animatable(0.3f) }
    LaunchedEffect(info) {
        launch { fade.animateTo(1f, tween(500)) }
        delay(100)
        pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 220f))
    }

    val bars = remember(info) {
        List(22) {
            Falling(
                x = 0.04f + Random.nextFloat() * 0.88f,
                width = 18f + Random.nextFloat() * 18f,
                delayMs = it * 85L,
                durationMs = 1600 + Random.nextInt(900),
                spin = Random.nextFloat() * 120f - 60f,
            )
        }
    }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.value }
            .background(
                Brush.radialGradient(
                    0f to Color(0xEB42300C), 0.7f to Color(0xF208080A), 1f to Color(0xF208080A),
                )
            )
            // 뒤의 금고가 눌리지 않게 막는다
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        val heightPx = constraints.maxHeight.toFloat()
        val widthDp = maxWidth.value

        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height * 0.45f)
            val radius = size.minDimension * 0.95f
            val brush = Brush.radialGradient(
                0f to Color(0x2EFBE7A6), 0.35f to Color(0x2EFBE7A6), 1f to Color.Transparent,
                center = c, radius = radius,
            )
            rotate(rot, pivot = c) {
                for (i in 0 until 18) {
                    val a0 = Math.toRadians(i * 20.0)
                    val a1 = Math.toRadians(i * 20.0 + 8.0)
                    val p = Path().apply {
                        moveTo(c.x, c.y)
                        lineTo(c.x + (cos(a0) * radius).toFloat(), c.y + (sin(a0) * radius).toFloat())
                        lineTo(c.x + (cos(a1) * radius).toFloat(), c.y + (sin(a1) * radius).toFloat())
                        close()
                    }
                    drawPath(p, brush)
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            bars.forEach { FallingBar(it, widthDp, heightPx) }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            Text("축하해요", color = Palette.Gold, fontSize = 12.sp, letterSpacing = 3.sp)
            Text(
                "${nyang}냥",
                modifier = Modifier.graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                    alpha = ((pop.value - 0.3f) / 0.7f).coerceIn(0f, 1f)
                },
                style = TextStyle(
                    brush = Brush.verticalGradient(
                        0.1f to Color(0xFFFFF6D2), 0.5f to Color(0xFFF0C75A), 0.92f to Color(0xFFA8761A),
                    ),
                    fontSize = 80.sp,
                    fontFamily = SerifFamily,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
            Text("금 ${info.don}돈 · ${gramLabel(info.don)} 모았어요", color = Palette.Text, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                info.upgradedTo?.let { "${it}로 업그레이드" } ?: "다음 목표 ${nyang + 1}냥",
                color = Palette.GoldHi,
                fontSize = 12.5.sp,
                modifier = Modifier
                    .border(1.dp, Color(0x73DCAE42), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(26.dp))
            Box(
                Modifier
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF3A2F17), Color(0xFF211A0D))),
                        RoundedCornerShape(22.dp),
                    )
                    .border(1.dp, Palette.GoldLo, RoundedCornerShape(22.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 30.dp, vertical = 11.dp)
            ) {
                Text("금고로 돌아가기", color = Palette.GoldHi, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FallingBar(f: Falling, widthDp: Float, heightPx: Float) {
    val t = remember { Animatable(0f) }
    LaunchedEffect(f) {
        delay(f.delayMs)
        t.animateTo(1f, tween(f.durationMs, easing = FastOutLinearInEasing))
    }
    if (t.value <= 0f || t.value >= 1f) return
    Image(
        painter = painterResource(R.drawable.goldbar_mini),
        contentDescription = null,
        modifier = Modifier
            .offset(x = (widthDp * f.x).dp, y = 0.dp)
            .size(f.width.dp, (f.width * 1.5f).dp)
            .graphicsLayer {
                translationY = -70f * density + t.value * (heightPx + 140f * density)
                rotationZ = f.spin * t.value
            },
    )
}
