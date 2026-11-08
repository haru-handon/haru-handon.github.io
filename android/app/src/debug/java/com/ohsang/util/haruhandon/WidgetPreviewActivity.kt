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

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(0x3B, 0x4A, 0x63), Color.rgb(0x1F, 0x27, 0x36), Color.rgb(0x14, 0x18, 0x22)),
            )
            setPadding(dp(16), dp(120), dp(16), dp(16))
        }

        fun label(text: String) = TextView(this).apply {
            this.text = text
            setTextColor(Color.argb(170, 255, 255, 255))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(28), 0, dp(10))
        }

        fun addWidget(cls: Class<*>, wDp: Int, hDp: Int) {
            val id = host.allocateAppWidgetId()
            val ok = mgr.bindAppWidgetIdIfAllowed(id, ComponentName(this, cls))
            if (!ok) {
                root.addView(label("위젯 연결 권한이 없어요 (appwidget grantbind 필요)"))
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
            root.addView(view, LinearLayout.LayoutParams(dp(wDp), dp(hDp)))
        }

        root.addView(label("2×2"))
        addWidget(SmallGoldWidgetReceiver::class.java, 170, 170)
        root.addView(label("4×1"))
        addWidget(WideGoldWidgetReceiver::class.java, 350, 84)
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
