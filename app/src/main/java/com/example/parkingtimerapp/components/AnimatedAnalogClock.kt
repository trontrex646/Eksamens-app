package com.example.parkingtimerapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedAnalogClock() {
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }

    // Animācijas efekts katrai rādītāju kustībai
    val transition = rememberInfiniteTransition()
    val animatedSeconds by transition.animateFloat(
        initialValue = calendar.get(Calendar.SECOND) * 6f,
        targetValue = (calendar.get(Calendar.SECOND) + 1) * 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    LaunchedEffect(Unit) {
        while (true) {
            calendar = Calendar.getInstance()
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .size(250.dp)
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2

            // Zīmē pulksteņa rāmi
            drawCircle(
                color = Color.Black,
                radius = radius,
                style = Stroke(5.dp.toPx())
            )

            // Minūšu rādītājs
            val minutes = calendar.get(Calendar.MINUTE)
            val minAngle = (minutes * 6).toFloat()
            val minHandLength = radius * 0.75f
            drawLine(
                color = Color.Blue,
                start = center,
                end = Offset(
                    center.x + minHandLength * cos(Math.toRadians(minAngle - 90.0)).toFloat(),
                    center.y + minHandLength * sin(Math.toRadians(minAngle - 90.0)).toFloat()
                ),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Stundu rādītājs
            val hours = calendar.get(Calendar.HOUR)
            val hourAngle = ((hours % 12) * 30 + (minutes / 2)).toFloat()
            val hourHandLength = radius * 0.55f
            drawLine(
                color = Color.Red,
                start = center,
                end = Offset(
                    center.x + hourHandLength * cos(Math.toRadians(hourAngle - 90.0)).toFloat(),
                    center.y + hourHandLength * sin(Math.toRadians(hourAngle - 90.0)).toFloat()
                ),
                strokeWidth = 8.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Sekunžu rādītājs
            val secHandLength = radius * 0.85f
            drawLine(
                color = Color.Green,
                start = center,
                end = Offset(
                    center.x + secHandLength * cos(Math.toRadians(animatedSeconds - 90.0)).toFloat(),
                    center.y + secHandLength * sin(Math.toRadians(animatedSeconds - 90.0)).toFloat()
                ),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
