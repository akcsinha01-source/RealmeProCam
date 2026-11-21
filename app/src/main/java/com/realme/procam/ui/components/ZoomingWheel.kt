package com.realme.procam.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realme.procam.theme.*
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Tactical Rotary Zooming Wheel with up to 200x Telescopic Periscope Zoom.
 * Silky-smooth 120fps sliding graduation ticks with rate-limited HAL updates.
 */
@Composable
fun ZoomingWheel(
    currentZoom: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var localZoom by remember { mutableFloatStateOf(currentZoom) }
    var visualOffset by remember { mutableFloatStateOf(0f) }
    var lastSentZoom by remember { mutableFloatStateOf(currentZoom) }

    // Sync external changes (e.g. preset tapped)
    LaunchedEffect(currentZoom) {
        localZoom = currentZoom
        visualOffset = (ln(currentZoom / 0.6f) * 160f)
    }

    val quickPresets = listOf(0.6f, 1.0f, 3.0f, 6.0f, 10.0f, 30.0f, 60.0f, 120.0f, 200.0f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Current Zoom Indicator Badge
        val lensLabel = when {
            localZoom < 0.95f -> "0.6x ULTRA-WIDE"
            localZoom < 2.95f -> "1x MAIN WIDE (Sony IMX890)"
            localZoom < 5.95f -> "3x OPTICAL PERISCOPE (Sony IMX882)"
            localZoom < 10.0f -> "6x IN-SENSOR LOSSLESS CROP"
            else -> "⚡ TELESCOPIC ULTRA-ZOOM (Max 200x)"
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(OledSurface.copy(alpha = 0.90f))
                .border(
                    1.dp,
                    if (localZoom >= 3.0f) AccentCyan.copy(alpha = 0.7f) else RealmeGold.copy(alpha = 0.7f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = String.format("%.1fx", localZoom),
                color = if (localZoom >= 3.0f) AccentCyan else RealmeGold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "• $lensLabel",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Quick Preset Pills
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(quickPresets) { preset ->
                val isSelected = kotlin.math.abs(localZoom - preset) < 0.25f
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                if (preset >= 3.0f) AccentCyan else RealmeGold
                            } else {
                                OledCard.copy(alpha = 0.85f)
                            }
                        )
                        .border(1.dp, if (isSelected) Color.Transparent else OledBorder, CircleShape)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            localZoom = preset
                            visualOffset = (ln(preset / 0.6f) * 160f)
                            onZoomChange(preset)
                        }
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (preset == 0.6f) "0.6x" else "${preset.toInt()}x",
                        color = if (isSelected) Black else if (preset >= 3.0f) AccentCyan else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Tactile Continuous Zoom Wheel with Silky-Smooth Drag & Sliding Marks
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(OledSurface.copy(alpha = 0.85f))
                .border(1.dp, OledBorder, RoundedCornerShape(24.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            val finalZoom = (localZoom * 10).roundToInt() / 10f
                            onZoomChange(finalZoom)
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val delta = dragAmount.x
                        visualOffset += delta
                        // Smooth logarithmic scaling: 0.6x to 200x
                        val newZoom = (0.6f * exp(visualOffset / 160f)).coerceIn(0.6f, 200.0f)
                        localZoom = newZoom

                        // Dispatch throttled step updates
                        val step = if (newZoom < 3f) 0.1f else if (newZoom < 10f) 0.2f else if (newZoom < 50f) 1.0f else 2.0f
                        if (kotlin.math.abs(newZoom - lastSentZoom) >= step) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            lastSentZoom = (newZoom * 10).roundToInt() / 10f
                            onZoomChange(lastSentZoom)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Drawn Moving Graduation Ticks
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val centerX = w / 2f

                val tickSpacing = 16.dp.toPx()
                val offsetMod = visualOffset % tickSpacing

                val visibleCount = (w / tickSpacing).toInt() + 4
                for (i in -visibleCount / 2..visibleCount / 2) {
                    val x = centerX + i * tickSpacing + offsetMod
                    if (x < 40.dp.toPx() || x > w - 40.dp.toPx()) continue

                    val distFromCenter = kotlin.math.abs(x - centerX)
                    val alpha = (1.0f - (distFromCenter / (centerX * 0.9f))).coerceIn(0f, 1f)
                    val isMajor = ((i - (visualOffset / tickSpacing).toInt()) % 5) == 0
                    val tickHeight = if (isMajor) h * 0.45f else h * 0.25f

                    drawLine(
                        color = (if (localZoom >= 3.0f) AccentCyan else TextMuted).copy(alpha = alpha * 0.75f),
                        start = Offset(x, (h - tickHeight) / 2f),
                        end = Offset(x, (h + tickHeight) / 2f),
                        strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                    )
                }

                // Center Gold/Cyan Indicator Pointer
                val pointerColor = if (localZoom >= 3.0f) AccentCyan else RealmeGold
                val path = Path().apply {
                    moveTo(centerX - 6.dp.toPx(), 4.dp.toPx())
                    lineTo(centerX + 6.dp.toPx(), 4.dp.toPx())
                    lineTo(centerX, 12.dp.toPx())
                    close()
                }
                drawPath(path, color = pointerColor)

                drawLine(
                    color = pointerColor,
                    start = Offset(centerX, 10.dp.toPx()),
                    end = Offset(centerX, h - 10.dp.toPx()),
                    strokeWidth = 2.5.dp.toPx()
                )
            }

            // Left Fine Step [-] and Right Fine Step [+] Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(OledCard)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            val step = if (localZoom <= 3f) 0.2f else if (localZoom <= 10f) 0.5f else 5.0f
                            val target = (localZoom - step).coerceAtLeast(0.6f)
                            localZoom = target
                            visualOffset = (ln(target / 0.6f) * 160f)
                            onZoomChange(target)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "−", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(OledCard)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            val step = if (localZoom < 3f) 0.2f else if (localZoom < 10f) 0.5f else 5.0f
                            val target = (localZoom + step).coerceAtMost(200.0f)
                            localZoom = target
                            visualOffset = (ln(target / 0.6f) * 160f)
                            onZoomChange(target)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "+", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
