package com.coroding.dontjustsave.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppColors {
    val AppBackground = Color(0xFFFBFAF7)
    val CardBackground = Color(0xFFFFFFFF)
    val SoftBackground = Color(0xFFFFF8EF)
    val PrimaryText = Color(0xFF3A3D46)
    val SecondaryText = Color(0xFF7A7F8C)
    val TertiaryText = Color(0xFFA1A6B3)
    val DreamPurple = Color(0xFF9F99D1)
    val CrystalBlue = Color(0xFF86BADA)
    val FairyPink = Color(0xFFDBAAD7)
    val PeachMilk = Color(0xFFF6BEB0)
    val HoneyCream = Color(0xFFFFE3B3)
    val SoftMint = Color(0xFFBFE3D0)
    val SoftLavender = Color(0xFFE8E6F2)
    val Outline = Color(0xFFEEEAF4)
}

object AppGradients {
    val PurpleBlue = Brush.horizontalGradient(
        listOf(AppColors.DreamPurple, AppColors.CrystalBlue),
    )
    val PinkPeach = Brush.horizontalGradient(
        listOf(AppColors.FairyPink, AppColors.PeachMilk),
    )
    val CreamPurple = Brush.linearGradient(
        listOf(AppColors.HoneyCream, Color(0xFFF8D6C8), AppColors.DreamPurple),
    )
}

object CategoryColorMapper {
    fun colorFor(category: String): Color {
        return when (category) {
            "选题灵感" -> AppColors.DreamPurple
            "标题参考" -> AppColors.CrystalBlue
            "封面参考" -> AppColors.FairyPink
            "脚本结构" -> AppColors.PeachMilk
            "素材案例" -> AppColors.HoneyCream
            "表达方式" -> AppColors.SoftMint
            "待判断" -> AppColors.SoftLavender
            else -> AppColors.SoftLavender
        }
    }
}
