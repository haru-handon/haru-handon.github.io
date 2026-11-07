package com.ohsang.util.haruhandon

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.ohsang.util.haruhandon.data.HandonStore
import com.ohsang.util.haruhandon.notify.DailyReminder
import com.ohsang.util.haruhandon.sound.Sfx
import com.ohsang.util.haruhandon.ui.HomeScreen

class MainActivity : ComponentActivity() {

    private lateinit var store: HandonStore
    private lateinit var sfx: Sfx
    private var resumeTick by mutableIntStateOf(0)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) DailyReminder.schedule(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        store = HandonStore(this)
        sfx = Sfx(this).apply { enabled = store.soundOn }

        DailyReminder.ensureChannel(this)
        DailyReminder.schedule(this)

        setContent {
            HomeScreen(
                store = store,
                sfx = sfx,
                resumeTick = resumeTick,
                onRequestNotification = ::requestNotificationPermission,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }

    override fun onDestroy() {
        sfx.release()
        super.onDestroy()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
