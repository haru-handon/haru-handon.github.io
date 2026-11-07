package com.ohsang.play.haruhandon.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
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
import com.ohsang.play.haruhandon.MainActivity
import com.ohsang.play.haruhandon.R
import com.ohsang.play.haruhandon.data.Fortunes
import com.ohsang.play.haruhandon.data.HandonStore
import com.ohsang.play.haruhandon.data.donLabel
import com.ohsang.play.haruhandon.data.gramLabel

object HandonWidgets {
    suspend fun updateAll(context: Context) {
        SmallGoldWidget().updateAll(context)
        WideGoldWidget().updateAll(context)
    }
}

private val GoldHi = ColorProvider(Color(0xFFFBE7A6))
private val Muted = ColorProvider(Color(0xFF9C9585))

/** 2×2: 골드바를 크게, 아래에 무게만 */
class SmallGoldWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val don = HandonStore(context).totalDon()
        provideContent { SmallContent(don) }
    }
}

@Composable
private fun SmallContent(don: Int) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg))
            .clickable(actionStartActivity<MainActivity>())
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            provider = ImageProvider(R.drawable.goldbar_mini),
            contentDescription = "골드바",
            contentScale = ContentScale.Fit,
            modifier = GlanceModifier.size(width = 62.dp, height = 93.dp),
        )
        Spacer(GlanceModifier.height(6.dp))
        Text(
            donLabel(don),
            style = TextStyle(color = GoldHi, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
        )
    }
}

/** 4×2: 골드바를 크게, 오른쪽에 무게·그램·덕담 */
class WideGoldWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = HandonStore(context)
        val don = store.totalDon()
        val fortune = Fortunes.forDate(store.today())
        provideContent { WideContent(don, fortune) }
    }
}

@Composable
private fun WideContent(don: Int, fortune: String) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg))
            .clickable(actionStartActivity<MainActivity>())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(R.drawable.goldbar_mini),
            contentDescription = "골드바",
            contentScale = ContentScale.Fit,
            modifier = GlanceModifier.size(width = 70.dp, height = 105.dp),
        )
        Spacer(GlanceModifier.width(16.dp))
        Column {
            Text(
                donLabel(don),
                style = TextStyle(color = GoldHi, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
            )
            Text(gramLabel(don), style = TextStyle(color = Muted, fontSize = 12.sp))
            Spacer(GlanceModifier.height(8.dp))
            Text(fortune, maxLines = 2, style = TextStyle(color = Muted, fontSize = 11.sp))
        }
    }
}

class SmallGoldWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallGoldWidget()
}

class WideGoldWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WideGoldWidget()
}
