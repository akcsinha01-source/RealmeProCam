package com.realme.procam.ui.components

import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realme.procam.processing.DarkroomEngine
import com.realme.procam.processing.DarkroomEngine.PresetLut
import com.realme.procam.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniDarkroomSheet(
    capturedBitmap: Bitmap?,
    onDismiss: () -> Unit,
    onSaveAdjusted: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    if (capturedBitmap == null) return

    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    var selectedLut by remember { mutableStateOf(PresetLut.CINEMATIC_TEAL_ORANGE) }
    var clarity by remember { mutableStateOf(15f) }
    var shadows by remember { mutableStateOf(20f) }
    var highlights by remember { mutableStateOf(-15f) }
    var watermarkEnabled by remember { mutableStateOf(true) }

    var previewBitmap by remember(capturedBitmap) { mutableStateOf(capturedBitmap) }
    var isProcessing by remember { mutableStateOf(false) }

    // Recompute preview when adjustments change
    LaunchedEffect(selectedLut, clarity, shadows, highlights, watermarkEnabled) {
        isProcessing = true
        previewBitmap = DarkroomEngine.applyAdjustments(
            source = capturedBitmap,
            lut = selectedLut,
            clarity = clarity,
            shadows = shadows,
            highlights = highlights,
            addWatermark = watermarkEnabled
        )
        isProcessing = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = OledSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = OledBorder) },
        modifier = modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = RealmeGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MINI DARKROOM (POST-EDITOR)",
                        color = RealmeGold,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Photo Preview with Border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, OledBorder, RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = "Darkroom Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                if (isProcessing) {
                    CircularProgressIndicator(
                        color = RealmeGold,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3D LUT Presets
            Text(
                text = "3D LUT COLOR PROFILES",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PresetLut.values()) { lut ->
                    val isSelected = lut == selectedLut
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) RealmeGold.copy(alpha = 0.2f) else OledCard)
                            .border(1.dp, if (isSelected) RealmeGold else OledBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                selectedLut = lut
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = lut.displayName,
                            color = if (isSelected) RealmeGold else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Micro-Contrast & Clarity Slider
            AdjustmentSlider(
                title = "CLARITY & MICRO-CONTRAST",
                value = clarity,
                onValueChange = { clarity = it },
                range = -50f..50f
            )

            // Shadow Recovery Slider
            AdjustmentSlider(
                title = "SHADOW RECOVERY",
                value = shadows,
                onValueChange = { shadows = it },
                range = 0f..80f
            )

            // Highlight Compression Slider
            AdjustmentSlider(
                title = "HIGHLIGHT CONTROL",
                value = highlights,
                onValueChange = { highlights = it },
                range = -60f..0f
            )

            // Watermark Toggle Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(OledCard)
                    .border(1.dp, OledBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Realme 14 Pro+ EXIF Watermark",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "50MP Periscope OIS | 73mm eq ƒ/2.65",
                        color = RealmeGold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Switch(
                    checked = watermarkEnabled,
                    onCheckedChange = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        watermarkEnabled = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = RealmeGold,
                        checkedTrackColor = RealmeGold.copy(alpha = 0.35f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Master Button
            Button(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onSaveAdjusted(previewBitmap)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RealmeGold),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SAVE MASTER PHOTO TO GALLERY",
                    color = Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AdjustmentSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text(text = String.format("%+.0f", value), color = RealmeGold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = RealmeGold,
                activeTrackColor = RealmeGold,
                inactiveTrackColor = OledBorder
            )
        )
    }
}
