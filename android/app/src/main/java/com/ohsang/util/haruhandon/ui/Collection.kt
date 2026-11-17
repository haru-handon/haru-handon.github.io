package com.ohsang.util.haruhandon.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ohsang.util.haruhandon.data.Category
import com.ohsang.util.haruhandon.data.Fortunes

private val BrassPlate = Brush.verticalGradient(
    0f to Color(0xFFF7DE8C), 0.45f to Color(0xFFD6A647), 1f to Color(0xFF9C7123),
)
private val BrassText = Color(0xFF4A3208)

/**
 * 금고 문 왼쪽 위에 붙는 놋쇠 라벨. 다 모은 카테고리만 순서대로 붙는다.
 * [fresh] 는 방금 붙은 라벨 — 문이 닫히며 보일 때 툭 하고 붙는다.
 */
@Composable
fun VaultLabels(done: Set<Category>, fresh: Category?, modifier: Modifier = Modifier, vertical: Boolean = true) {
    val earned = Category.entries.filter { it in done }
    if (vertical) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            earned.forEach { LabelPlate(it.label, isFresh = it == fresh) }
        }
    } else {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            earned.forEach { LabelPlate(it.label, isFresh = it == fresh) }
        }
    }
}

@Composable
private fun LabelPlate(text: String, isFresh: Boolean) {
    val pop = remember(isFresh) { Animatable(if (isFresh) 0f else 1f) }
    LaunchedEffect(isFresh) {
        if (isFresh) pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f))
    }
    Box(
        Modifier
            .graphicsLayer {
                val s = 1.8f - 0.8f * pop.value
                scaleX = s
                scaleY = s
                alpha = pop.value.coerceIn(0f, 1f)
            }
            .size(width = 40.dp, height = 16.dp)
            .background(BrassPlate, RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF6E4E12), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        // 작은 나사 두 개
        Row(Modifier.fillMaxSize().padding(horizontal = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(3.dp).background(Color(0xFF7A5718), CircleShape))
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(3.dp).background(Color(0xFF7A5718), CircleShape))
        }
        Text(text, color = BrassText, fontSize = 9.5.sp, fontFamily = SerifFamily, fontWeight = FontWeight.ExtraBold)
    }
}


@Composable
private fun NavItem(c: Category, got: Int, size: Int, complete: Boolean, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) Palette.Panel else Color.Transparent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(56.dp)
                .background(if (selected) Palette.Gold else Color.Transparent)
        )
        Column(Modifier.padding(start = 13.dp, top = 10.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    c.label,
                    color = if (selected) Palette.GoldHi else Palette.Text,
                    fontSize = 15.sp,
                    fontFamily = SerifFamily,
                    fontWeight = FontWeight.ExtraBold,
                )
                if (complete) {
                    Spacer(Modifier.width(5.dp))
                    Box(Modifier.size(6.dp).background(Palette.Gold, CircleShape))
                }
            }
            Text("$got / $size", color = if (selected) Palette.Gold else Palette.Faint, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CategoryHead(c: Category, got: Int, size: Int, complete: Boolean) {
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 18.dp, top = 16.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.label, color = Palette.GoldHi, fontSize = 18.sp, fontFamily = SerifFamily, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))
            Text("$got / $size", color = Palette.Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(3.dp).background(Palette.Line, RoundedCornerShape(2.dp))) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(got.toFloat() / size)
                    .background(Brush.horizontalGradient(listOf(Palette.GoldLo, Palette.GoldHi)), RoundedCornerShape(2.dp))
            )
        }
        Spacer(Modifier.height(12.dp))
        // 이 카테고리를 다 모으면 생기는 금고 장식
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    if (complete) Palette.GoldLo.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.03f),
                    RoundedCornerShape(12.dp),
                )
                .border(1.dp, if (complete) Palette.Gold else Palette.Line, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                if (complete) "금고에 「${c.label}」 라벨을 붙였어요" else "다 모으면 금고에 「${c.label}」 라벨이 붙어요",
                color = if (complete) Palette.GoldHi else Palette.Text,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun EntryRow(no: Int, text: String?) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            "%02d".format(no),
            color = Palette.Faint,
            fontSize = 11.sp,
            modifier = Modifier.width(24.dp).padding(top = 3.dp),
        )
        if (text != null) {
            Text(
                text,
                color = Palette.Text,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontFamily = SerifFamily,
                fontWeight = FontWeight.Bold,
            )
        } else {
            Text("???", color = Palette.Faint, fontSize = 14.sp, letterSpacing = 2.sp)
        }
    }
}
