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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
 * 금고 앞에 붙는 놋쇠 라벨 줄. 다 모은 카테고리는 라벨이 붙고, 아직이면 빈 자리만 보인다.
 * [fresh] 는 방금 붙은 라벨 — 툭 하고 붙는 움직임을 준다.
 */
@Composable
fun VaultLabels(done: Set<Category>, fresh: Category?, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Category.entries.forEach { c ->
            if (c in done) LabelPlate(c.label, isFresh = c == fresh) else EmptySlot()
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
            .size(width = 44.dp, height = 18.dp)
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
        Text(text, color = BrassText, fontSize = 10.5.sp, fontFamily = SerifFamily, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun EmptySlot() {
    Box(
        Modifier
            .size(width = 44.dp, height = 18.dp)
            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(4.dp))
    )
}

/** 모은 문구를 카테고리별로 보는 도감. 못 모은 문구는 ??? */
@Composable
fun CollectionScreen(visible: Boolean, counts: Map<Int, Int>, done: Set<Category>, onClose: () -> Unit) {
    if (visible) BackHandler(onBack = onClose)
    AnimatedVisibility(
        visible,
        enter = slideInVertically { it / 6 } + fadeIn(),
        exit = slideOutVertically { it / 6 } + fadeOut(),
    ) {
        val total = Fortunes.ALL.size
        val have = counts.keys.count { Fortunes.of(it) != null }
        Column(
            Modifier
                .fillMaxSize()
                .background(Palette.Ink)
                .clickable(remember { MutableInteractionSource() }, indication = null) {}
                .safeDrawingPadding()
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 22.dp, end = 10.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("문구 도감", color = Palette.GoldHi, fontSize = 21.sp, fontFamily = SerifFamily, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(2.dp))
                    Text("하루 한 번, 금고를 열 때 문구 하나를 받아요", color = Palette.Muted, fontSize = 11.5.sp)
                }
                Text(
                    "닫기",
                    color = Palette.Text,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable(onClick = onClose).padding(12.dp),
                )
            }

            // 요약: 모은 개수 + 금고 라벨
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .background(Palette.Panel, RoundedCornerShape(16.dp))
                    .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$have", color = Palette.GoldHi, fontSize = 28.sp, fontFamily = SerifFamily, fontWeight = FontWeight.ExtraBold)
                    Text(" / $total 모음", color = Palette.Muted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
                }
                Spacer(Modifier.height(10.dp))
                VaultLabels(done, fresh = null)
                Spacer(Modifier.height(8.dp))
                Text("한 카테고리를 다 모으면 금고에 라벨이 붙어요", color = Palette.Faint, fontSize = 11.sp)
            }

            LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
                Category.entries.forEach { c ->
                    val list = Fortunes.inCategory(c)
                    val got = list.count { it.id in counts }
                    item(key = "h-${c.name}") { CategoryHeader(c, got, list.size, c in done) }
                    items(list, key = { it.id }) { f ->
                        val n = counts[f.id] ?: 0
                        val no = list.indexOf(f) + 1
                        EntryRow(no, if (n > 0) f.text else null, n)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(c: Category, got: Int, size: Int, complete: Boolean) {
    Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.label, color = if (complete) Palette.GoldHi else Palette.Text, fontSize = 16.sp, fontFamily = SerifFamily, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(8.dp))
            if (complete) {
                Text(
                    "라벨 획득",
                    color = BrassText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(BrassPlate, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
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
    }
}

@Composable
private fun EntryRow(no: Int, text: String?, count: Int) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 9.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            "%02d".format(no),
            color = Palette.Faint,
            fontSize = 11.sp,
            modifier = Modifier.width(26.dp).padding(top = 3.dp),
        )
        if (text != null) {
            Text(
                text,
                color = Palette.Text,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontFamily = SerifFamily,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (count > 1) {
                Text("×$count", color = Palette.Gold, fontSize = 11.5.sp, modifier = Modifier.padding(start = 8.dp, top = 3.dp))
            }
        } else {
            Text("???", color = Palette.Faint, fontSize = 14.sp, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
        }
    }
}
