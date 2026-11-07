package com.ohsang.play.haruhandon.ui

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
import androidx.compose.ui.unit.dp
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
    bar: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val w = min(maxWidth * 0.84f, maxHeight * 0.82f / 1.08f)
        val h = w * 1.08f
        val open = (doorAngle / DOOR_OPEN_DEG).coerceIn(0f, 1f)

        Box(Modifier.size(w, h)) {
            Canvas(Modifier.fillMaxSize()) { drawSafeBody(tier, open) }

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
                        drawDoorFace(tier, dialAngle, leverTurn)
                    } else {
                        scale(-1f, 1f, pivot = center) { drawDoorBack() }
                    }
                }
                if (showHint) DoorHint(Modifier.align(BiasAlignment(0f, 0.62f)))
            }

            Canvas(Modifier.fillMaxSize().zIndex(3f)) { drawHinges() }
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

private fun DrawScope.drawSafeBody(tier: Int, open: Float) {
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

private fun DrawScope.drawDoorFace(tier: Int, dialAngle: Float, leverTurn: Float) {
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
