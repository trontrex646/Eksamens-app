package com.example.parkingtimerapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = {
                val newValue = if (value >= range.last) range.first else value + 1
                onValueChange(newValue)
            }
        ) {
            Text("▲")
        }

        Text(
            text = String.format("%02d", value),
            style = MaterialTheme.typography.headlineMedium
        )

        IconButton(
            onClick = {
                val newValue = if (value <= range.first) range.last else value - 1
                onValueChange(newValue)
            }
        ) {
            Text("▼")
        }
    }
} 