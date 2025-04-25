package com.example.parkingtimerapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnalogClock(setHours: Int, setMinutes: Int) {
    var hours by remember { mutableStateOf(setHours) }
    var minutes by remember { mutableStateOf(setMinutes) }

    val animatedMinutes = animateFloatAsState(
        targetValue = (minutes * 6).toFloat(),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
    )

    val animatedHours = animateFloatAsState(
        targetValue = ((hours % 12) * 30 + (minutes / 2)).toFloat(),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
    )

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2

                // Zīmē pulksteņa rāmi
                drawCircle(
                    color = Color.Black,
                    radius = radius,
                    style = Stroke(4.dp.toPx())
                )

                // Minūšu rādītājs
                val minHandLength = radius * 0.8f
                drawLine(
                    color = Color.Blue,
                    start = center,
                    end = Offset(
                        center.x + minHandLength * cos(Math.toRadians(animatedMinutes.value - 90.0)).toFloat(),
                        center.y + minHandLength * sin(Math.toRadians(animatedMinutes.value - 90.0)).toFloat()
                    ),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Stundu rādītājs
                val hourHandLength = radius * 0.6f
                drawLine(
                    color = Color.Red,
                    start = center,
                    end = Offset(
                        center.x + hourHandLength * cos(Math.toRadians(animatedHours.value - 90.0)).toFloat(),
                        center.y + hourHandLength * sin(Math.toRadians(animatedHours.value - 90.0)).toFloat()
                    ),
                    strokeWidth = 8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Digitālais laika displejs zem pulksteņa
        BasicText(
            text = String.format("%02d:%02d", hours, minutes),
            modifier = Modifier.padding(top = 8.dp),
            style = androidx.compose.ui.text.TextStyle(
                color = Color.Black,
                fontSize = androidx.compose.ui.unit.TextUnit.Unspecified
            )
        )
    }
}
