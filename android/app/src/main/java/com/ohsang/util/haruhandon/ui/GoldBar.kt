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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
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
                    motion.sway = sin(sec * 0.59f) * 2.5f
                    motion.swayX = cos(sec * 0.43f) * 2f
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
            .drawWithCache {
                val s = size.width / W
                val front = renderFace(measurer, s, don, back = false)
                val back = renderFace(measurer, s, don, back = true)
                val plate = Path().apply { addRoundRect(RoundRect(0f, 0f, W, H, CornerRadius(14f))) }
                val dimFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.1f) })
                val src = floatArrayOf(0f, 0f, W * s, 0f, W * s, H * s, 0f, H * s)
                val faceMatrix = android.graphics.Matrix()
                val quad = Path()

                onDrawBehind {
                    val cam = Slab(
                        yawDeg = motion.totalY + REST_YAW,
                        pitchDeg = REST_PITCH + motion.swayX,
                        pxPerUnit = s,
                        origin = center,
                    )
                    val half = THICK / 2f

                    // 1) 옆면: 둥근 테두리를 따라 이어진 면들 중 카메라를 향한 것만, 방향에 따라 빛을 계산
                    val n = OUTLINE.size
                    for (i in 0 until n) {
                        val p0 = OUTLINE[i]
                        val p1 = OUTLINE[(i + 1) % n]
                        val tx = p1.x - p0.x
                        val ty = p1.y - p0.y
                        val len = kotlin.math.sqrt(tx * tx + ty * ty)
                        if (len < 1e-3f) continue
                        val nx = ty / len
                        val ny = -tx / len
                        if (!cam.facesCamera(nx, ny, 0f, (p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f, 0f)) continue
                        val a = cam.project(p0.x, p0.y, half)
                        val b = cam.project(p1.x, p1.y, half)
                        val c = cam.project(p1.x, p1.y, -half)
                        val d = cam.project(p0.x, p0.y, -half)
                        quad.reset()
                        quad.moveTo(a.x, a.y); quad.lineTo(b.x, b.y); quad.lineTo(c.x, c.y); quad.lineTo(d.x, d.y); quad.close()
                        val col = cam.edgeColor(nx, ny, 0f, dim)
                        // 면을 따라 밝은 쪽 → 어두운 쪽으로 살짝 흐르게 해서 금속 느낌을 낸다
                        val brush = Brush.linearGradient(
                            0f to lerp(col, Color(0xFFFFF0C0), 0.12f),
                            1f to lerp(col, EdgeDark, 0.22f),
                            start = Offset((a.x + d.x) / 2f, (a.y + d.y) / 2f),
                            end = Offset((b.x + c.x) / 2f, (b.y + c.y) / 2f),
                        )
                        drawPath(quad, brush)
                        drawPath(quad, brush, style = Stroke(1.2f)) // 면 사이 틈 메우기
                    }

                    // 2) 카메라를 향한 큰 면(앞 또는 뒤)에 그림을 원근 변형으로 입힌다
                    val showFront = cam.facesCamera(0f, 0f, 1f, 0f, 0f, half)
                    val z = if (showFront) half else -half
                    // 뒷면은 뒤에서 보므로 그림의 왼쪽이 판의 +x 쪽에 온다
                    val xl = if (showFront) -W / 2f else W / 2f
                    val xr = -xl
                    val tl = cam.project(xl, -H / 2f, z)
                    val tr = cam.project(xr, -H / 2f, z)
                    val br = cam.project(xr, H / 2f, z)
                    val bl = cam.project(xl, H / 2f, z)
                    val dst = floatArrayOf(tl.x, tl.y, tr.x, tr.y, br.x, br.y, bl.x, bl.y)
                    if (faceMatrix.setPolyToPoly(src, 0, dst, 0, 4)) {
                        // 원근(perspective) 행렬은 안드로이드 캔버스에 직접 건다
                        drawIntoCanvas { it.nativeCanvas.save(); it.nativeCanvas.concat(faceMatrix) }
                        drawImage(if (showFront) front else back, colorFilter = if (dim) dimFilter else null)
                        scale(s, s, pivot = Offset.Zero) {
                            clipPath(plate) {
                                if (dim) {
                                    drawRect(Color.Black.copy(alpha = 0.45f), size = Size(W, H))
                                } else {
                                    drawReflections(motion.relative, motion.swayX, motion.shine.value)
                                }
                            }
                        }
                        drawIntoCanvas { it.nativeCanvas.restore() }
                    }
                }
            }
    )
}

/* ---------- 3D 판 (원근 투영) ---------- */

/** 판 두께 (설계 좌표, 폭 200 기준) */
private const val THICK = 30f

/** 카메라까지 거리. 작을수록 원근이 세다 */
private const val CAM_DIST = 900f

/** 가만히 있을 때 정면 눈높이 (흔들릴 때 위아래 두께가 살짝 비침) */
private const val REST_YAW = 0f
private const val REST_PITCH = 0f

/** 둥근 모서리 사각형 테두리 (판 중심 기준, 화면 좌표처럼 y는 아래로, 시계 방향) */
private val OUTLINE: List<Offset> = run {
    val r = 14f
    val hw = W / 2f
    val hh = H / 2f
    val seg = 6
    val corners = listOf(
        Offset(-hw + r, -hh + r) to 180f,
        Offset(hw - r, -hh + r) to 270f,
        Offset(hw - r, hh - r) to 0f,
        Offset(-hw + r, hh - r) to 90f,
    )
    buildList {
        corners.forEach { (c, start) ->
            for (k in 0..seg) {
                val a = Math.toRadians((start + 90f * k / seg).toDouble())
                add(Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat()))
            }
        }
    }
}

private val EdgeDark = Color(0xFF573805)
private val EdgeLight = Color(0xFFEDC152)

/** 세로축(yaw) → 가로축(pitch) 순으로 돌리고 원근 투영한다 */
private class Slab(yawDeg: Float, pitchDeg: Float, private val pxPerUnit: Float, private val origin: Offset) {
    private val cy = cos(Math.toRadians(yawDeg.toDouble())).toFloat()
    private val sy = sin(Math.toRadians(yawDeg.toDouble())).toFloat()
    private val cp = cos(Math.toRadians(pitchDeg.toDouble())).toFloat()
    private val sp = sin(Math.toRadians(pitchDeg.toDouble())).toFloat()

    private fun rx(x: Float, z: Float) = x * cy + z * sy
    private fun rz1(x: Float, z: Float) = -x * sy + z * cy
    private fun ry(y: Float, z1: Float) = y * cp - z1 * sp
    private fun rz(y: Float, z1: Float) = y * sp + z1 * cp

    fun project(x: Float, y: Float, z: Float): Offset {
        val x1 = rx(x, z)
        val z1 = rz1(x, z)
        val y2 = ry(y, z1)
        val z2 = rz(y, z1)
        val k = CAM_DIST / (CAM_DIST - z2)
        return Offset(origin.x + x1 * k * pxPerUnit, origin.y + y2 * k * pxPerUnit)
    }

    /** 면의 바깥 방향 (nx,ny,nz)이 점 (px,py,pz)에서 카메라 쪽을 보는지 */
    fun facesCamera(nx: Float, ny: Float, nz: Float, px: Float, py: Float, pz: Float): Boolean {
        val n = rotate(nx, ny, nz)
        val p = rotate(px, py, pz)
        return n[0] * (0f - p[0]) + n[1] * (0f - p[1]) + n[2] * (CAM_DIST - p[2]) > 0f
    }

    /** 왼쪽 위 앞에서 오는 빛 + 반짝임 */
    fun edgeColor(nx: Float, ny: Float, nz: Float, dim: Boolean): Color {
        val n = rotate(nx, ny, nz)
        val diffuse = (n[0] * LX + n[1] * LY + n[2] * LZ).coerceAtLeast(0f)
        val spec = (n[0] * HX + n[1] * HY + n[2] * HZ).coerceAtLeast(0f).let { it * it * it * it * it * it * it * it }
        val t = (0.18f + 0.82f * diffuse).coerceIn(0f, 1f)
        var col = lerp(EdgeDark, EdgeLight, t)
        col = lerp(col, Color(0xFFFFF4CF), (spec * 0.4f).coerceIn(0f, 1f))
        if (dim) {
            val g = (col.red * 0.3f + col.green * 0.59f + col.blue * 0.11f) * 0.5f
            col = Color(g, g, g)
        }
        return col
    }

    private fun rotate(x: Float, y: Float, z: Float): FloatArray {
        val x1 = rx(x, z)
        val z1 = rz1(x, z)
        return floatArrayOf(x1, ry(y, z1), rz(y, z1))
    }

    private companion object {
        // 빛 방향 (화면 기준 왼쪽 위 앞), 반사광용 중간 벡터
        private val L = normalize(-0.45f, -0.65f, 0.62f)
        val LX = L[0]; val LY = L[1]; val LZ = L[2]
        private val Hv = normalize(LX, LY, LZ + 1f)
        val HX = Hv[0]; val HY = Hv[1]; val HZ = Hv[2]

        fun normalize(x: Float, y: Float, z: Float): FloatArray {
            val l = kotlin.math.sqrt(x * x + y * y + z * z)
            return floatArrayOf(x / l, y / l, z / l)
        }
    }
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
