package com.example.parkingtimerapp.ui.components

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parkingtimerapp.ui.theme.getClockTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key

@Composable
fun CustomTimePicker(
    initialHours: Int,
    initialMinutes: Int,
    is24HourFormat: Boolean = false,
    onTimeChanged: (hours: Int, minutes: Int) -> Unit
) {
    var selectedHours by remember { mutableStateOf(initialHours) }
    var selectedMinutes by remember { mutableStateOf(initialMinutes) }
    var isAm by remember { mutableStateOf(initialHours < 12) }

    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE) }
    
    // Create a state that will be updated when the theme changes
    var currentThemeName by remember { mutableStateOf(sharedPreferences.getString("ClockTheme", "Default") ?: "Default") }
    
    // Log initial theme
    LaunchedEffect(Unit) {
        Log.d("CustomTimePicker", "Initial theme: $currentThemeName")
    }
    
    // Observe SharedPreferences changes
    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "ClockTheme") {
                val newTheme = prefs.getString(key, "Default") ?: "Default"
                Log.d("CustomTimePicker", "Theme changed to: $newTheme (current: $currentThemeName)")
                currentThemeName = newTheme
            }
        }
        
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        
        onDispose {
            Log.d("CustomTimePicker", "Disposing theme listener")
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    // Get current theme and force recomposition when it changes
    val clockTheme by remember(currentThemeName) {
        mutableStateOf(getClockTheme(currentThemeName))
    }

    // Log when theme is applied
    SideEffect {
        Log.d("CustomTimePicker", "Applying theme in composition: $currentThemeName")
    }

    // Force recomposition of the Card when theme changes
    key(clockTheme) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(
                    elevation = clockTheme.elevation.dp,
                    shape = RoundedCornerShape(clockTheme.cornerRadius.dp)
                ),
            shape = RoundedCornerShape(clockTheme.cornerRadius.dp),
            colors = CardDefaults.cardColors(
                containerColor = clockTheme.backgroundColor
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Time pickers row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours picker
                    ScrollableNumberPicker(
                        value = if (is24HourFormat) selectedHours else (selectedHours % 12).let { if (it == 0) 12 else it },
                        onValueChange = { newHours ->
                            selectedHours = if (is24HourFormat) {
                                newHours
                            } else {
                                when {
                                    !isAm && newHours != 12 -> newHours + 12
                                    !isAm && newHours == 12 -> 12
                                    isAm && newHours == 12 -> 0
                                    else -> newHours
                                }
                            }
                            onTimeChanged(selectedHours, selectedMinutes)
                        },
                        range = if (is24HourFormat) 0..23 else 1..12,
                        modifier = Modifier.width(100.dp),
                        clockTheme = clockTheme,
                        visibleItems = if (is24HourFormat) 5 else 3
                    )

                    Text(
                        text = ":",
                        style = clockTheme.numberStyle.copy(
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Light,
                            color = clockTheme.selectedTextColor
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Minutes picker
                    ScrollableNumberPicker(
                        value = selectedMinutes,
                        onValueChange = { newMinutes ->
                            selectedMinutes = newMinutes
                            onTimeChanged(selectedHours, selectedMinutes)
                        },
                        range = 0..59,
                        modifier = Modifier.width(100.dp),
                        clockTheme = clockTheme,
                        visibleItems = if (is24HourFormat) 5 else 3
                    )
                }

                // AM/PM selector or spacer
                if (!is24HourFormat) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // AM button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { 
                                    isAm = true
                                    selectedHours = when {
                                        selectedHours >= 12 -> selectedHours - 12
                                        else -> selectedHours
                                    }
                                    onTimeChanged(selectedHours, selectedMinutes)
                                }
                                .background(
                                    brush = if (isAm) {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                clockTheme.selectedItemBackgroundStart,
                                                clockTheme.selectedItemBackgroundEnd
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                clockTheme.unselectedItemBackground,
                                                clockTheme.unselectedItemBackground
                                            )
                                        )
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "AM",
                                style = clockTheme.numberStyle.copy(
                                    fontSize = 22.sp,
                                    fontWeight = if (isAm) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isAm) clockTheme.selectedTextColor
                                           else clockTheme.unselectedTextColor
                                )
                            )
                        }

                        // PM button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { 
                                    isAm = false
                                    selectedHours = when {
                                        selectedHours < 12 -> selectedHours + 12
                                        else -> selectedHours
                                    }
                                    onTimeChanged(selectedHours, selectedMinutes)
                                }
                                .background(
                                    brush = if (!isAm) {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                clockTheme.selectedItemBackgroundStart,
                                                clockTheme.selectedItemBackgroundEnd
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                clockTheme.unselectedItemBackground,
                                                clockTheme.unselectedItemBackground
                                            )
                                        )
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PM",
                                style = clockTheme.numberStyle.copy(
                                    fontSize = 22.sp,
                                    fontWeight = if (!isAm) FontWeight.Bold else FontWeight.Normal,
                                    color = if (!isAm) clockTheme.selectedTextColor
                                           else clockTheme.unselectedTextColor
                                )
                            )
                        }
                    }
                } else {
                    // Add a spacer in 24-hour mode to maintain consistent height
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
}

@Composable
private fun ScrollableNumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    clockTheme: com.example.parkingtimerapp.ui.theme.ClockTheme,
    visibleItems: Int = 3
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val itemHeight = 70.dp

    LaunchedEffect(value) {
        val index = (value - range.first).coerceIn(0, range.last - range.first)
        listState.scrollToItem(index)
    }

    Box(
        modifier = modifier
            .height(itemHeight * 3)  // Keep consistent height
            .clip(RoundedCornerShape(24.dp))
            .background(clockTheme.unselectedItemBackground)
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = itemHeight),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(range.last - range.first + 1) { index ->
                val number = index + range.first
                val isSelected = number == value
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onValueChange(number) }
                        .background(
                            brush = if (isSelected) {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        clockTheme.selectedItemBackgroundStart,
                                        clockTheme.selectedItemBackgroundEnd
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        clockTheme.unselectedItemBackground,
                                        clockTheme.unselectedItemBackground
                                    )
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", number),
                        style = clockTheme.numberStyle.copy(
                            fontSize = 32.sp,
                            color = if (isSelected) clockTheme.selectedTextColor
                                   else clockTheme.unselectedTextColor
                        ),
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Light
                    )
                }
            }
        }

        // Top gradient shadow with rounded corners
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            clockTheme.backgroundColor,
                            clockTheme.backgroundColor.copy(alpha = 0f)
                        )
                    )
                )
                .align(Alignment.TopCenter)
        )

        // Bottom gradient shadow with rounded corners
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            clockTheme.backgroundColor.copy(alpha = 0f),
                            clockTheme.backgroundColor
                        )
                    )
                )
                .align(Alignment.BottomCenter)
        )

        // Selection indicator with rounded corners
        Box(
            modifier = Modifier
                .height(itemHeight)
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .align(Alignment.Center)
                .background(clockTheme.dividerColor.copy(alpha = 0.1f))
        )
    }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val firstVisible = listState.firstVisibleItemIndex
        val offset = listState.firstVisibleItemScrollOffset
        
        if (offset > itemHeight.value * 0.6) {
            val targetIndex = firstVisible + 1
            if (targetIndex + range.first in range) {
                onValueChange(targetIndex + range.first)
                listState.animateScrollToItem(targetIndex)
            }
        } else if (offset < itemHeight.value * 0.4) {
            if (firstVisible + range.first in range) {
                onValueChange(firstVisible + range.first)
                listState.animateScrollToItem(firstVisible)
            }
        }
    }
}

@Composable
private fun AmPmSelector(
    isAm: Boolean,
    onAmPmChanged: (Boolean) -> Unit,
    clockTheme: com.example.parkingtimerapp.ui.theme.ClockTheme
) {
    Card(
        modifier = Modifier
            .width(80.dp)
            .height(140.dp)
            .shadow(
                elevation = (clockTheme.elevation / 2).dp,
                shape = RoundedCornerShape(clockTheme.cornerRadius.dp)
            ),
        shape = RoundedCornerShape(clockTheme.cornerRadius.dp),
        colors = CardDefaults.cardColors(
            containerColor = clockTheme.unselectedItemBackground
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // AM Selection
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable { onAmPmChanged(true) }
                    .background(
                        brush = if (isAm) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    clockTheme.selectedItemBackgroundStart,
                                    clockTheme.selectedItemBackgroundEnd
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    clockTheme.unselectedItemBackground,
                                    clockTheme.unselectedItemBackground
                                )
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AM",
                    style = clockTheme.numberStyle.copy(
                        fontSize = 22.sp,
                    fontWeight = if (isAm) FontWeight.Bold else FontWeight.Normal,
                        color = if (isAm) clockTheme.selectedTextColor
                               else clockTheme.unselectedTextColor
                    )
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(clockTheme.dividerColor)
            )

            // PM Selection
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable { onAmPmChanged(false) }
                    .background(
                        brush = if (!isAm) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    clockTheme.selectedItemBackgroundStart,
                                    clockTheme.selectedItemBackgroundEnd
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    clockTheme.unselectedItemBackground,
                                    clockTheme.unselectedItemBackground
                                )
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PM",
                    style = clockTheme.numberStyle.copy(
                        fontSize = 22.sp,
                    fontWeight = if (!isAm) FontWeight.Bold else FontWeight.Normal,
                        color = if (!isAm) clockTheme.selectedTextColor
                               else clockTheme.unselectedTextColor
                    )
                )
            }
        }
    }
}
