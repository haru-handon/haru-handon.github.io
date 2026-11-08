package com.ohsang.util.haruhandon.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ohsang.util.haruhandon.MainActivity
import com.ohsang.util.haruhandon.R
import com.ohsang.util.haruhandon.data.Fortunes
import com.ohsang.util.haruhandon.data.HandonStore
import com.ohsang.util.haruhandon.ui.renderGoldBarBitmap

object HandonWidgets {
    suspend fun updateAll(context: Context) {
        SmallGoldWidget().updateAll(context)
        WideGoldWidget().updateAll(context)
    }
}

private val Fortune = ColorProvider(Color(0xFFF3E6C4))

/** 2×2: 배경 없이 골드바만 홈 화면 위에 놓인 것처럼. 그림자는 그림에 들어 있다. */
class SmallGoldWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val don = HandonStore(context).totalDon()
        val bar = renderGoldBarBitmap(context, don, landscape = false, longSidePx = 520, shadow = true)
        provideContent { SmallContent(bar) }
    }
}

@Composable
private fun SmallContent(bar: Bitmap) {
    val size = LocalSize.current
    val ratio = bar.width.toFloat() / bar.height
    val pad = 2.dp
    val h = minOf(size.height - pad * 2, (size.width - pad * 2) / ratio)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(bar),
            contentDescription = "모은 금 골드바",
            contentScale = ContentScale.Fit,
            modifier = GlanceModifier.size(width = h * ratio, height = h),
        )
    }
}

/** 4×1: 세로 골드바 윗부분이 위젯 아래로 잘려 들어간 모양 + 오늘의 행운 문구 */
class WideGoldWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = HandonStore(context)
        val don = store.totalDon()
        val fortune = Fortunes.forDate(store.today())
        val bar = renderGoldBarBitmap(context, don, landscape = false, longSidePx = 560)
        val top = firstOpaqueRow(bar)
        provideContent { WideContent(bar, top, fortune) }
    }
}

/** 그림 위쪽 투명 여백이 끝나는 줄 */
private fun firstOpaqueRow(b: Bitmap): Int {
    val row = IntArray(b.width)
    for (y in 0 until b.height) {
        b.getPixels(row, 0, b.width, 0, y, b.width, 1)
        if (row.any { (it ushr 24) > 16 }) return y
    }
    return 0
}

@Composable
private fun WideContent(bar: Bitmap, opaqueTop: Int, fortune: String) {
    val size = LocalSize.current
    val topGap = 10.dp
    val barW = size.height * 1.05f // 골드바 폭: 위젯 높이 정도로 크게
    val visibleH = size.height - topGap
    // 보이는 부분만 잘라 낸다: 골드바 꼭대기 바로 위부터, 위젯 바닥에서 끊기게
    val pxPerDp = bar.width / barW.value
    val y0 = (opaqueTop - 2).coerceAtLeast(0)
    val cropH = (visibleH.value * pxPerDp).toInt().coerceAtMost(bar.height - y0)
    val visible = Bitmap.createBitmap(bar, 0, y0, bar.width, cropH)
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg))
            .clickable(actionStartActivity<MainActivity>())
            .padding(start = 18.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier.width(barW).fillMaxHeight(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Image(
                provider = ImageProvider(visible),
                contentDescription = "모은 금 골드바",
                contentScale = ContentScale.FillBounds,
                modifier = GlanceModifier.size(width = barW, height = visibleH),
            )
        }
        Spacer(GlanceModifier.width(14.dp))
        Text(
            fortune,
            maxLines = 3,
            style = TextStyle(color = Fortune, fontSize = 13.sp),
        )
    }
}

class SmallGoldWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallGoldWidget()
}

class WideGoldWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WideGoldWidget()
}
