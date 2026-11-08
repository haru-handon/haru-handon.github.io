package com.ohsang.util.haruhandon.widget

import android.content.Context
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
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ohsang.util.haruhandon.MainActivity
import com.ohsang.util.haruhandon.R
import com.ohsang.util.haruhandon.data.Fortunes
import com.ohsang.util.haruhandon.data.HandonStore
import com.ohsang.util.haruhandon.data.donLabel
import com.ohsang.util.haruhandon.data.gramLabel
import com.ohsang.util.haruhandon.ui.renderGoldBarBitmap

object HandonWidgets {
    suspend fun updateAll(context: Context) {
        SmallGoldWidget().updateAll(context)
        WideGoldWidget().updateAll(context)
    }
}

private val GoldHi = ColorProvider(Color(0xFFFBE7A6))
private val Muted = ColorProvider(Color(0xFF9C9585))

/** 그림 비율 (renderGoldBarBitmap의 여백 포함) */
private const val PORTRAIT_RATIO = (200f * 1.1f) / (300f * 1.14f)
private const val LANDSCAPE_RATIO = (300f * 1.1f) / (200f * 1.14f)

/** 2×2: 골드바 하나가 위젯을 거의 다 채운다. 무게는 골드바 각인으로 보인다. */
class SmallGoldWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val don = HandonStore(context).totalDon()
        val bar = renderGoldBarBitmap(context, don, landscape = false, longSidePx = 560)
        provideContent { SmallContent(bar) }
    }
}

@Composable
private fun SmallContent(bar: android.graphics.Bitmap) {
    val size = LocalSize.current
    val pad = 8.dp
    val maxH = size.height - pad * 2
    val maxW = size.width - pad * 2
    val h = minOf(maxH, maxW / PORTRAIT_RATIO)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg))
            .clickable(actionStartActivity<MainActivity>())
            .padding(pad),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(bar),
            contentDescription = "모은 금 골드바",
            contentScale = ContentScale.Fit,
            modifier = GlanceModifier.size(width = h * PORTRAIT_RATIO, height = h),
        )
    }
}

/** 4×1: 가로로 누운 골드바를 위젯 높이만큼 크게, 옆에 무게와 오늘의 덕담 */
class WideGoldWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = HandonStore(context)
        val don = store.totalDon()
        val fortune = Fortunes.forDate(store.today())
        val bar = renderGoldBarBitmap(context, don, landscape = true, longSidePx = 560)
        provideContent { WideContent(bar, don, fortune) }
    }
}

@Composable
private fun WideContent(bar: android.graphics.Bitmap, don: Int, fortune: String) {
    val size = LocalSize.current
    // 위젯 둥근 모서리에 골드바가 걸리지 않게 띄운다
    val padV = 9.dp
    val barH = size.height - padV * 2
    val barW = barH * LANDSCAPE_RATIO
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg))
            .clickable(actionStartActivity<MainActivity>())
            .padding(start = 14.dp, end = 14.dp, top = padV, bottom = padV),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(bar),
            contentDescription = "모은 금 골드바",
            contentScale = ContentScale.Fit,
            modifier = GlanceModifier.size(width = barW, height = barH),
        )
        Spacer(GlanceModifier.width(12.dp))
        Column {
            Text(
                donLabel(don),
                maxLines = 1,
                style = TextStyle(color = GoldHi, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                "${gramLabel(don)} · $fortune",
                maxLines = 1,
                style = TextStyle(color = Muted, fontSize = 11.sp),
            )
        }
    }
}

class SmallGoldWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallGoldWidget()
}

class WideGoldWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WideGoldWidget()
}
