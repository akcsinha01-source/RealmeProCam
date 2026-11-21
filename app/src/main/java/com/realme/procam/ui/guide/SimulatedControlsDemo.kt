package com.realme.procam.ui.guide

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realme.procam.theme.*
import kotlin.math.abs
import kotlin.random.Random

/**
 * Interactive mini-simulator for users to play with ISO, Shutter Speed,
 * and Focus Peaking inside the Feature Guide before shooting!
 */
@Composable
fun SimulatedIsoDemo(modifier: Modifier = Modifier) {
    var isoValue by remember { mutableStateOf(100f) }
    val view = LocalView.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OledCard, RoundedCornerShape(14.dp))
            .border(1.dp, OledBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "ISO सिमुलेटर (Noise vs Brightness)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = "ISO ${isoValue.toInt()}", color = RealmeGold, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Visual Canvas showing Noise simulation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F1218))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val noiseFactor = ((isoValue - 100f) / 6300f).coerceIn(0f, 1f)
                val dotCount = (noiseFactor * 1200).toInt()

                // Subject in dark room
                drawCircle(
                    color = Color(0xFF2A364F).copy(alpha = 0.8f + noiseFactor * 0.2f),
                    radius = 45.dp.toPx(),
                    center = Offset(size.width / 2f, size.height / 2f)
                )

                // Simulated digital noise grain
                val rnd = Random(42)
                for (i in 0 until dotCount) {
                    val x = rnd.nextFloat() * size.width
                    val y = rnd.nextFloat() * size.height
                    val colorGrain = if (rnd.nextBoolean()) Color.White.copy(alpha = 0.35f) else Color(0xFFFF5252).copy(alpha = 0.35f)
                    drawCircle(colorGrain, radius = 1.2f, center = Offset(x, y))
                }
            }

            Text(
                text = if (isoValue < 800) "कम नॉइज़ (Clean Shadows)" else "ज़्यादा ब्राइटनेस + ग्रेन (Digital Noise)",
                color = if (isoValue < 800) FocusGreen else AccentOrange,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
            )
        }

        Slider(
            value = isoValue,
            onValueChange = {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                isoValue = it
            },
            valueRange = 100f..6400f,
            colors = SliderDefaults.colors(thumbColor = RealmeGold, activeTrackColor = RealmeGold)
        )
    }
}

@Composable
fun SimulatedShutterDemo(modifier: Modifier = Modifier) {
    var shutterFrac by remember { mutableStateOf(0.1f) }
    val view = LocalView.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OledCard, RoundedCornerShape(14.dp))
            .border(1.dp, OledBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        val speedText = if (shutterFrac < 0.3f) "1/2000s (Action Freeze)" else if (shutterFrac < 0.7f) "1/60s (Normal)" else "2.0s (Motion Blur / Silk)"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "शटर स्पीड सिमुलेटर (Freeze vs Blur)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = speedText, color = RealmeGold, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F1218))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val blurSpread = (shutterFrac * 90.dp.toPx())

                // Simulated moving ball / car light
                if (shutterFrac < 0.25f) {
                    // Crisp frozen circle
                    drawCircle(color = AccentCyan, radius = 18.dp.toPx(), center = Offset(cx, cy))
                } else {
                    // Motion blur streak
                    drawLine(
                        color = AccentCyan.copy(alpha = 0.7f),
                        start = Offset(cx - blurSpread / 2f, cy),
                        end = Offset(cx + blurSpread / 2f, cy),
                        strokeWidth = 36.dp.toPx()
                    )
                }
            }

            Text(
                text = if (shutterFrac < 0.3f) "तेज़ शटर: चलती चीज़ें जम जाती हैं" else "धीमा शटर: पानी और रोशनी की लकीरें (Light Trails)",
                color = AccentCyan,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
            )
        }

        Slider(
            value = shutterFrac,
            onValueChange = {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                shutterFrac = it
            },
            valueRange = 0.05f..1.0f,
            colors = SliderDefaults.colors(thumbColor = RealmeGold, activeTrackColor = RealmeGold)
        )
    }
}

@Composable
fun SimulatedFocusPeakingDemo(modifier: Modifier = Modifier) {
    var focusDial by remember { mutableStateOf(5.0f) }
    val view = LocalView.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OledCard, RoundedCornerShape(14.dp))
            .border(1.dp, OledBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "फोकस पीकिंग (हरी किनारी संकेत)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = String.format("Distance: %.1fm", 10.0f / (focusDial + 0.1f)), color = FocusGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F1218))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                // Subject 1: Near flower (Peak at dial ~8)
                val isNearInFocus = abs(focusDial - 8.0f) < 1.0f
                drawCircle(
                    color = Color(0xFFD48B28),
                    radius = 24.dp.toPx(),
                    center = Offset(cx - 70.dp.toPx(), cy)
                )
                if (isNearInFocus) {
                    drawCircle(
                        color = FocusGreen,
                        radius = 25.dp.toPx(),
                        center = Offset(cx - 70.dp.toPx(), cy),
                        style = Stroke(3.dp.toPx())
                    )
                }

                // Subject 2: Far building (Peak at dial ~2)
                val isFarInFocus = abs(focusDial - 2.0f) < 1.0f
                drawRect(
                    color = Color(0xFF4A5568),
                    topLeft = Offset(cx + 40.dp.toPx(), cy - 30.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(60.dp.toPx(), 60.dp.toPx())
                )
                if (isFarInFocus) {
                    drawRect(
                        color = FocusGreen,
                        topLeft = Offset(cx + 39.dp.toPx(), cy - 31.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(62.dp.toPx(), 62.dp.toPx()),
                        style = Stroke(3.dp.toPx())
                    )
                }
            }

            Text(
                text = "जब विषय पर हरी रेखा दिखे, तो वह एकदम शार्प फोकस में है!",
                color = FocusGreen,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
            )
        }

        Slider(
            value = focusDial,
            onValueChange = {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                focusDial = it
            },
            valueRange = 0.0f..10.0f,
            colors = SliderDefaults.colors(thumbColor = FocusGreen, activeTrackColor = FocusGreen)
        )
    }
}
