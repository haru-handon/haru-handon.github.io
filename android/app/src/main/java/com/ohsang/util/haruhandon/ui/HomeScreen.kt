package com.ohsang.util.haruhandon.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ohsang.util.haruhandon.data.Fortunes
import com.ohsang.util.haruhandon.data.HandonStore
import com.ohsang.util.haruhandon.data.donLabel
import com.ohsang.util.haruhandon.data.gramLabel
import com.ohsang.util.haruhandon.data.isNyangMilestone
import com.ohsang.util.haruhandon.data.nextTier
import com.ohsang.util.haruhandon.data.tierIndex
import com.ohsang.util.haruhandon.data.tierOf
import com.ohsang.util.haruhandon.sound.Haptics
import com.ohsang.util.haruhandon.sound.Sfx
import com.ohsang.util.haruhandon.sound.Sound
import com.ohsang.util.haruhandon.widget.HandonWidgets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlin.math.roundToInt

private enum class Phase { Closed, Opening, Open, Closing }

@Composable
fun HomeScreen(
    store: HandonStore,
    sfx: Sfx,
    resumeTick: Int,
    onRequestNotification: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val haptics = remember(view) { Haptics(view) }
    val scope = rememberCoroutineScope()

    var phase by remember { mutableStateOf(Phase.Closed) }
    var today by remember { mutableStateOf(store.today()) }
    var shownDon by remember { mutableIntStateOf(store.totalDon()) }
    var streak by remember { mutableIntStateOf(store.streak()) }
    var fortuneVisible by remember { mutableStateOf(false) }
    var celebration by remember { mutableStateOf<CelebrationInfo?>(null) }
    var toast by remember { mutableStateOf<ToastMsg?>(null) }
    var soundOn by remember { mutableStateOf(store.soundOn) }

    val dial = remember { Animatable(0f) }
    val door = remember { Animatable(0f) }
    val lever = remember { Animatable(0f) }
    val lift = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    val plus = remember { Animatable(-1f) }
    val motion = remember { BarMotion() }
    val sparks = remember { SparkleField() }
    var stageRect by remember { mutableStateOf(Rect.Zero) }
    var barRect by remember { mutableStateOf(Rect.Zero) }

    fun reload() {
        today = store.today()
        shownDon = store.totalDon()
        streak = store.streak()
    }

    fun refreshWidgets() {
        scope.launch { HandonWidgets.updateAll(context) }
    }

    fun burstAt(stagePoint: Offset, count: Int) = with(density) {
        sparks.burst(stagePoint, count, 30.dp.toPx(), 80.dp.toPx(), 8.dp.toPx(), 20.dp.toPx())
    }

    // 앱으로 돌아왔을 때 날짜가 바뀌었으면 반영 (금고가 닫혀 있을 때만)
    LaunchedEffect(resumeTick) {
        if (phase == Phase.Closed && today != store.today()) {
            reload()
            fortuneVisible = false
        }
    }

    fun openVault() {
        if (phase != Phase.Closed) return
        phase = Phase.Opening
        scope.launch {
            val ticking = launch {
                repeat(19) { i ->
                    sfx.play(Sound.TICK, 0.55f, jitter = true)
                    if (i % 3 == 0) haptics.tick()
                    delay(70)
                }
            }
            launch { lever.animateTo(1f, tween(350)) }
            dial.animateTo(330f, tween(510, easing = FastOutSlowInEasing))
            dial.animateTo(150f, tween(410, easing = FastOutSlowInEasing))
            dial.animateTo(420f, tween(430, easing = FastOutSlowInEasing))
            ticking.cancel()

            sfx.play(Sound.CLUNK)
            haptics.thud()
            launch { door.animateTo(DOOR_OPEN_DEG, tween(950, easing = CubicBezierEasing(0.35f, 0.75f, 0.2f, 1f))) }
            delay(650)
            launch { lift.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 260f)) }
            delay(500)

            val before = store.totalDon()
            val isNew = store.attendToday()
            val after = store.totalDon()
            streak = store.streak()
            fortuneVisible = true
            phase = Phase.Open

            if (isNew) {
                sfx.play(Sound.CHIME)
                haptics.confirm()
                launch { flash.snapTo(0f); flash.animateTo(1f, tween(150)); flash.animateTo(0f, tween(850)) }
                launch { plus.snapTo(0f); plus.animateTo(1f, tween(1600, easing = LinearOutSlowInEasing)); plus.snapTo(-1f) }
                launch { motion.sweep() }
                burstAt(Offset(stageRect.width / 2, stageRect.height / 2), 12)
                launch {
                    val count = Animatable(before.toFloat())
                    count.animateTo(after.toFloat(), tween(700)) { shownDon = value.roundToInt() }
                    shownDon = after
                }
                refreshWidgets()

                val upgraded = tierOf(after).takeIf { it != tierOf(before) }
                if (isNyangMilestone(after)) {
                    delay(1100)
                    sfx.play(Sound.FANFARE)
                    haptics.confirm()
                    celebration = CelebrationInfo(after, upgraded?.name)
                } else {
                    toast = ToastMsg(upgraded?.let { "${it.name}로 업그레이드됐어요!" } ?: "금 1돈 입고! 총 ${donLabel(after)}")
                    if (!store.notificationAsked) {
                        store.notificationAsked = true
                        delay(1600)
                        onRequestNotification()
                    }
                }
            } else {
                sfx.play(Sound.TING)
                launch { motion.sweep() }
                toast = ToastMsg("오늘의 금 1돈은 이미 받았어요. 내일 또 만나요.")
            }
        }
    }

    fun closeVault() {
        if (phase != Phase.Open) return
        phase = Phase.Closing
        scope.launch {
            launch {
                motion.angle.animateTo(0f, tween(380))
                motion.base = 0f
            }
            lift.animateTo(0f, tween(380, easing = FastOutSlowInEasing))
            motion.reset()
            door.animateTo(0f, tween(850, easing = FastOutSlowInEasing))
            sfx.play(Sound.CLUNK)
            haptics.thud()
            lever.snapTo(0f)
            dial.snapTo(0f)
            phase = Phase.Closed
        }
    }

    fun tapBar(fx: Float, fy: Float) {
        if (phase != Phase.Open) return
        val p = Offset(
            barRect.left - stageRect.left + fx * barRect.width,
            barRect.top - stageRect.top + fy * barRect.height,
        )
        burstAt(p, 9)
        sfx.play(Sound.TING)
        haptics.tick()
        scope.launch { motion.sweep() }
    }

    fun flipped() {
        sfx.play(Sound.FLIP)
        haptics.tick()
        scope.launch {
            delay(380)
            motion.sweep()
        }
    }


    val tier = tierIndex(shownDon)

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(0f to Palette.InkLift, 0.7f to Palette.Ink, 1f to Palette.Ink))
    ) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Header(shownDon, streak, soundOn) {
                soundOn = !soundOn
                store.soundOn = soundOn
                sfx.enabled = soundOn
            }
            TierLine(shownDon)

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { stageRect = it.boundsInRoot() },
                contentAlignment = Alignment.Center,
            ) {
                VaultStage(
                    tier = tier,
                    doorAngle = door.value,
                    dialAngle = dial.value,
                    leverTurn = lever.value,
                    lift = lift.value,
                    showHint = phase == Phase.Closed,
                    onDoorTap = ::openVault,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    InteractiveGoldBar(
                        don = shownDon,
                        motion = motion,
                        enabled = phase == Phase.Open,
                        dim = shownDon == 0,
                        onTap = ::tapBar,
                        onFlip = { flipped() },
                        modifier = Modifier.onGloballyPositioned { barRect = it.boundsInRoot() },
                    )
                }
                GoldFlash(flash.value, Modifier.fillMaxSize())
                sparks.Layer(Modifier.fillMaxSize())
                PlusOne(plus.value, Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
                GoldToast(toast, Modifier.align(Alignment.TopCenter).padding(top = 4.dp))
            }

            FortuneCard(today, fortuneVisible)
            Foot(open = phase == Phase.Open, onClose = ::closeVault)

        }

        celebration?.let { info ->
            NyangCelebration(
                info = info,
                onDismiss = {
                    celebration = null
                    scope.launch { motion.sweep() }
                    if (!store.notificationAsked) {
                        store.notificationAsked = true
                        onRequestNotification()
                    }
                },
            )
        }
    }
}

/* ---------- 화면 조각 ---------- */

@Composable
private fun Header(don: Int, streak: Int, soundOn: Boolean, onToggleSound: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 16.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                "하루 한돈",
                color = Palette.GoldHi,
                fontSize = 21.sp,
                fontFamily = SerifFamily,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("연속 출석 ${streak}일", color = Palette.Muted, fontSize = 11.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    if (soundOn) "효과음 켬" else "효과음 끔",
                    color = if (soundOn) Palette.Gold else Palette.Faint,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clickable(onClick = onToggleSound)
                        .padding(vertical = 4.dp),
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                donLabel(don),
                color = Palette.GoldHi,
                fontSize = 24.sp,
                fontFamily = SerifFamily,
                fontWeight = FontWeight.ExtraBold,
            )
            Text("${gramLabel(don)} · ${don}돈", color = Palette.Muted, fontSize = 11.5.sp)
        }
    }
}

@Composable
private fun TierLine(don: Int) {
    val tier = tierOf(don)
    val next = nextTier(don)
    val progress = if (next == null) 1f else (don - tier.min).toFloat() / (next.min - tier.min)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(tier.name, color = Palette.Gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .weight(1f)
                .height(4.dp)
                .background(Palette.Line, RoundedCornerShape(2.dp))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .background(
                        Brush.horizontalGradient(listOf(Palette.GoldLo, Palette.GoldHi)),
                        RoundedCornerShape(2.dp),
                    )
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            next?.let { "${it.short}까지 ${it.min - don}돈" } ?: "최고 단계",
            color = Palette.Muted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun FortuneCard(today: LocalDate, visible: Boolean) {
    val dow = today.dayOfWeek.getDisplayName(DateTextStyle.SHORT, Locale.KOREAN)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .background(Color.White.copy(alpha = 0.035f), RoundedCornerShape(14.dp))
            .border(1.dp, Palette.Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .heightIn(min = 68.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("오늘의 재물운", color = Palette.Gold, fontSize = 11.sp, letterSpacing = 0.9.sp, fontWeight = FontWeight.SemiBold)
            Text("${today.monthValue}.${today.dayOfMonth} $dow", color = Palette.Faint, fontSize = 11.sp)
        }
        Spacer(Modifier.height(6.dp))
        if (visible) {
            Text(
                Fortunes.forDate(today),
                color = Palette.Text,
                fontSize = 15.5.sp,
                lineHeight = 23.sp,
                fontFamily = SerifFamily,
                fontWeight = FontWeight.Bold,
            )
        } else {
            Text("금고를 열면 오늘의 덕담이 나와요.", color = Palette.Faint, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Foot(open: Boolean, onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(92.dp)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (open) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("금괴를 잡고 옆으로 돌리면 뒷면이 보여요", color = Palette.Faint, fontSize = 11.5.sp)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .background(Palette.Panel, RoundedCornerShape(22.dp))
                        .border(1.dp, Palette.Line, RoundedCornerShape(22.dp))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text("금고 보관", color = Palette.Text, fontSize = 13.sp)
                }
            }
        } else {
            Text(
                "하루 한 번 금고를 열면 금 1돈이 더해져요",
                color = Palette.Faint,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
