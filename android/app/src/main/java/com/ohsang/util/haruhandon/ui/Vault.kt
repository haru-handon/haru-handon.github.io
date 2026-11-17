package com.ohsang.util.haruhandon.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.ohsang.util.haruhandon.data.Category
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** 문이 다 열린 각도 */
const val DOOR_OPEN_DEG = -112f

/**
 * 금고. 몸체·안쪽 → 골드바 → 문 → 경첩 순으로 쌓는다.
 * 골드바가 앞으로 나오면([lift] > 0) 문보다 위로 올라온다.
 */
@Composable
fun VaultStage(
    tier: Int,
    doorAngle: Float,
    dialAngle: Float,
    leverTurn: Float,
    lift: Float,
    showHint: Boolean,
    onDoorTap: () -> Unit,
    modifier: Modifier = Modifier,
    decor: Set<Category> = emptySet(),
    fresh: Category? = null,
    freshProgress: Float = 1f,
    bar: @Composable () -> Unit,
) {
    val measurer = rememberTextMeasurer()
    val look = Decor(decor, fresh, freshProgress)
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val w = min(maxWidth * 0.84f, maxHeight * 0.82f / 1.08f)
        val h = w * 1.08f
        val open = (doorAngle / DOOR_OPEN_DEG).coerceIn(0f, 1f)

        Box(Modifier.size(w, h)) {
            Canvas(Modifier.fillMaxSize()) { drawSafeBody(tier, open, look) }

            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.54f)
                    .zIndex(if (lift > 0.02f) 4f else 1f)
                    .graphicsLayer {
                        val sc = 1f + 0.3f * lift
                        scaleX = sc
                        scaleY = sc
                    }
            ) { bar() }

            Box(
                Modifier
                    .fillMaxSize()
                    .padding(w * 0.055f)
                    .zIndex(2f)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        rotationY = doorAngle
                        cameraDistance = 12f * density
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClickLabel = "금고 열기",
                        onClick = onDoorTap,
                    )
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    if (abs(doorAngle) < 90f) {
                        drawDoorFace(tier, dialAngle, leverTurn, look, measurer)
                    } else {
                        scale(-1f, 1f, pivot = center) { drawDoorBack() }
                    }
                }
                if (showHint) DoorHint(Modifier.align(BiasAlignment(0f, 0.62f)))
            }

            Canvas(Modifier.fillMaxSize().zIndex(3f)) { drawHinges() }

            // 재물 장식: 금고 위에 얹힌 엽전 (문이 열려도 보인다)
            if (look.has(Category.MONEY)) {
                Canvas(Modifier.fillMaxSize().zIndex(3f)) { drawCoinCrest(look.alpha(Category.MONEY)) }
            }
        }
    }
}

@Composable
private fun DoorHint(modifier: Modifier) {
    val pulse = rememberInfiniteTransition(label = "hint")
    val alpha by pulse.animateFloat(
        initialValue = 0.8f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "hintAlpha",
    )
    Text(
        "터치해서 열기",
        modifier = modifier.graphicsLayer { this.alpha = alpha },
        color = Palette.GoldHi,
        fontSize = 11.sp,
        letterSpacing = 0.9.sp,
        fontWeight = FontWeight.Medium,
    )
}

/* ---------- 몸체 ---------- */

private fun DrawScope.drawSafeBody(tier: Int, open: Float, look: Decor) {
    val r = CornerRadius(16.dp.toPx())
    // 다리
    val footW = size.width * 0.14f
    val footH = 9.dp.toPx()
    listOf(0.10f, 0.76f).forEach { fx ->
        drawRoundRect(
            Color(0xFF15171A), Offset(size.width * fx, size.height - footH), Size(footW, footH * 2),
            CornerRadius(4.dp.toPx()),
        )
    }
    drawRoundRect(
        Brush.linearGradient(
            0f to Color(0xFF41464E), 0.55f to Color(0xFF262A2F), 1f to Color(0xFF1A1D21),
            start = Offset.Zero, end = Offset(size.width, size.height),
        ),
        cornerRadius = r,
    )
    drawRoundRect(Color(0xFF15171A), cornerRadius = r, style = Stroke(2.dp.toPx()))
    drawLine(Color.White.copy(alpha = 0.14f), Offset(r.x, 1.dp.toPx()), Offset(size.width - r.x, 1.dp.toPx()), 1.dp.toPx())

    // 안쪽
    val inset = size.width * 0.075f
    val tl = Offset(inset, inset)
    val isz = Size(size.width - inset * 2, size.height - inset * 2)
    val ir = CornerRadius(9.dp.toPx())
    drawRoundRect(
        Brush.radialGradient(
            0f to Color(0xFF2B1F0B), 0.75f to Color(0xFF0D0A05), 1f to Color(0xFF0D0A05),
            center = Offset(size.width / 2, tl.y + isz.height * 0.85f),
            radius = isz.width * 0.9f,
        ),
        tl, isz, ir,
    )
    drawRoundRect(Color.Black, tl, isz, ir, style = Stroke(1.dp.toPx()))
    if (open > 0f) {
        drawRoundRect(
            Brush.radialGradient(
                0f to Color(0x59FBE7A6), 1f to Color.Transparent,
                center = Offset(size.width / 2, tl.y + isz.height * 0.55f),
                radius = isz.width * 0.7f,
            ),
            tl, isz, ir, alpha = open,
        )
    }
}

private fun DrawScope.drawHinges() {
    val hw = 9.dp.toPx()
    val hh = size.height * 0.13f
    val brush = Brush.horizontalGradient(
        0f to Color(0xFF1B1E22), 0.5f to Color(0xFF4D535B), 1f to Color(0xFF1B1E22),
        startX = -5.dp.toPx(), endX = -5.dp.toPx() + hw,
    )
    listOf(size.height * 0.16f, size.height * (1f - 0.16f - 0.13f)).forEach { y ->
        drawRoundRect(brush, Offset(-5.dp.toPx(), y), Size(hw, hh), CornerRadius(3.dp.toPx()))
    }
}

/* ---------- 문 ---------- */

private fun DrawScope.drawDoorFace(tier: Int, dialAngle: Float, leverTurn: Float, look: Decor, measurer: TextMeasurer) {
    val r = CornerRadius(11.dp.toPx())
    drawRoundRect(
        Brush.linearGradient(listOf(Palette.DoorHi[tier], Palette.DoorLo[tier]), Offset.Zero, Offset(size.width, size.height)),
        cornerRadius = r,
    )
    val s7 = 7.dp.toPx()
    drawRoundRect(
        Color.Black.copy(alpha = 0.35f), Offset(s7 / 2, s7 / 2), Size(size.width - s7, size.height - s7),
        CornerRadius(r.x - s7 / 2), style = Stroke(5.dp.toPx()),
    )
    drawRoundRect(Palette.Rim[tier], cornerRadius = r, style = Stroke(2.dp.toPx()))
    drawLine(Color.White.copy(alpha = 0.15f), Offset(r.x, 2.dp.toPx()), Offset(size.width - r.x, 2.dp.toPx()), 1.dp.toPx())

    // 리벳
    for (fx in listOf(0.07f, 0.93f)) for (fy in listOf(0.07f, 0.93f)) {
        drawCircle(Color(0xFF9AA0A8), 2.dp.toPx(), Offset(size.width * fx, size.height * fy))
    }
    // 성실 장식: 문 테두리를 따라 금 징
    if (look.has(Category.STEADY)) drawStuds(look.alpha(Category.STEADY))
    // 여유 장식: 네 모서리 구름 장석
    if (look.has(Category.EASE)) drawCloudCorners(look.alpha(Category.EASE))

    val cx = size.width / 2
    // 다이얼 위 표시
    val py = size.height * 0.12f
    drawPath(
        Path().apply {
            moveTo(cx - 6.dp.toPx(), py)
            lineTo(cx + 6.dp.toPx(), py)
            lineTo(cx, py + 9.dp.toPx())
            close()
        },
        Palette.Gold,
    )

    // 다이얼
    val c = Offset(cx, size.height * 0.43f)
    val outer = size.width * 0.27f
    drawCircle(Color(0xFF15171A), outer, c)
    drawCircle(Color(0xFF2A2E33), outer * 0.97f, c, style = Stroke(2.dp.toPx()))
    val ring = outer * 0.88f
    rotate(dialAngle, pivot = c) {
        drawCircle(Brush.radialGradient(listOf(Color(0xFF4A5058), Color(0xFF2B2F34)), c, ring), ring, c)
        for (i in 0 until 36) {
            val a = Math.toRadians(i * 10.0 - 90.0)
            val major = i % 3 == 0
            val r1 = ring * 0.97f
            val r2 = ring * if (major) 0.76f else 0.85f
            drawLine(
                Color(0xFFE2E5E9),
                c + Offset((cos(a) * r1).toFloat(), (sin(a) * r1).toFloat()),
                c + Offset((cos(a) * r2).toFloat(), (sin(a) * r2).toFloat()),
                if (major) 1.6.dp.toPx() else 1.dp.toPx(),
            )
        }
        drawCircle(Color(0xFF0F1012), ring * 0.64f, c, style = Stroke(1.dp.toPx()))
        val knob = ring * 0.56f
        drawCircle(
            Brush.radialGradient(
                0f to Color(0xFFA3A9B1), 0.38f to Color(0xFF5D636B), 1f to Color(0xFF2A2E33),
                center = c + Offset(-knob * 0.25f, -knob * 0.4f), radius = knob * 1.3f,
            ),
            knob, c,
        )
        // 행운 장식: 금빛 福 손잡이
        if (look.has(Category.LUCK)) drawLuckKnob(c, knob, look.alpha(Category.LUCK), measurer)
        drawRoundRect(
            Palette.Gold, Offset(c.x - 2.dp.toPx(), c.y - knob * 0.9f), Size(4.dp.toPx(), knob * 0.28f),
            CornerRadius(2.dp.toPx()),
        )
    }

    // 손잡이
    val lw = size.width * 0.34f
    val lh = size.height * 0.07f
    val ly = size.height * (1f - 0.11f - 0.07f)
    rotate(-28f * leverTurn, pivot = Offset(cx, ly + lh / 2)) {
        drawRoundRect(
            Brush.verticalGradient(
                0f to Color(0xFF9AA0A8), 0.6f to Color(0xFF4D535B), 1f to Color(0xFF2A2E33),
                startY = ly, endY = ly + lh,
            ),
            Offset(cx - lw / 2, ly), Size(lw, lh), CornerRadius(lh / 2),
        )
    }
    // 인연 장식: 손잡이에 매단 매듭 노리개
    if (look.has(Category.BOND)) drawTassel(Offset(cx + lw * 0.36f, ly + lh * 0.5f), look.alpha(Category.BOND))
}

/* ---------- 문구 도감 장식 (카테고리를 다 모으면 생긴다) ---------- */

class Decor(private val set: Set<Category>, private val fresh: Category?, private val progress: Float) {
    fun has(c: Category) = c in set
    fun alpha(c: Category) = if (c == fresh) progress.coerceIn(0f, 1f) else 1f
}

private val GoldFit = listOf(Color(0xFFFFF0B8), Color(0xFFE2B24A), Color(0xFF9C7123))

private fun goldBrush(a: Offset, b: Offset) = Brush.linearGradient(GoldFit, a, b)

/** 재물: 금고 꼭대기에 얹힌 엽전 */
private fun DrawScope.drawCoinCrest(alpha: Float) {
    val r = size.width * 0.085f
    val c = Offset(size.width / 2, -r * 0.35f)
    // 받침
    drawRoundRect(
        goldBrush(Offset(c.x - r, 0f), Offset(c.x + r, r * 0.4f)),
        Offset(c.x - r * 0.75f, -r * 0.05f), Size(r * 1.5f, r * 0.42f), CornerRadius(r * 0.12f), alpha = alpha,
    )
    drawCircle(Color.Black.copy(alpha = 0.35f * alpha), r * 1.04f, c + Offset(0f, r * 0.08f))
    drawCircle(goldBrush(c - Offset(r, r), c + Offset(r, r)), r, c, alpha = alpha)
    drawCircle(Color(0xFF7A5718), r * 0.82f, c, alpha = alpha, style = Stroke(r * 0.07f))
    val h = r * 0.3f
    drawRect(Color(0xFF2A1C04), Offset(c.x - h, c.y - h), Size(h * 2, h * 2), alpha = alpha)
    drawRect(Color(0xFFFFF0B8), Offset(c.x - h, c.y - h), Size(h * 2, h * 2), alpha = 0.5f * alpha, style = Stroke(r * 0.04f))
}

/** 행운: 다이얼 손잡이가 금빛으로 바뀌고 福 자가 새겨진다 */
private fun DrawScope.drawLuckKnob(c: Offset, knob: Float, alpha: Float, measurer: TextMeasurer) {
    drawCircle(
        Brush.radialGradient(
            0f to Color(0xFFFFF3C4), 0.4f to Color(0xFFE2B24A), 1f to Color(0xFF8A6316),
            center = c + Offset(-knob * 0.25f, -knob * 0.4f), radius = knob * 1.3f,
        ),
        knob, c, alpha = alpha,
    )
    drawCircle(Color(0xFF7A5718), knob * 0.86f, c, alpha = alpha, style = Stroke(knob * 0.05f))
    val layout = measurer.measure(
        "福",
        TextStyle(color = Color(0xFF6B4708), fontSize = (knob * 0.95f / density / fontScale).sp, fontWeight = FontWeight.Black),
    )
    drawText(layout, topLeft = c - Offset(layout.size.width / 2f, layout.size.height / 2f), alpha = alpha)
}

/** 인연: 붉은 매듭과 술 */
private fun DrawScope.drawTassel(top: Offset, alpha: Float) {
    val u = size.width * 0.016f
    val red = Color(0xFFB0262C)
    drawLine(red, top, top + Offset(0f, u * 1.6f), u * 0.5f, alpha = alpha)
    // 매듭 (마름모)
    val k = top + Offset(0f, u * 3f)
    drawPath(
        Path().apply {
            moveTo(k.x, k.y - u * 1.5f); lineTo(k.x + u * 1.5f, k.y); lineTo(k.x, k.y + u * 1.5f); lineTo(k.x - u * 1.5f, k.y); close()
        },
        red, alpha = alpha,
    )
    drawCircle(goldBrush(k - Offset(u, u), k + Offset(u, u)), u * 0.7f, k, alpha = alpha)
    // 금 고리 + 술
    val ring = k + Offset(0f, u * 2.4f)
    drawRoundRect(goldBrush(ring, ring + Offset(u * 2, u)), Offset(ring.x - u * 1.1f, ring.y - u * 0.5f), Size(u * 2.2f, u * 1.1f), CornerRadius(u * 0.4f), alpha = alpha)
    val len = u * 6.5f
    for (i in -4..4) {
        val x0 = ring.x + i * u * 0.22f
        drawLine(red, Offset(x0, ring.y + u * 0.5f), Offset(ring.x + i * u * 0.42f, ring.y + len), u * 0.32f, alpha = alpha)
    }
}

/** 성실: 문 테두리를 따라 박힌 금 징 */
private fun DrawScope.drawStuds(alpha: Float) {
    val inset = size.width * 0.045f
    val r = 1.9.dp.toPx()
    val n = 9
    for (i in 1 until n) {
        val x = inset + (size.width - inset * 2) * i / n
        val y = inset + (size.height - inset * 2) * i / n
        listOf(Offset(x, inset), Offset(x, size.height - inset), Offset(inset, y), Offset(size.width - inset, y)).forEach { p ->
            drawCircle(goldBrush(p - Offset(r, r), p + Offset(r, r)), r, p, alpha = alpha)
        }
    }
}

/** 여유: 네 모서리에 구름 모양 금 장석 */
private fun DrawScope.drawCloudCorners(alpha: Float) {
    val arm = size.width * 0.17f
    val t = size.width * 0.032f
    val m = size.width * 0.03f
    for ((sx, sy) in listOf(1f to 1f, -1f to 1f, 1f to -1f, -1f to -1f)) {
        val o = Offset(if (sx > 0) m else size.width - m, if (sy > 0) m else size.height - m)
        fun pt(dx: Float, dy: Float) = Offset(o.x + dx * sx, o.y + dy * sy)
        val b = goldBrush(pt(0f, 0f), pt(arm, arm))
        val h0 = pt(0f, 0f); val h1 = pt(arm, t)
        drawRect(b, Offset(minOf(h0.x, h1.x), minOf(h0.y, h1.y)), Size(arm, t), alpha = alpha)
        val v1 = pt(t, arm)
        drawRect(b, Offset(minOf(h0.x, v1.x), minOf(h0.y, v1.y)), Size(t, arm), alpha = alpha)
        // 구름 꽃잎
        drawCircle(b, t * 1.25f, pt(t * 0.9f, t * 0.9f), alpha = alpha)
        drawCircle(b, t * 0.85f, pt(arm, t / 2), alpha = alpha)
        drawCircle(b, t * 0.85f, pt(t / 2, arm), alpha = alpha)
        drawCircle(b, t * 0.6f, pt(arm * 0.62f, t * 1.15f), alpha = alpha)
        drawCircle(b, t * 0.6f, pt(t * 1.15f, arm * 0.62f), alpha = alpha)
        drawCircle(Color(0xFF6B4708), t * 0.28f, pt(t * 0.9f, t * 0.9f), alpha = alpha)
    }
}

private fun DrawScope.drawDoorBack() {
    val r = CornerRadius(11.dp.toPx())
    drawRoundRect(
        Brush.linearGradient(listOf(Color(0xFF2A2E33), Color(0xFF141619)), Offset(size.width, 0f), Offset(0f, size.height)),
        cornerRadius = r,
    )
    drawRoundRect(Color(0xFF0C0D0F), cornerRadius = r, style = Stroke(2.dp.toPx()))
    val bw = size.width * 0.16f
    val bh = size.height * 0.05f
    val bolt = Brush.verticalGradient(listOf(Color(0xFF9AA0A8), Color(0xFF4D535B)))
    listOf(0.25f, 0.70f).forEach { fy ->
        drawRoundRect(bolt, Offset(size.width - bw * 0.8f, size.height * fy), Size(bw, bh), CornerRadius(3.dp.toPx()))
    }
}
