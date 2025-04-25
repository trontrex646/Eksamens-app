package com.example.parkingtimerapp.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import com.example.parkingtimerapp.R

private val RobotoMono = FontFamily.Default
private val Montserrat = FontFamily.Default
private val Quicksand = FontFamily.Default

data class ClockTheme(
    val name: String,
    val frameColor: Color,
    val backgroundColor: Color,
    val selectedItemBackgroundStart: Color,
    val selectedItemBackgroundEnd: Color,
    val unselectedItemBackground: Color,
    val selectedTextColor: Color,
    val unselectedTextColor: Color,
    val dividerColor: Color,
    val numberStyle: TextStyle,
    val cornerRadius: Int = 20,
    val elevation: Int = 4
)

val DefaultClockTheme = ClockTheme(
    name = "Default",
    frameColor = Color(0xFF091626),
    backgroundColor = Color.White,
    selectedItemBackgroundStart = Color(0xFF091626),
    selectedItemBackgroundEnd = Color(0xFF1A2B44),
    unselectedItemBackground = Color.White,
    selectedTextColor = Color.White,
    unselectedTextColor = Color(0xFF091626).copy(alpha = 0.5f),
    dividerColor = Color(0xFF091626).copy(alpha = 0.1f),
    numberStyle = TextStyle(
        color = Color(0xFF091626),
        fontSize = 24.sp,
        fontFamily = RobotoMono,
        fontWeight = FontWeight.Medium
    ),
    cornerRadius = 16,
    elevation = 8
)

val DarkClockTheme = ClockTheme(
    name = "Dark",
    frameColor = Color(0xFF2C2C2C),
    backgroundColor = Color(0xFF1B1B1B),
    selectedItemBackgroundStart = Color(0xFF3C3C3C),
    selectedItemBackgroundEnd = Color(0xFF4C4C4C),
    unselectedItemBackground = Color(0xFF1B1B1B),
    selectedTextColor = Color.White,
    unselectedTextColor = Color.White.copy(alpha = 0.5f),
    dividerColor = Color.White.copy(alpha = 0.1f),
    numberStyle = TextStyle(
        color = Color.White,
        fontSize = 24.sp,
        fontFamily = Montserrat,
        fontWeight = FontWeight.Normal
    ),
    cornerRadius = 20,
    elevation = 12
)

val ColorfulClockTheme = ClockTheme(
    name = "Colorful",
    frameColor = Color(0xFF6200EE),
    backgroundColor = Color(0xFFFDF7FF),
    selectedItemBackgroundStart = Color(0xFF6200EE),
    selectedItemBackgroundEnd = Color(0xFF9C27B0),
    unselectedItemBackground = Color(0xFFFDF7FF),
    selectedTextColor = Color.White,
    unselectedTextColor = Color(0xFF6200EE).copy(alpha = 0.5f),
    dividerColor = Color(0xFF6200EE).copy(alpha = 0.2f),
    numberStyle = TextStyle(
        color = Color(0xFF6200EE),
        fontSize = 24.sp,
        fontFamily = Quicksand,
        fontWeight = FontWeight.Bold
    ),
    cornerRadius = 24,
    elevation = 16
)

val MinimalistClockTheme = ClockTheme(
    name = "Minimalist",
    frameColor = Color(0xFFE0E0E0),
    backgroundColor = Color(0xFFFAFAFA),
    selectedItemBackgroundStart = Color(0xFFF0F0F0),
    selectedItemBackgroundEnd = Color(0xFFF8F8F8),
    unselectedItemBackground = Color(0xFFFAFAFA),
    selectedTextColor = Color.Black,
    unselectedTextColor = Color.Black.copy(alpha = 0.3f),
    dividerColor = Color.Black.copy(alpha = 0.05f),
    numberStyle = TextStyle(
        color = Color.Black,
        fontSize = 20.sp,
        fontFamily = Montserrat,
        fontWeight = FontWeight.Light
    ),
    cornerRadius = 12,
    elevation = 2
)

fun getClockTheme(themeName: String): ClockTheme {
    return when (themeName) {
        "Dark" -> DarkClockTheme
        "Colorful" -> ColorfulClockTheme
        "Minimalist" -> MinimalistClockTheme
        else -> DefaultClockTheme
    }
} 