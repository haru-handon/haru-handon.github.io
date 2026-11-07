package com.ohsang.play.haruhandon.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ohsang.play.haruhandon.MainActivity
import com.ohsang.play.haruhandon.R
import com.ohsang.play.haruhandon.data.HandonStore
import com.ohsang.play.haruhandon.widget.HandonWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * 매일 아침 8시 "오늘의 금 1돈 도착" 알림.
 * 정확한 알람 권한 없이 쓰는 방식이라 몇 분 늦게 올 수 있다.
 * 알림이 울릴 때마다 다음 날 것을 다시 예약하고, 위젯도 새 날짜로 갱신한다.
 */
object DailyReminder {
    const val CHANNEL_ID = "daily_gold"
    private const val NOTIFICATION_ID = 8
    private const val HOUR = 8

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.channel_desc) }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun schedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val now = ZonedDateTime.now()
        var next = now.toLocalDate().atTime(HOUR, 0).atZone(now.zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), alarmIntent(context))
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 1,
        Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun show(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_goldbar)
            .setColor(0xFFDCAE42.toInt())
            .setContentTitle("오늘의 금 1돈이 도착했어요")
            .setContentText("금고를 열어 오늘의 금과 덕담을 받아가세요.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // 권한이 그사이 꺼졌으면 조용히 넘어간다
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = HandonStore(context)
        if (!store.attendedToday()) DailyReminder.show(context)
        DailyReminder.schedule(context)
        refreshWidgets(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        DailyReminder.schedule(context)
        refreshWidgets(context)
    }
}

private fun BroadcastReceiver.refreshWidgets(context: Context) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.Default).launch {
        try {
            HandonWidgets.updateAll(context)
        } finally {
            pending.finish()
        }
    }
}
