package com.realme.procam.ui.guide

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realme.procam.theme.*

/**
 * Interactive Split-Screen Before / After Comparison Slider:
 * Visually illustrates standard single-lens shot with blurry background (bokeh falloff)
 * versus the Dual-Lens Deep Focus Stacked photo with edge-to-edge optical sharpness!
 */
@Composable
fun BeforeAfterSlider(
    modifier: Modifier = Modifier
) {
    var splitPosition by remember { mutableStateOf(0.5f) }
    val view = LocalView.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, OledBorder, RoundedCornerShape(16.dp))
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val newPos = (change.position.x / size.width).coerceIn(0.05f, 0.95f)
                    if ((newPos * 20).toInt() != (splitPosition * 20).toInt()) {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                    splitPosition = newPos
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val splitX = w * splitPosition

            // LEFT SIDE: Standard Single Shot (Foreground sharp, background out-of-focus bokeh)
            clipRect(left = 0f, top = 0f, right = splitX, bottom = h) {
                // Background blurry simulation (soft gradient)
                drawRect(
                    color = Color(0xFF1E2430),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h)
                )
                // Blurry distant mountain/building
                drawCircle(
                    color = Color(0xFF384358).copy(alpha = 0.5f),
                    radius = 120f,
                    center = Offset(w * 0.75f, h * 0.45f)
                )
                // Sharp near object
                drawCircle(
                    color = RealmeGold,
                    radius = 70f,
                    center = Offset(w * 0.28f, h * 0.55f)
                )
            }

            // RIGHT SIDE: Dual-Lens Focus Stack (Both foreground and background razor-sharp)
            clipRect(left = splitX, top = 0f, right = w, bottom = h) {
                // Background sharp high-contrast details
                drawRect(
                    color = Color(0xFF0F1520),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h)
                )
                // Crisp 3x telephoto distant mountain/architecture
                drawCircle(
                    color = AccentCyan,
                    radius = 110f,
                    center = Offset(w * 0.75f, h * 0.45f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = 20f,
                    center = Offset(w * 0.75f, h * 0.45f)
                )
                // Crisp 1x foreground
                drawCircle(
                    color = RealmeGold,
                    radius = 70f,
                    center = Offset(w * 0.28f, h * 0.55f)
                )
            }

            // Divider vertical line
            drawLine(
                color = Color.White,
                start = Offset(splitX, 0f),
                end = Offset(splitX, h),
                strokeWidth = 2.5.dp.toPx()
            )
        }

        // Draggable Knob
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (230.dp * splitPosition) - 18.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(RealmeGold)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = "Drag to compare", tint = Black, modifier = Modifier.size(20.dp))
        }

        // Label Left (Before)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .background(Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "सामान्य 1x (धुंधला बैकग्राउंड)",
                color = TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Label Right (After)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .background(Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "हाइब्रिड फोकस स्टैक (सब कुछ शार्प)",
                color = RealmeGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Drag instruction hint at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .background(Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Text(
                text = "⟵ बीच की रेखा को बाएँ-दाएँ खींचकर अंतर देखें ⟶",
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}
