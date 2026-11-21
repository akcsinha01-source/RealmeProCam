package com.realme.procam.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ShutterSpeed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realme.procam.camera.RealmeCameraConstants
import com.realme.procam.theme.*
import kotlin.math.roundToInt

enum class ManualParamTab(val label: String, val icon: ImageVector) {
    ISO("ISO", Icons.Default.Tune),
    SHUTTER("S (Shutter)", Icons.Default.ShutterSpeed),
    FOCUS("MF (Focus)", Icons.Default.CenterFocusStrong),
    WB("WB (Kelvin)", Icons.Default.WbSunny),
    EV("EV (Exp)", Icons.Default.AutoAwesome)
}

@Composable
fun ManualControlsPanel(
    iso: Int,
    isManualExposure: Boolean,
    onIsoChanged: (Int) -> Unit,
    onAutoExposureToggle: () -> Unit,
    shutterSpeedNs: Long,
    onShutterChanged: (Long) -> Unit,
    focusDistance: Float,
    isManualFocus: Boolean,
    onFocusDistanceChanged: (Float) -> Unit,
    onAutoFocusToggle: () -> Unit,
    focusPeakingEnabled: Boolean,
    onToggleFocusPeaking: (Boolean) -> Unit,
    whiteBalanceKelvin: Int,
    onWbChanged: (Int) -> Unit,
    evCompensation: Float,
    onEvChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ManualParamTab.ISO) }
    val view = LocalView.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OledSurface.copy(alpha = 0.92f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .border(1.dp, OledBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        // Tab Selector Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(ManualParamTab.values()) { tab ->
                val isSelected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) RealmeGold.copy(alpha = 0.18f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (isSelected) RealmeGold else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            selectedTab = tab
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.label,
                        color = if (isSelected) RealmeGold else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dynamic Parameter Slider based on active tab
        when (selectedTab) {
            ManualParamTab.ISO -> {
                IsoControlRow(
                    iso = iso,
                    isManual = isManualExposure,
                    onIsoChanged = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onIsoChanged(it)
                    },
                    onAutoToggle = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onAutoExposureToggle()
                    }
                )
            }
            ManualParamTab.SHUTTER -> {
                ShutterControlRow(
                    shutterNs = shutterSpeedNs,
                    isManual = isManualExposure,
                    onShutterChanged = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onShutterChanged(it)
                    },
                    onAutoToggle = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onAutoExposureToggle()
                    }
                )
            }
            ManualParamTab.FOCUS -> {
                FocusControlRow(
                    distance = focusDistance,
                    isManual = isManualFocus,
                    peaking = focusPeakingEnabled,
                    onFocusChanged = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onFocusDistanceChanged(it)
                    },
                    onAutoToggle = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onAutoFocusToggle()
                    },
                    onPeakingToggle = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onToggleFocusPeaking(it)
                    }
                )
            }
            ManualParamTab.WB -> {
                WbControlRow(
                    kelvin = whiteBalanceKelvin,
                    onWbChanged = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onWbChanged(it)
                    }
                )
            }
            ManualParamTab.EV -> {
                EvControlRow(
                    ev = evCompensation,
                    onEvChanged = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onEvChanged(it)
                    }
                )
            }
        }
    }
}

@Composable
private fun IsoControlRow(
    iso: Int,
    isManual: Boolean,
    onIsoChanged: (Int) -> Unit,
    onAutoToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AutoPillButton(isAuto = !isManual, onClick = onAutoToggle)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (!isManual) "AUTO (ISO ${iso})" else "MANUAL: ISO $iso",
                color = if (isManual) RealmeGold else AccentCyan,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = iso.toFloat(),
                onValueChange = { onIsoChanged(it.roundToInt()) },
                valueRange = 50f..12800f,
                colors = SliderDefaults.colors(
                    thumbColor = RealmeGold,
                    activeTrackColor = RealmeGold,
                    inactiveTrackColor = OledBorder
                )
            )
        }
    }
}

@Composable
private fun ShutterControlRow(
    shutterNs: Long,
    isManual: Boolean,
    onShutterChanged: (Long) -> Unit,
    onAutoToggle: () -> Unit
) {
    val seconds = shutterNs / 1_000_000_000.0
    val displayShutter = when {
        seconds >= 1.0 -> String.format("%.1fs", seconds)
        seconds > 0 -> "1/${(1.0 / seconds).roundToInt()}s"
        else -> "1/8000s"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AutoPillButton(isAuto = !isManual, onClick = onAutoToggle)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (!isManual) "AUTO ($displayShutter)" else "SHUTTER: $displayShutter",
                color = if (isManual) RealmeGold else AccentCyan,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            // Logarithmic scale representation (0 to 100)
            Slider(
                value = (shutterNs - RealmeCameraConstants.MIN_SHUTTER_NS).toFloat() /
                        (RealmeCameraConstants.MAX_SHUTTER_NS - RealmeCameraConstants.MIN_SHUTTER_NS).toFloat(),
                onValueChange = { frac ->
                    val ns = (RealmeCameraConstants.MIN_SHUTTER_NS + frac * (RealmeCameraConstants.MAX_SHUTTER_NS - RealmeCameraConstants.MIN_SHUTTER_NS)).toLong()
                    onShutterChanged(ns)
                },
                colors = SliderDefaults.colors(
                    thumbColor = RealmeGold,
                    activeTrackColor = RealmeGold,
                    inactiveTrackColor = OledBorder
                )
            )
        }
    }
}

@Composable
private fun FocusControlRow(
    distance: Float,
    isManual: Boolean,
    peaking: Boolean,
    onFocusChanged: (Float) -> Unit,
    onAutoToggle: () -> Unit,
    onPeakingToggle: (Boolean) -> Unit
) {
    val displayFocus = when {
        !isManual -> "AUTO FOCUS (AF-C)"
        distance <= 0.1f -> "MANUAL: ∞ (Infinity)"
        distance >= 9.5f -> "MANUAL: 10cm (Super Macro)"
        else -> String.format("MANUAL: %.1f diopters (~%.1fm)", distance, 1.0f / distance)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AutoPillButton(isAuto = !isManual, onClick = onAutoToggle)
            Spacer(modifier = Modifier.width(8.dp))

            // Focus Peaking Toggle Pill
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (peaking) FocusGreen.copy(alpha = 0.2f) else OledCard)
                    .border(1.dp, if (peaking) FocusGreen else OledBorder, CircleShape)
                    .clickable { onPeakingToggle(!peaking) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (peaking) "PEAK ON" else "PEAK OFF",
                    color = if (peaking) FocusGreen else TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = displayFocus,
                color = if (isManual) RealmeGold else AccentCyan,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = distance,
            onValueChange = { onFocusChanged(it) },
            valueRange = 0.0f..10.0f,
            colors = SliderDefaults.colors(
                thumbColor = if (peaking) FocusGreen else RealmeGold,
                activeTrackColor = if (peaking) FocusGreen else RealmeGold,
                inactiveTrackColor = OledBorder
            )
        )
    }
}

@Composable
private fun WbControlRow(
    kelvin: Int,
    onWbChanged: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "WHITE BALANCE: ${kelvin}K (${if (kelvin == 5500) "Daylight Auto" else if (kelvin < 4000) "Warm Tungsten" else "Cool Cloudy"})",
            color = RealmeGold,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Slider(
            value = kelvin.toFloat(),
            onValueChange = { onWbChanged(it.roundToInt()) },
            valueRange = 2000f..8000f,
            colors = SliderDefaults.colors(
                thumbColor = RealmeGold,
                activeTrackColor = RealmeGold,
                inactiveTrackColor = OledBorder
            )
        )
    }
}

@Composable
private fun EvControlRow(
    ev: Float,
    onEvChanged: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = String.format("EXPOSURE COMPENSATION: %+.1f EV", ev),
            color = RealmeGold,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Slider(
            value = ev,
            onValueChange = { onEvChanged(it) },
            valueRange = -3.0f..+3.0f,
            steps = 11, // 0.5 EV steps
            colors = SliderDefaults.colors(
                thumbColor = RealmeGold,
                activeTrackColor = RealmeGold,
                inactiveTrackColor = OledBorder
            )
        )
    }
}

@Composable
private fun AutoPillButton(
    isAuto: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isAuto) AccentCyan.copy(alpha = 0.2f) else OledCard)
            .border(1.dp, if (isAuto) AccentCyan else OledBorder, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = if (isAuto) "AUTO" else "MANUAL",
            color = if (isAuto) AccentCyan else TextMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
