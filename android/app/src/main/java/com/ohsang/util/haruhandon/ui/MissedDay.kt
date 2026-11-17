package com.ohsang.util.haruhandon.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val GoldPill = Brush.verticalGradient(listOf(Color(0xFFF6D77A), Color(0xFFC9962E)))
private val PillText = Color(0xFF2A1C04)

/** 닫힌 금고 아래: 어제를 놓쳤을 때만 보이는 작은 카드 */
@Composable
fun MissedDayCard(missedStreak: Int, onFill: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Palette.Panel, RoundedCornerShape(16.dp))
            .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
            .padding(start = 16.dp, end = 10.dp, top = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("어제 금고를 못 열었어요", color = Palette.Text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text("연속 출석 ${missedStreak}일을 이어갈 수 있어요", color = Palette.Muted, fontSize = 11.5.sp)
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .background(GoldPill, RoundedCornerShape(20.dp))
                .clickable(onClick = onFill)
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Text("어제 1돈 채우기", color = PillText, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** 채우기 전에 광고를 본다는 걸 분명히 알려 주는 아래 시트 */
@Composable
fun FillSheet(visible: Boolean, missedStreak: Int, onWatch: () -> Unit, onDismiss: () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
        )
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Palette.Panel, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .border(1.dp, Palette.Line, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(remember { MutableInteractionSource() }, indication = null) {}
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(width = 36.dp, height = 4.dp).background(Palette.Line, RoundedCornerShape(2.dp)))
                Spacer(Modifier.height(20.dp))
                Text(
                    "어제의 1돈 채우기",
                    color = Palette.GoldHi,
                    fontSize = 20.sp,
                    fontFamily = SerifFamily,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "짧은 광고를 끝까지 보면 어제도 금고를 연 것으로 기록돼요.",
                    color = Palette.Text,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.035f), RoundedCornerShape(14.dp))
                        .border(1.dp, Palette.Line, RoundedCornerShape(14.dp))
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Gain("금", "+1돈")
                    Gain("연속 출석", "${missedStreak}일 → ${missedStreak + 1}일")
                }
                Spacer(Modifier.height(20.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(GoldPill, RoundedCornerShape(26.dp))
                        .clickable(onClick = onWatch)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("광고 보고 채우기", color = PillText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "다음에 할게요",
                    color = Palette.Muted,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 10.dp, horizontal = 16.dp),
                )
                Text("바로 전날 하루만 채울 수 있어요", color = Palette.Faint, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun Gain(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Palette.Muted, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = Palette.GoldHi, fontSize = 16.sp, fontFamily = SerifFamily, fontWeight = FontWeight.Bold)
    }
}

/**
 * 미리보기용: 실제 보상형 광고(AdMob)가 들어갈 자리.
 * 광고 SDK를 넣을 때 이 화면 대신 RewardedAd.show() 를 부르고, 보상 콜백에서 onRewarded 를 부른다.
 */
@Composable
fun AdStandIn(visible: Boolean, onRewarded: () -> Unit) {
    if (!visible) return
    var left by remember { mutableIntStateOf(5) }
    LaunchedEffect(Unit) {
        while (left > 0) {
            delay(1000)
            left--
        }
        onRewarded()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF050506))
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("광고", color = Color.White.copy(alpha = 0.85f), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "실제 앱에서는 이 자리에\n15~30초짜리 보상형 광고가 나와요",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
            )
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp, end = 20.dp)
                .size(34.dp)
                .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$left", color = Color.White, fontSize = 13.sp)
        }
    }
}
