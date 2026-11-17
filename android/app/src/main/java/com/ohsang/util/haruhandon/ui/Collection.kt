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

/**
 * 문구 도감. 왼쪽 카테고리 목록은 고정, 오른쪽 문구 목록만 스크롤된다.
 * 못 모은 문구는 ??? 로만 보인다.
 */
@Composable
fun CollectionScreen(visible: Boolean, counts: Map<Int, Int>, done: Set<Category>, onClose: () -> Unit) {
    if (visible) BackHandler(onBack = onClose)
    var selected by remember { mutableStateOf(Category.MONEY) }
    AnimatedVisibility(
        visible,
        enter = slideInVertically { it / 6 } + fadeIn(),
        exit = slideOutVertically { it / 6 } + fadeOut(),
    ) {
        val have = counts.keys.count { Fortunes.of(it) != null }
        Column(
            Modifier
                .fillMaxSize()
                .background(Palette.Ink)
                .clickable(remember { MutableInteractionSource() }, indication = null) {}
                .safeDrawingPadding()
        ) {
            // 제목줄
            Row(
                Modifier.fillMaxWidth().padding(start = 22.dp, end = 10.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("문구 도감", color = Palette.GoldHi, fontSize = 21.sp, fontFamily = SerifFamily, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$have", color = Palette.GoldHi, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(" / ${Fortunes.ALL.size} 모음", color = Palette.Muted, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    VaultLabels(done, fresh = null)
                }
                Text(
                    "닫기",
                    color = Palette.Text,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable(onClick = onClose).padding(12.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Line))

            Row(Modifier.fillMaxSize()) {
                // 왼쪽: 카테고리 (고정)
                Column(
                    Modifier
                        .width(96.dp)
                        .fillMaxHeight()
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(vertical = 8.dp)
                ) {
                    Category.entries.forEach { c ->
                        val list = Fortunes.inCategory(c)
                        NavItem(
                            c = c,
                            got = list.count { it.id in counts },
                            size = list.size,
                            complete = c in done,
                            selected = c == selected,
                            onClick = { selected = c },
                        )
                    }
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(Palette.Line))

                // 오른쪽: 고른 카테고리의 문구만 스크롤
                val list = Fortunes.inCategory(selected)
                val state = rememberLazyListState()
                LaunchedEffect(selected) { state.scrollToItem(0) }
                LazyColumn(
                    Modifier.weight(1f).fillMaxHeight(),
                    state = state,
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    item(key = "head-${selected.name}") {
                        CategoryHead(selected, list.count { it.id in counts }, list.size, selected in done)
                    }
                    itemsIndexed(list, key = { _, f -> f.id }) { i, f ->
                        EntryRow(i + 1, if (f.id in counts) f.text else null)
                    }
                }
            }
        }
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
