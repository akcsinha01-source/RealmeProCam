package com.realme.procam.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realme.procam.theme.*
import kotlin.math.abs

enum class GridType {
    NONE,
    RULE_OF_THIRDS,
    GOLDEN_RATIO,
    CROSSHAIRS
}

@Composable
fun FramingGridOverlay(
    gridType: GridType,
    modifier: Modifier = Modifier
) {
    if (gridType == GridType.NONE) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val linePaint = Color.White.copy(alpha = 0.35f)
        val strokeWidth = 1.dp.toPx()

        when (gridType) {
            GridType.RULE_OF_THIRDS -> {
                // Vertical lines at 1/3 and 2/3
                drawLine(linePaint, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth)
                drawLine(linePaint, Offset(2f * w / 3f, 0f), Offset(2f * w / 3f, h), strokeWidth)
                // Horizontal lines at 1/3 and 2/3
                drawLine(linePaint, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth)
                drawLine(linePaint, Offset(0f, 2f * h / 3f), Offset(w, 2f * h / 3f), strokeWidth)
            }
            GridType.GOLDEN_RATIO -> {
                val phi = 0.618f
                val x1 = w * (1f - phi)
                val x2 = w * phi
                val y1 = h * (1f - phi)
                val y2 = h * phi

                drawLine(linePaint, Offset(x1, 0f), Offset(x1, h), strokeWidth)
                drawLine(linePaint, Offset(x2, 0f), Offset(x2, h), strokeWidth)
                drawLine(linePaint, Offset(0f, y1), Offset(w, y1), strokeWidth)
                drawLine(linePaint, Offset(0f, y2), Offset(w, y2), strokeWidth)
            }
            GridType.CROSSHAIRS -> {
                val cx = w / 2f
                val cy = h / 2f
                val arm = 30.dp.toPx()
                drawLine(RealmeGold.copy(alpha = 0.6f), Offset(cx - arm, cy), Offset(cx + arm, cy), 1.5f)
                drawLine(RealmeGold.copy(alpha = 0.6f), Offset(cx, cy - arm), Offset(cx, cy + arm), 1.5f)
                drawCircle(RealmeGold.copy(alpha = 0.4f), radius = 12.dp.toPx(), center = Offset(cx, cy), style = Stroke(1.5f))
            }
            GridType.NONE -> {}
        }
    }
}

/**
 * Electronic Horizon Level with Real-Time Gyro Roll & Pitch.
 * Snaps to vivid green (#00E676) when the camera is level.
 */
@Composable
fun ElectronicHorizonOverlay(
    rollAngle: Float,
    isLevel: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(240.dp, 80.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val barLength = 70.dp.toPx()
            val lineColor = if (isLevel) FocusGreen else Color.White.copy(alpha = 0.5f)
            val stroke = if (isLevel) 2.5f else 1.5f

            // Center target dot
            drawCircle(lineColor, radius = 3.dp.toPx(), center = Offset(cx, cy))

            // Rotated Horizon Line
            val radians = Math.toRadians(rollAngle.toDouble())
            val cos = Math.cos(radians).toFloat()
            val sin = Math.sin(radians).toFloat()

            // Left wing
            drawLine(
                color = lineColor,
                start = Offset(cx - 15.dp.toPx() * cos, cy - 15.dp.toPx() * sin),
                end = Offset(cx - (15.dp.toPx() + barLength) * cos, cy - (15.dp.toPx() + barLength) * sin),
                strokeWidth = stroke
            )

            // Right wing
            drawLine(
                color = lineColor,
                start = Offset(cx + 15.dp.toPx() * cos, cy + 15.dp.toPx() * sin),
                end = Offset(cx + (15.dp.toPx() + barLength) * cos, cy + (15.dp.toPx() + barLength) * sin),
                strokeWidth = stroke
            )
        }

        // Tilt angle readout pill
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 42.dp)
                .background(OledSurface.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                .border(1.dp, if (isLevel) FocusGreen else Color.Transparent, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = String.format("%+.1f°", rollAngle),
                color = if (isLevel) FocusGreen else TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Live Cinematography RGB + Luma Waveform / Histogram.
 */
@Composable
fun LiveHistogramView(
    lumaHistogram: IntArray?,
    modifier: Modifier = Modifier
) {
    if (lumaHistogram == null || lumaHistogram.isEmpty()) return

    Box(
        modifier = modifier
            .size(130.dp, 60.dp)
            .background(OledSurface.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .border(1.dp, OledBorder, RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxCount = (lumaHistogram.maxOrNull() ?: 1).toFloat().coerceAtLeast(1f)
            val w = size.width
            val h = size.height
            val path = Path()

            path.moveTo(0f, h)
            for (i in 0 until 256) {
                val x = (i / 255f) * w
                val count = lumaHistogram.getOrElse(i) { 0 }
                val y = h - ((count / maxCount) * h)
                path.lineTo(x, y)
            }
            path.lineTo(w, h)
            path.close()

            drawPath(path, color = AccentCyan.copy(alpha = 0.45f))
            drawPath(path, color = RealmeGold, style = Stroke(width = 1.2f))
        }

        Text(
            text = "HISTOGRAM",
            color = TextMuted,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}
