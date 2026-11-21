package com.realme.procam.ui

import android.graphics.SurfaceTexture
import android.view.HapticFeedbackConstants
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.realme.procam.camera.RealmeCameraConstants.CameraMode
import com.realme.procam.camera.RealmeCameraConstants.LensType
import com.realme.procam.processing.HybridFocusStacker
import com.realme.procam.theme.*
import com.realme.procam.ui.components.*
import com.realme.procam.ui.guide.FeatureGuideScreen
import com.realme.procam.ui.viewmodel.CameraViewModel

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    // Observe State
    val activeMode by viewModel.activeMode.collectAsState()
    val currentLens by viewModel.cameraController.currentLens.collectAsState()
    val iso by viewModel.cameraController.iso.collectAsState()
    val isManualExposure by viewModel.cameraController.isManualExposure.collectAsState()
    val shutterSpeedNs by viewModel.cameraController.shutterSpeedNs.collectAsState()
    val focusDistance by viewModel.cameraController.focusDistance.collectAsState()
    val isManualFocus by viewModel.cameraController.isManualFocus.collectAsState()
    val focusPeakingEnabled by viewModel.cameraController.focusPeakingEnabled.collectAsState()
    val whiteBalanceKelvin by viewModel.cameraController.whiteBalanceKelvin.collectAsState()
    val evCompensation by viewModel.cameraController.evCompensation.collectAsState()
    val isProcessingCapture by viewModel.cameraController.isProcessingCapture.collectAsState()

    // Dual-Lens Stacking State
    val stackerPhase by viewModel.focusStacker.phase.collectAsState()
    val stackerStatus by viewModel.focusStacker.statusMessage.collectAsState()
    val ghostOverlayBitmap by viewModel.focusStacker.ghostOverlayBitmap.collectAsState()

    // Overlays & Cinematography
    val gridType by viewModel.gridType.collectAsState()
    val rollAngle by viewModel.sensorHelper.rollAngle.collectAsState()
    val isLevel by viewModel.sensorHelper.isLevel.collectAsState()
    val rawEnabled by viewModel.rawCaptureEnabled.collectAsState()
    val zebraEnabled by viewModel.zebraStripesEnabled.collectAsState()
    val liveHistogram by viewModel.liveHistogram.collectAsState()
    val isGuideOpen by viewModel.isGuideOpen.collectAsState()
    val darkroomBitmap by viewModel.darkroomBitmap.collectAsState()
    val notification by viewModel.capturedNotification.collectAsState()

    // Focus Target Ring Animation on Tap
    var tapRingPosition by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
    ) {
        // 1. Android TextureView Viewfinder with Tap-To-Focus
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        tapRingPosition = offset
                        val normalizedX = (offset.x / size.width).coerceIn(0f, 1f)
                        val normalizedY = (offset.y / size.height).coerceIn(0f, 1f)
                        viewModel.cameraController.tapToFocus(normalizedX, normalizedY)
                    }
                },
            factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                            val surface = Surface(st)
                            viewModel.onSurfaceReady(surface)
                        }

                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean = true
                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                    }
                }
            }
        )

        // 2. Framing Grid Overlay (Thirds / Golden / Cross)
        FramingGridOverlay(gridType = gridType)

        // 3. Electronic Horizon Level (Real-time Gyro Roll)
        ElectronicHorizonOverlay(rollAngle = rollAngle, isLevel = isLevel)

        // 4. Ghost Overlay (Onion Skin) during Dual-Lens Step 2 & 3
        if (activeMode == CameraMode.DUAL_FOCUS) {
            GhostOverlayView(ghostBitmap = ghostOverlayBitmap)
        }

        // 5. Live Histogram in Upper Left
        LiveHistogramView(
            lumaHistogram = liveHistogram,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 80.dp)
        )

        // 6. Tap-To-Focus Animated Ring
        tapRingPosition?.let { pos ->
            val infiniteTransition = rememberInfiniteTransition(label = "FocusRing")
            val ringScale by infiniteTransition.animateFloat(
                initialValue = 1.3f,
                targetValue = 0.9f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "FocusScale"
            )
            Box(
                modifier = Modifier
                    .offset(x = (pos.x.dp / 2.7f) - 30.dp, y = (pos.y.dp / 2.7f) - 30.dp)
                    .size(60.dp * ringScale)
                    .border(1.5.dp, RealmeGold, CircleShape)
            )
        }

        // 7. Top Status Bar & Controls (RAW, ZEBRA, GRID, GUIDE)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hardware Badge
            Column {
                Text(
                    text = "REALME 14 PRO+ 5G",
                    color = RealmeGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "SD 7s Gen 3 | 50MP Periscope OIS",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Quick Toggle Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // RAW Toggle Pill
                QuickTogglePill(
                    text = if (rawEnabled) "RAW+JPG" else "JPG",
                    isActive = rawEnabled,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        viewModel.toggleRawCapture(!rawEnabled)
                    }
                )

                // Grid Toggle Pill
                QuickTogglePill(
                    text = when (gridType) {
                        GridType.RULE_OF_THIRDS -> "GRID 3x3"
                        GridType.GOLDEN_RATIO -> "GOLDEN"
                        GridType.CROSSHAIRS -> "CROSS"
                        GridType.NONE -> "GRID OFF"
                    },
                    isActive = gridType != GridType.NONE,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        val nextGrid = when (gridType) {
                            GridType.RULE_OF_THIRDS -> GridType.GOLDEN_RATIO
                            GridType.GOLDEN_RATIO -> GridType.CROSSHAIRS
                            GridType.CROSSHAIRS -> GridType.NONE
                            GridType.NONE -> GridType.RULE_OF_THIRDS
                        }
                        viewModel.setGridType(nextGrid)
                    }
                )

                // Guide (?) Button - Launches In-App Interactive Hindi/English Guide
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(RealmeGold.copy(alpha = 0.2f))
                        .border(1.dp, RealmeGold, CircleShape)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.openGuide()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.HelpOutline,
                        contentDescription = "Feature Guide & Interactive Demos",
                        tint = RealmeGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 8. Dual-Lens Focus Stacking Guidance Banner
        if (activeMode == CameraMode.DUAL_FOCUS) {
            DualLensProgressIndicator(
                phase = stackerPhase,
                statusMessage = stackerStatus,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 110.dp)
            )
        }

        // Notification Banner
        notification?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OledSurface.copy(alpha = 0.95f))
                    .border(1.dp, FocusGreen, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(text = msg, color = FocusGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 9. Bottom Control Stack
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Physical Lens Quick-Switch Selector Pills
            LensSelectorBar(
                currentLens = currentLens,
                onLensSelected = { lens ->
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    viewModel.switchLens(lens)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Pro Manual Controls (Sliders for ISO, Shutter, Focus, WB, EV)
            if (activeMode == CameraMode.PRO || activeMode == CameraMode.SUPER_MACRO) {
                ManualControlsPanel(
                    iso = iso,
                    isManualExposure = isManualExposure,
                    onIsoChanged = viewModel.cameraController::setIso,
                    onAutoExposureToggle = viewModel.cameraController::resetAutoExposure,
                    shutterSpeedNs = shutterSpeedNs,
                    onShutterChanged = viewModel.cameraController::setShutterSpeed,
                    focusDistance = focusDistance,
                    isManualFocus = isManualFocus,
                    onFocusDistanceChanged = viewModel.cameraController::setFocusDistance,
                    onAutoFocusToggle = viewModel.cameraController::resetAutoFocus,
                    focusPeakingEnabled = focusPeakingEnabled,
                    onToggleFocusPeaking = viewModel.cameraController::toggleFocusPeaking,
                    whiteBalanceKelvin = whiteBalanceKelvin,
                    onWbChanged = viewModel.cameraController::setWhiteBalanceKelvin,
                    evCompensation = evCompensation,
                    onEvChanged = viewModel.cameraController::setEvCompensation
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Camera Mode Selector Carousel
            CameraModeCarousel(
                activeMode = activeMode,
                onModeSelected = { mode ->
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    viewModel.switchMode(mode)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Shutter Row (Gallery Thumb, Shutter Button, Switcher)
            ShutterBar(
                isProcessing = isProcessingCapture,
                activeMode = activeMode,
                stackerPhase = stackerPhase,
                onShutterClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    viewModel.onShutterClicked()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 10. Interactive User Guide Screen Overlay
        if (isGuideOpen) {
            FeatureGuideScreen(
                onDismiss = { viewModel.closeGuide() },
                onLaunchMode = { mode -> viewModel.switchMode(mode) }
            )
        }

        // 11. Post-Capture Mini Darkroom Modal
        darkroomBitmap?.let { bmp ->
            MiniDarkroomSheet(
                capturedBitmap = bmp,
                onDismiss = { viewModel.closeDarkroom() },
                onSaveAdjusted = { adjustedBmp ->
                    viewModel.closeDarkroom()
                }
            )
        }
    }
}

@Composable
private fun QuickTogglePill(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isActive) RealmeGold.copy(alpha = 0.2f) else OledCard)
            .border(1.dp, if (isActive) RealmeGold else OledBorder, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (isActive) RealmeGold else TextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Direct Physical Lens Selector Pills for Realme 14 Pro+
 */
@Composable
private fun LensSelectorBar(
    currentLens: LensType,
    onLensSelected: (LensType) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(OledSurface.copy(alpha = 0.85f))
            .border(1.dp, OledBorder, CircleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LensType.values().forEach { lens ->
            val isSelected = lens == currentLens
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) RealmeGold else Color.Transparent)
                    .clickable { onLensSelected(lens) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = lens.label,
                    color = if (isSelected) Black else TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Camera Mode Selector Carousel
 */
@Composable
private fun CameraModeCarousel(
    activeMode: CameraMode,
    onModeSelected: (CameraMode) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(CameraMode.values()) { mode ->
            val isSelected = mode == activeMode
            Box(
                modifier = Modifier
                    .clickable { onModeSelected(mode) }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.title,
                    color = if (isSelected) RealmeGold else TextMuted,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * Bottom Shutter Button Bar
 */
@Composable
private fun ShutterBar(
    isProcessing: Boolean,
    activeMode: CameraMode,
    stackerPhase: HybridFocusStacker.StackingPhase,
    onShutterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Space / Gallery Icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(OledCard)
                .border(1.dp, OledBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = TextSecondary, modifier = Modifier.size(24.dp))
        }

        // Master Shutter Button
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(CircleShape)
                .background(Color.Transparent)
                .border(3.dp, if (activeMode == CameraMode.DUAL_FOCUS) AccentCyan else RealmeGold, CircleShape)
                .clickable(enabled = !isProcessing, onClick = onShutterClick)
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isProcessing) {
                CircularProgressIndicator(color = RealmeGold, strokeWidth = 3.dp, modifier = Modifier.fillMaxSize())
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(if (activeMode == CameraMode.DUAL_FOCUS) AccentCyan else Color.White)
                )
            }
        }

        // Right Space / Lens Information Badge
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(OledCard)
                .border(1.dp, OledBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Switch Camera", tint = TextSecondary, modifier = Modifier.size(24.dp))
        }
    }
}
