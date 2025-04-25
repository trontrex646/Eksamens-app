package com.example.parkingtimerapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
fun InteractiveAnalogClock(
    initialHours: Int,
    initialMinutes: Int,
    onTimeChanged: (Int, Int) -> Unit
) {
    var hours by remember { mutableStateOf(initialHours % 12) }
    var minutes by remember { mutableStateOf(initialMinutes % 60) }

    var selectedHand by remember { mutableStateOf<ClockHand?>(null) }

    val hourAngle by animateFloatAsState(
        targetValue = (hours % 12) * 30f + (minutes / 2f),
        animationSpec = tween(300)
    )
    val minuteAngle by animateFloatAsState(
        targetValue = minutes * 6f,
        animationSpec = tween(300)
    )

    Box(
        modifier = Modifier
            .size(250.dp)
            .padding(16.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val touchX = change.position.x - center.x
                        val touchY = change.position.y - center.y
                        val touchRadius = sqrt(touchX.pow(2) + touchY.pow(2))

                        val touchAngle = atan2(touchY, touchX).toDegrees()
                        val adjustedAngle = (touchAngle + 360) % 360

                        val radius = size.width.coerceAtMost(size.height) / 2
                        val isMinuteHand = touchRadius > radius * 0.55

                        if (selectedHand == null) {
                            selectedHand = if (isMinuteHand) ClockHand.MINUTE else ClockHand.HOUR
                        }

                        if (selectedHand == ClockHand.MINUTE) {
                            minutes = ((adjustedAngle / 6).roundToInt()) % 60
                        } else {
                            hours = ((adjustedAngle / 30).roundToInt()) % 12
                        }

                        onTimeChanged(hours, minutes)
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width.coerceAtMost(size.height) / 2f

            // Pulksteņa rāmis
            drawCircle(
                color = Color.Gray,
                radius = radius,
                style = Stroke(6.dp.toPx())
            )

            // Minūšu rādītājs (zils)
            val minHandLength = radius * 0.8f
            rotate(minuteAngle - 90, center) {
                drawLine(
                    color = Color.Blue,
                    start = center,
                    end = Offset(center.x, center.y - minHandLength),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Stundu rādītājs (sarkans)
            val hourHandLength = radius * 0.6f
            rotate(hourAngle - 90, center) {
                drawLine(
                    color = Color.Red,
                    start = center,
                    end = Offset(center.x, center.y - hourHandLength),
                    strokeWidth = 8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Pulksteņa centrs
            drawCircle(color = Color.Black, radius = 10.dp.toPx())
        }
    }
}

// Enum klase, lai pārvaldītu, kuru rādītāju lietotājs vilk
enum class ClockHand { HOUR, MINUTE }

// Papildfunkcija radiānu pārvēršanai grādos
private fun Float.toDegrees() = Math.toDegrees(this.toDouble()).toFloat()
