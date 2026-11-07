package com.ohsang.play.haruhandon.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** 어두운 강철 금고 + 금빛. 앱은 늘 이 한 가지 색으로 보인다. */
object Palette {
    val Ink = Color(0xFF101113)
    val InkLift = Color(0xFF23262B)
    val Panel = Color(0xFF1B1E22)
    val Line = Color(0xFF2C3036)
    val Text = Color(0xFFEDE7D7)
    val Muted = Color(0xFF9C9585)
    val Faint = Color(0xFF6D685D)

    val GoldHi = Color(0xFFFBE7A6)
    val Gold = Color(0xFFDCAE42)
    val GoldLo = Color(0xFF8A6316)

    val Engrave = Color(0xFFA87818)
    val EngraveShadow = Color(0xFF6B4708)
    val EngraveLight = Color(0xFFFFF8DC)

    /** 금고 단계별 테두리·문 색: 철제, 황동, 금빛, 황금 */
    val Rim = listOf(Color(0xFF5B626B), Color(0xFFA9823D), Color(0xFFDCAE42), Color(0xFFFBE7A6))
    val DoorHi = listOf(Color(0xFF4B5159), Color(0xFF4F4A40), Color(0xFF5A4C2C), Color(0xFF7A6331))
    val DoorLo = listOf(Color(0xFF24282D), Color(0xFF26231D), Color(0xFF2A2213), Color(0xFF3A2C10))
}

/**
 * 제목·각인용 명조. 지금은 시스템 serif(대부분의 기기에서 Noto Serif CJK)를 쓴다.
 * 나눔명조를 넣으려면 res/font/nanum_myeongjo_extrabold.ttf 를 추가하고
 * FontFamily(Font(R.font.nanum_myeongjo_extrabold, FontWeight.ExtraBold)) 로 바꾸면 된다.
 */
val SerifFamily: FontFamily = FontFamily.Serif
val SansFamily: FontFamily = FontFamily.SansSerif
val MonoFamily: FontFamily = FontFamily.Monospace
