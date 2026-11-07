package com.ohsang.util.haruhandon.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.ohsang.util.haruhandon.data.donLabel
import com.ohsang.util.haruhandon.data.gramLabel
import com.ohsang.util.haruhandon.data.serialLabel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * 압인 골드바. 프로토타입(design/prototype.html)의 200×300 SVG 좌표를 그대로 쓴다.
 * 앞·뒷면의 고정된 그림은 비트맵으로 한 번만 그려 두고, 매 프레임에는 반사광만 다시 그린다.
 */

private const val W = 200f
private const val H = 300f

/** 판 두께 (옆면이 보일 때 최대 폭) */
private const val THICK = 22f

/** 아래쪽으로 보이는 두께 */
private const val BOTTOM = 10f

/** 골드바의 회전·흔들림·반짝임 상태 */
@Stable
class BarMotion {
    /** 잡고 돌린 각도 (기준면 + 끌기) */
    val angle = Animatable(0f)

    /** 마지막으로 멈춘 면의 각도 (0, ±180, ±360 …) */
    var base by mutableFloatStateOf(0f)

    /** 가만히 있을 때 천천히 흔들리는 양 */
    var sway by mutableFloatStateOf(0f)
    var swayX by mutableFloatStateOf(0f)

    var dragging by mutableStateOf(false)

    /** 빛줄기 진행도: -1이면 없음, 0→1로 지나감 */
    val shine = Animatable(-1f)

    val totalY: Float get() = angle.value + sway
    val relative: Float get() = totalY - base
    val showingBack: Boolean get() = isBackAngle(totalY)

    suspend fun sweep() {
        shine.snapTo(0f)
        shine.animateTo(1f, tween(850, easing = FastOutSlowInEasing))
        shine.snapTo(-1f)
    }

    suspend fun reset() {
        angle.snapTo(0f)
        base = 0f
        sway = 0f
        swayX = 0f
        dragging = false
    }
}

fun isBackAngle(deg: Float): Boolean {
    val n = ((deg % 360f) + 360f) % 360f
    return n > 90f && n < 270f
}

/**
 * 금고 밖으로 나온 골드바. 가만히 있으면 천천히 흔들리고,
 * 누르면 [onTap](바 안에서의 비율 좌표), 옆으로 돌려 다른 면에 멈추면 [onFlip].
 */
@Composable
fun InteractiveGoldBar(
    don: Int,
    motion: BarMotion,
    enabled: Boolean,
    dim: Boolean,
    onTap: (fx: Float, fy: Float) -> Unit,
    onFlip: (toBack: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val tap by rememberUpdatedState(onTap)
    val flip by rememberUpdatedState(onFlip)

    LaunchedEffect(enabled) {
        if (!enabled) {
            motion.sway = 0f
            motion.swayX = 0f
            return@LaunchedEffect
        }
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { t ->
                val sec = (t - start) / 1_000_000_000f
                if (!motion.dragging) {
                    motion.sway = sin(sec * 0.59f) * 6f
                    motion.swayX = cos(sec * 0.43f) * 3f
                }
            }
        }
    }

    val gestures = if (!enabled) Modifier else Modifier
        .pointerInput(Unit) {
            detectTapGestures { pos ->
                tap(pos.x / size.width.coerceAtLeast(1), pos.y / size.height.coerceAtLeast(1))
            }
        }
        .pointerInput(Unit) {
            fun settle() {
                motion.dragging = false
                scope.launch {
                    val wasBack = isBackAngle(motion.base)
                    val target = (motion.angle.value / 180f).roundToInt() * 180f
                    motion.base = target
                    val nowBack = isBackAngle(target)
                    if (nowBack != wasBack) flip(nowBack)
                    motion.angle.animateTo(target, spring(dampingRatio = 0.6f, stiffness = 180f))
                }
            }
            detectHorizontalDragGestures(
                onDragStart = { motion.dragging = true },
                onDragEnd = { settle() },
                onDragCancel = { settle() },
            ) { change, dx ->
                change.consume()
                val deg = dx / density * 0.9f
                scope.launch { motion.angle.snapTo(motion.angle.value + deg) }
            }
        }

    GoldBar(don = don, motion = motion, dim = dim, modifier = modifier.then(gestures))
}

@Composable
fun GoldBar(don: Int, motion: BarMotion, dim: Boolean, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer(cacheSize = 32)
    Box(
        modifier
            .aspectRatio(W / H)
            .graphicsLayer {
                rotationX = motion.swayX
                cameraDistance = 16f * density
            }
            .drawWithCache {
                val s = size.width / W
                val front = renderFace(measurer, s, don, back = false)
                val back = renderFace(measurer, s, don, back = true)
                val plate = Path().apply { addRoundRect(RoundRect(0f, 0f, W, H, CornerRadius(14f))) }
                val dimFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.1f) })

                onDrawBehind {
                    /*
                     * 두께가 있는 판을 세로축으로 돌린 모습을 직접 그린다.
                     * 앞면 중심은 (T/2)·sinθ 만큼 옆으로 밀리고 폭은 W·|cosθ|,
                     * 옆면은 폭 T·|sinθ| 로 앞면 바로 옆에 붙어 보인다.
                     */
                    val rad = Math.toRadians(motion.totalY.toDouble())
                    val c = cos(rad).toFloat()
                    val sn = sin(rad).toFloat()
                    val isBack = c < 0f
                    val ac = abs(c).coerceAtLeast(0.002f)
                    val cx = W / 2f
                    val faceCx = cx + THICK / 2f * sn
                    val faceHalf = W / 2f * ac
                    val sideW = THICK * abs(sn)

                    scale(s, s, pivot = Offset.Zero) {
                        if (sideW > 0.3f) {
                            // sinθ<0 이면 오른쪽 옆면, sinθ>0 이면 왼쪽 옆면이 보인다
                            val sideCx = cx + if (sn < 0f) W / 2f * c else -W / 2f * c
                            val l = sideCx - sideW / 2f
                            val r = sideCx + sideW / 2f
                            val lit = sideCx < faceCx
                            val brush = if (lit) {
                                Brush.horizontalGradient(
                                    0f to Color(0xFFC08A24), 0.6f to Color(0xFFEFCB6E), 1f to Color(0xFFF8DE92),
                                    startX = l, endX = r,
                                )
                            } else {
                                Brush.horizontalGradient(
                                    0f to Color(0xFFA36F14), 0.5f to Color(0xFF8A5C0E), 1f to Color(0xFF5E3D06),
                                    startX = l, endX = r,
                                )
                            }
                            drawRoundRect(
                                brush, Offset(l, 0f), Size(sideW, H + BOTTOM),
                                CornerRadius(min(sideW / 2f, 12f), 12f),
                            )
                            // 모서리 하이라이트
                            val edgeX = if (lit) l + 0.6f else r - 0.6f
                            drawLine(Color(0x66FFF6D2), Offset(edgeX, 10f), Offset(edgeX, H + BOTTOM - 10f), 1f)
                        }
                        // 아래쪽 두께
                        drawRoundRect(
                            Brush.verticalGradient(0f to Color(0xFFA8741A), 1f to Color(0xFF5E3D06), startY = H - 14f, endY = H + BOTTOM),
                            Offset(faceCx - faceHalf, H - 14f), Size(faceHalf * 2f, 14f + BOTTOM),
                            CornerRadius(min(14f * ac, faceHalf), 14f),
                        )
                    }

                    // 앞면 또는 뒷면 (뒷면도 글자가 바로 읽히게 그대로 그린다)
                    withTransform({
                        translate(left = (faceCx - faceHalf) * s)
                        scale(ac, 1f, pivot = Offset.Zero)
                    }) {
                        drawImage(if (isBack) back else front, colorFilter = if (dim) dimFilter else null)
                        scale(s, s, pivot = Offset.Zero) {
                            clipPath(plate) {
                                if (dim) {
                                    drawRect(Color.Black.copy(alpha = 0.45f), size = Size(W, H))
                                } else {
                                    drawReflections(motion.relative, motion.swayX, motion.shine.value)
                                }
                            }
                        }
                    }
                }
            }
    )
}

/* ---------- 고정된 면 그림 ---------- */

private class Engr(val layout: TextLayoutResult, val cx: Float, val baseline: Float)

private fun CacheDrawScope.measureText(
    measurer: TextMeasurer, text: String, px: Float, family: FontFamily,
    weight: FontWeight, spacing: Float = 0f,
): TextLayoutResult = measurer.measure(
    text,
    TextStyle(
        fontSize = px.toSp(),
        fontFamily = family,
        fontWeight = weight,
        letterSpacing = spacing.toSp(),
    ),
)

private fun CacheDrawScope.renderFace(measurer: TextMeasurer, s: Float, don: Int, back: Boolean): ImageBitmap {
    val wPx = ceil(W * s).toInt().coerceAtLeast(1)
    val hPx = ceil(H * s).toInt().coerceAtLeast(1)
    val bmp = ImageBitmap(wPx, hPx)

    val texts: List<Engr>
    var pattern: TextLayoutResult? = null
    if (!back) {
        texts = listOf(
            Engr(measureText(measurer, "金", 25f, SerifFamily, FontWeight.ExtraBold), 100f, 67f),
            Engr(measureText(measurer, "FINE GOLD", 10f, SansFamily, FontWeight.SemiBold, 4f), 100f, 108f),
            Engr(measureText(measurer, "999.9", 31f, SerifFamily, FontWeight.ExtraBold, 1f), 100f, 143f),
            Engr(measureText(measurer, donLabel(don), 25f, SerifFamily, FontWeight.ExtraBold), 100f, 191f),
            Engr(measureText(measurer, gramLabel(don), 10.5f, SansFamily, FontWeight.Medium, 1.5f), 100f, 211f),
            Engr(measureText(measurer, "하루 한돈", 10f, SerifFamily, FontWeight.Bold, 3f), 100f, 248f),
            Engr(measureText(measurer, serialLabel(don), 8f, MonoFamily, FontWeight.Medium, 1.2f), 100f, 266f),
        )
    } else {
        pattern = measureText(measurer, "金", 10f, SerifFamily, FontWeight.ExtraBold)
        texts = listOf(
            Engr(measureText(measurer, "OHSANG VAULT", 8.5f, SansFamily, FontWeight.SemiBold, 3f), 100f, 43f),
            Engr(measureText(measurer, "福", 64f, SerifFamily, FontWeight.ExtraBold), 100f, 151f),
            Engr(measureText(measurer, "행복하세요", 17f, SerifFamily, FontWeight.ExtraBold, 1f), 100f, 218f),
            Engr(measureText(measurer, "돈 많이 버세요", 12f, SerifFamily, FontWeight.Bold, 1f), 100f, 240f),
            Engr(measureText(measurer, serialLabel(don), 8f, MonoFamily, FontWeight.Medium, 1.2f), 100f, 267f),
        )
    }

    CanvasDrawScope().draw(this, layoutDirection, Canvas(bmp), Size(wPx.toFloat(), hPx.toFloat())) {
        scale(s, s, pivot = Offset.Zero) {
            drawPlate()
            if (back) {
                drawBackPattern(pattern!!)
                // 각인 자리는 무늬 없이 매끈하게
                drawRoundRect(FieldBrush, Offset(44f, 30f), Size(112f, 18f), CornerRadius(4f))
                drawCircle(FieldBrush, 56f, Offset(100f, 128f))
                drawRoundRect(FieldBrush, Offset(30f, 194f), Size(140f, 82f), CornerRadius(6f))
                engraveCircle(Offset(100f, 128f), 52f, 2.2f)
                engraveCircle(Offset(100f, 128f), 45f, 0.8f)
                engraveLine(Offset(60f, 252f), Offset(140f, 252f), 1f)
            } else {
                engraveCircle(Offset(100f, 58f), 25f, 2f)
                engraveCircle(Offset(100f, 58f), 20f, 0.8f)
                engraveLine(Offset(50f, 157f), Offset(150f, 157f), 1.2f)
            }
            texts.forEach { engraveText(it) }
        }
    }
    return bmp
}

private val PlateBrush = Brush.linearGradient(
    0f to Color(0xFFFFF2C2), 0.16f to Color(0xFFEBC25A), 0.3f to Color(0xFFB7871F),
    0.44f to Color(0xFFF6DC8C), 0.58f to Color(0xFFD7A538), 0.76f to Color(0xFF9A6B14),
    0.9f to Color(0xFFD9AC45), 1f to Color(0xFFF3D27A),
    start = Offset(0f, 0f), end = Offset(W, H),
)
private val FieldBrush = Brush.linearGradient(
    0f to Color(0xFFB98523), 0.3f to Color(0xFFE7BD55), 0.5f to Color(0xFFFBE5A0),
    0.68f to Color(0xFFE2B44A), 1f to Color(0xFFA7771B),
    start = Offset(0f, H), end = Offset(W, 0f),
)
private val BevelBrush = Brush.linearGradient(
    0f to Color(0xFFFFFAE6), 0.5f to Color(0xFFD9A940), 1f to Color(0xFF6E4A0B),
    start = Offset(0f, 0f), end = Offset(W, H),
)
private val BevelInBrush = Brush.linearGradient(
    0f to Color(0xFFFFF6D2), 0.5f to Color(0xFFC8952E), 1f to Color(0xFF5F3F08),
    start = Offset(W, H), end = Offset(0f, 0f),
)

private fun DrawScope.drawPlate() {
    drawRoundRect(PlateBrush, Offset.Zero, Size(W, H), CornerRadius(14f))
    drawRoundRect(Color(0x99FFF7D8), Offset(1f, 1f), Size(198f, 298f), CornerRadius(13.5f), style = Stroke(1f))
    drawRoundRect(BevelBrush, Offset(11f, 11f), Size(178f, 278f), CornerRadius(8f), style = Stroke(3f))
    drawRoundRect(FieldBrush, Offset(14f, 14f), Size(172f, 272f), CornerRadius(6f))
    drawRoundRect(BevelInBrush, Offset(14f, 14f), Size(172f, 272f), CornerRadius(6f), style = Stroke(1.6f))
}

/** 뒷면: 비스듬히 반복되는 金 무늬 */
private fun DrawScope.drawBackPattern(mark: TextLayoutResult) {
    clipRect(15f, 15f, 185f, 285f) {
        rotate(-30f, pivot = Offset(100f, 150f)) {
            var y = -80f
            while (y < 380f) {
                var x = -110f
                while (x < 310f) {
                    drawText(mark, Palette.EngraveLight, Offset(x + 0.6f, y + 0.6f), alpha = 0.45f)
                    drawText(mark, Color(0xFF9A6B14), Offset(x, y), alpha = 0.4f)
                    x += 24f
                }
                y += 24f
            }
        }
    }
}

/** 새긴 글자: 아래 그림자, 위 하이라이트, 가운데 금색 */
private fun DrawScope.engraveText(e: Engr) {
    val tl = Offset(e.cx - e.layout.size.width / 2f, e.baseline - e.layout.firstBaseline)
    drawText(e.layout, Palette.EngraveShadow, tl + Offset(0.7f, 0.7f), alpha = 0.55f)
    drawText(e.layout, Palette.EngraveLight, tl + Offset(-0.5f, -0.5f), alpha = 0.85f)
    drawText(e.layout, Palette.Engrave, tl)
}

private fun DrawScope.engraveCircle(c: Offset, r: Float, w: Float) {
    drawCircle(Palette.EngraveShadow, r, c + Offset(0.7f, 0.7f), alpha = 0.55f, style = Stroke(w))
    drawCircle(Palette.EngraveLight, r, c + Offset(-0.5f, -0.5f), alpha = 0.85f, style = Stroke(w))
    drawCircle(Palette.Engrave, r, c, style = Stroke(w))
}

private fun DrawScope.engraveLine(a: Offset, b: Offset, w: Float) {
    drawLine(Palette.EngraveShadow, a + Offset(0.7f, 0.7f), b + Offset(0.7f, 0.7f), w, alpha = 0.55f)
    drawLine(Palette.EngraveLight, a + Offset(-0.5f, -0.5f), b + Offset(-0.5f, -0.5f), w, alpha = 0.85f)
    drawLine(Palette.Engrave, a, b, w)
}

/* ---------- 매 프레임 반사광 ---------- */

/** CSS skewX(-22deg)와 같은 기울임 */
private val Skew = Matrix().apply { values[Matrix.SkewX] = -0.404f }

private fun DrawScope.drawReflections(relativeDeg: Float, tiltX: Float, shine: Float) {
    val x = 80f + sin(Math.toRadians(relativeDeg.toDouble())).toFloat() * 260f - tiltX * 2.5f
    withTransform({ transform(Skew) }) {
        band(x - 40f, 54f, 0.55f)
        band(x + 22f, 16f, 0.38f)
        if (shine >= 0f) band(-150f + shine * 570f - 60f, 80f, 0.95f)
    }
}

private fun DrawScope.band(left: Float, width: Float, peak: Float) {
    val brush = Brush.horizontalGradient(
        0f to Color.White.copy(alpha = 0f),
        0.45f to Color.White.copy(alpha = peak),
        0.55f to Color.White.copy(alpha = peak),
        1f to Color.White.copy(alpha = 0f),
        startX = left, endX = left + width,
    )
    drawRect(brush, Offset(left, -40f), Size(width, 380f))
}
