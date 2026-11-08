package com.ohsang.util.haruhandon

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.ohsang.util.haruhandon.widget.HandonWidgets
import com.ohsang.util.haruhandon.widget.SmallGoldWidgetReceiver
import com.ohsang.util.haruhandon.widget.WideGoldWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 디버그 빌드 전용. 홈 화면처럼 배경을 깔고 2×2, 4×1 위젯을 실제로 띄운다.
 * 먼저 `adb shell appwidget grantbind --package com.ohsang.util.haruhandon` 로 위젯 연결 권한을 줘야 한다.
 */
class WidgetPreviewActivity : Activity() {

    private lateinit var host: AppWidgetHost

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        host = AppWidgetHost(this, 4242)
        host.startListening()
        val mgr = AppWidgetManager.getInstance(this)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        fun section(colors: IntArray, labelColor: Int, topPad: Int) = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors)
            setPadding(dp(16), dp(topPad), dp(16), dp(18))
            tag = labelColor
        }

        fun label(parent: LinearLayout, text: String) = parent.addView(TextView(this).apply {
            this.text = text
            setTextColor(parent.tag as Int)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, dp(12), 0, dp(8))
        })

        fun addWidget(parent: LinearLayout, cls: Class<*>, wDp: Int, hDp: Int) {
            val id = host.allocateAppWidgetId()
            val ok = mgr.bindAppWidgetIdIfAllowed(id, ComponentName(this, cls))
            if (!ok) {
                label(parent, "위젯 연결 권한이 없어요 (appwidget grantbind 필요)")
                return
            }
            val opts = Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, wDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, wDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, hDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, hDp)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    putParcelableArrayList(
                        AppWidgetManager.OPTION_APPWIDGET_SIZES,
                        arrayListOf(SizeF(wDp.toFloat(), hDp.toFloat())),
                    )
                }
            }
            mgr.updateAppWidgetOptions(id, opts)
            val view = host.createView(this, id, mgr.getAppWidgetInfo(id))
            parent.addView(view, LinearLayout.LayoutParams(dp(wDp), dp(hDp)))
        }

        val dark = section(
            intArrayOf(Color.rgb(0x3B, 0x4A, 0x63), Color.rgb(0x1F, 0x27, 0x36)),
            Color.argb(170, 255, 255, 255), 56,
        )
        val light = section(
            intArrayOf(Color.rgb(0xF4, 0xEF, 0xE6), Color.rgb(0xCF, 0xDD, 0xE8)),
            Color.argb(170, 0, 0, 0), 4,
        )
        for (sec in listOf(dark, light)) {
            label(sec, "2×2")
            addWidget(sec, SmallGoldWidgetReceiver::class.java, 170, 170)
            label(sec, "4×1")
            addWidget(sec, WideGoldWidgetReceiver::class.java, 350, 84)
            root.addView(sec, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        setContentView(root)

        CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            HandonWidgets.updateAll(this@WidgetPreviewActivity)
        }
    }

    override fun onDestroy() {
        host.stopListening()
        super.onDestroy()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
