package com.realme.procam.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realme.procam.camera.RealmeCameraConstants.LensType
import com.realme.procam.theme.*
import kotlinx.coroutines.delay
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

data class PhysicalLensOption(
    val label: String,
    val zoom: Float,
    val sensorName: String,
    val lensType: LensType
)

val PHYSICAL_LENSES = listOf(
    PhysicalLensOption(".6", 0.6f, "Ultra-Wide", LensType.ULTRA_WIDE),
    PhysicalLensOption("1x", 1.0f, "Main 50MP IMX890", LensType.MAIN_WIDE),
    PhysicalLensOption("3x", 3.0f, "Periscope 50MP IMX882", LensType.PERISCOPE),
    PhysicalLensOption("6x", 6.0f, "Lossless In-Sensor", LensType.IN_SENSOR)
)

/**
 * Clean Stock Camera style Lens Selector & Auto-Hiding Rotary Zoom Wheel.
 * - By default: Only clean physical lens selector pills (.6, 1x, 3x, 6x) are shown (no black bars).
 * - When zooming: The 120fps graduation wheel slides in smoothly.
 * - Inactivity: Auto-hides after 2.5 seconds, returning to a completely clean viewfinder.
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

    // Auto-hide wheel state
    var isWheelVisible by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(0L) }

    // Sync external changes (e.g. pinch-to-zoom)
    LaunchedEffect(currentZoom) {
        localZoom = currentZoom
        visualOffset = (ln(currentZoom / 0.6f) * 160f)
    }

    // Auto-hide timer: collapses wheel after 2.5 seconds of inactivity
    LaunchedEffect(lastInteractionTime) {
        if (lastInteractionTime > 0L) {
            delay(2500)
            isWheelVisible = false
        }
    }

    val quickPresets = listOf(0.6f, 1.0f, 3.0f, 6.0f, 10.0f, 30.0f, 60.0f, 120.0f, 200.0f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Sliding/Rotary Wheel Section (ONLY VISIBLE WHEN ACTIVELY ZOOMING)
        AnimatedVisibility(
            visible = isWheelVisible,
            enter = fadeIn(animationSpec = tween(180)) + expandVertically(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(250)) + shrinkVertically(animationSpec = tween(250))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Zoom Ratio & Lens Badge
                val lensLabel = when {
                    localZoom < 0.95f -> "0.6x ULTRA-WIDE"
                    localZoom < 2.95f -> "1x MAIN WIDE (Sony IMX890)"
                    localZoom < 5.95f -> "3x OPTICAL PERISCOPE (Sony IMX882)"
                    localZoom < 10.0f -> "6x IN-SENSOR LOSSLESS"
                    else -> "⚡ TELESCOPIC ULTRA-ZOOM (Max 200x)"
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(
                            1.dp,
                            if (localZoom >= 3.0f) AccentCyan.copy(alpha = 0.8f) else RealmeGold.copy(alpha = 0.8f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = String.format("%.1fx", localZoom),
                        color = if (localZoom >= 3.0f) AccentCyan else RealmeGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• $lensLabel",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Extended Quick Presets Row
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
                                        Color.Black.copy(alpha = 0.6f)
                                    }
                                )
                                .border(1.dp, if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.2f), CircleShape)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    localZoom = preset
                                    visualOffset = (ln(preset / 0.6f) * 160f)
                                    lastInteractionTime = System.currentTimeMillis()
                                    onZoomChange(preset)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (preset == 0.6f) "0.6x" else "${preset.toInt()}x",
                                color = if (isSelected) Black else if (preset >= 3.0f) AccentCyan else Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Tactile Continuous Zoom Wheel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(Color.Black.copy(alpha = 0.70f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(23.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                onDragEnd = {
                                    val finalZoom = (localZoom * 10).roundToInt() / 10f
                                    onZoomChange(finalZoom)
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                lastInteractionTime = System.currentTimeMillis()
                                val delta = dragAmount.x
                                visualOffset += delta
                                val newZoom = (0.6f * exp(visualOffset / 160f)).coerceIn(0.6f, 200.0f)
                                localZoom = newZoom

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
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val centerX = w / 2f
                        val tickSpacing = 16.dp.toPx()
                        val offsetMod = visualOffset % tickSpacing
                        val visibleCount = (w / tickSpacing).toInt() + 4

                        for (i in -visibleCount / 2..visibleCount / 2) {
                            val x = centerX + i * tickSpacing + offsetMod
                            if (x < 36.dp.toPx() || x > w - 36.dp.toPx()) continue
                            val distFromCenter = kotlin.math.abs(x - centerX)
                            val alpha = (1.0f - (distFromCenter / (centerX * 0.9f))).coerceIn(0f, 1f)
                            val isMajor = ((i - (visualOffset / tickSpacing).toInt()) % 5) == 0
                            val tickHeight = if (isMajor) h * 0.45f else h * 0.25f

                            drawLine(
                                color = (if (localZoom >= 3.0f) AccentCyan else Color.White).copy(alpha = alpha * 0.7f),
                                start = Offset(x, (h - tickHeight) / 2f),
                                end = Offset(x, (h + tickHeight) / 2f),
                                strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                            )
                        }

                        // Center pointer
                        val pointerColor = if (localZoom >= 3.0f) AccentCyan else RealmeGold
                        val path = Path().apply {
                            moveTo(centerX - 5.dp.toPx(), 4.dp.toPx())
                            lineTo(centerX + 5.dp.toPx(), 4.dp.toPx())
                            lineTo(centerX, 11.dp.toPx())
                            close()
                        }
                        drawPath(path, color = pointerColor)

                        drawLine(
                            color = pointerColor,
                            start = Offset(centerX, 10.dp.toPx()),
                            end = Offset(centerX, h - 10.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    // Fine step buttons [-] and [+]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    val step = if (localZoom <= 3f) 0.2f else if (localZoom <= 10f) 0.5f else 5.0f
                                    val target = (localZoom - step).coerceAtLeast(0.6f)
                                    localZoom = target
                                    visualOffset = (ln(target / 0.6f) * 160f)
                                    lastInteractionTime = System.currentTimeMillis()
                                    onZoomChange(target)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "−", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    val step = if (localZoom < 3f) 0.2f else if (localZoom < 10f) 0.5f else 5.0f
                                    val target = (localZoom + step).coerceAtMost(200.0f)
                                    localZoom = target
                                    visualOffset = (ln(target / 0.6f) * 160f)
                                    lastInteractionTime = System.currentTimeMillis()
                                    onZoomChange(target)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // 2. PRIMARY PHYSICAL LENS SELECTOR (ALWAYS VISIBLE & CLEAN - NO BLACK STRIP!)
        // Dedicated option for user to choose exactly which lens to photograph with:
        // [.6 Ultra-Wide]  [1x Main 50MP]  [3x Periscope 50MP]  [6x Lossless]
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Color.Black.copy(alpha = 0.50f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PHYSICAL_LENSES.forEach { lensOpt ->
                val isSelected = when (lensOpt.lensType) {
                    LensType.ULTRA_WIDE -> localZoom < 0.95f
                    LensType.MAIN_WIDE -> localZoom in 0.95f..2.5f
                    LensType.PERISCOPE -> localZoom in 2.51f..4.9f
                    LensType.IN_SENSOR -> localZoom >= 4.91f
                }

                // If currently at non-standard zoom (e.g. 15.2x), show the actual zoom text on the selected pill
                val displayText = if (isSelected && kotlin.math.abs(localZoom - lensOpt.zoom) > 0.3f) {
                    String.format("%.1fx", localZoom)
                } else {
                    lensOpt.label
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                if (lensOpt.zoom >= 3.0f) AccentCyan else RealmeGold
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            localZoom = lensOpt.zoom
                            visualOffset = (ln(lensOpt.zoom / 0.6f) * 160f)
                            onZoomChange(lensOpt.zoom)
                            // Tapping the active lens pill toggles the rotary wheel for fine tuning
                            if (isSelected) {
                                isWheelVisible = !isWheelVisible
                                if (isWheelVisible) lastInteractionTime = System.currentTimeMillis()
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    isWheelVisible = true
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                onDragEnd = {
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                isWheelVisible = true
                                lastInteractionTime = System.currentTimeMillis()
                                val delta = dragAmount.x
                                visualOffset += delta
                                val newZoom = (0.6f * exp(visualOffset / 160f)).coerceIn(0.6f, 200.0f)
                                localZoom = newZoom
                                val step = if (newZoom < 3f) 0.1f else 0.5f
                                if (kotlin.math.abs(newZoom - lastSentZoom) >= step) {
                                    lastSentZoom = (newZoom * 10).roundToInt() / 10f
                                    onZoomChange(lastSentZoom)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayText,
                        color = if (isSelected) Black else Color.White,
                        fontSize = if (displayText.length > 3) 10.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
