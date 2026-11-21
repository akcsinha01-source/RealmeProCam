package com.realme.procam.ui.viewmodel

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.realme.procam.camera.Camera2Controller
import com.realme.procam.camera.RealmeCameraConstants
import com.realme.procam.camera.RealmeCameraConstants.CameraMode
import com.realme.procam.camera.RealmeCameraConstants.LensType
import com.realme.procam.camera.SensorOrientationHelper
import com.realme.procam.processing.ComputationalPhotographyEngine
import com.realme.procam.processing.ComputationalPhotographyEngine.LongExposureMode
import com.realme.procam.processing.HybridFocusStacker
import com.realme.procam.ui.components.GridType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    val cameraController = Camera2Controller(application, viewModelScope)
    val focusStacker = HybridFocusStacker(application, cameraController, viewModelScope)
    val sensorHelper = SensorOrientationHelper(application)

    // UI State Flows
    private val _activeMode = MutableStateFlow(CameraMode.DUAL_FOCUS)
    val activeMode: StateFlow<CameraMode> = _activeMode.asStateFlow()

    private val _gridType = MutableStateFlow(GridType.RULE_OF_THIRDS)
    val gridType: StateFlow<GridType> = _gridType.asStateFlow()

    private val _rawCaptureEnabled = MutableStateFlow(false)
    val rawCaptureEnabled: StateFlow<Boolean> = _rawCaptureEnabled.asStateFlow()

    private val _zebraStripesEnabled = MutableStateFlow(false)
    val zebraStripesEnabled: StateFlow<Boolean> = _zebraStripesEnabled.asStateFlow()

    private val _isGuideOpen = MutableStateFlow(false)
    val isGuideOpen: StateFlow<Boolean> = _isGuideOpen.asStateFlow()

    private val _darkroomBitmap = MutableStateFlow<Bitmap?>(null)
    val darkroomBitmap: StateFlow<Bitmap?> = _darkroomBitmap.asStateFlow()

    private val _liveHistogram = MutableStateFlow<IntArray?>(null)
    val liveHistogram: StateFlow<IntArray?> = _liveHistogram.asStateFlow()

    private val _lastCapturedUri = MutableStateFlow<Uri?>(null)
    val lastCapturedUri: StateFlow<Uri?> = _lastCapturedUri.asStateFlow()

    private val _capturedNotification = MutableStateFlow<String?>(null)
    val capturedNotification: StateFlow<String?> = _capturedNotification.asStateFlow()

    init {
        sensorHelper.startListening()

        // Hook analysis frame listener for live histogram computation
        cameraController.onAnalysisFrameAvailable = { bmp ->
            val luma = IntArray(256)
            val r = IntArray(256)
            val g = IntArray(256)
            val b = IntArray(256)
            try {
                ComputationalPhotographyEngine.nativeComputeHistogram(bmp, luma, r, g, b)
                _liveHistogram.value = luma
            } catch (e: Throwable) {
                // Ignore fallback
            }
        }
    }

    val cameraErrorMessage: StateFlow<String?> = cameraController.cameraErrorMessage
    val isFrontCamera: StateFlow<Boolean> = cameraController.isFrontCamera

    fun toggleCameraFacing() {
        cameraController.toggleCameraFacing()
    }

    fun onSurfaceReady(surface: Surface) {
        cameraController.openCamera(surface)
        // Default to Dual Focus mode setup
        if (_activeMode.value == CameraMode.DUAL_FOCUS) {
            focusStacker.startDualLensSequence()
        }
    }

    fun pauseCamera() {
        cameraController.pauseCamera()
    }

    fun resumeCamera() {
        cameraController.resumeCamera()
    }

    fun resetCameraHardware() {
        cameraController.resetCameraHardware()
    }

    fun closeCamera() {
        cameraController.close()
    }

    fun switchMode(mode: CameraMode) {
        _activeMode.value = mode
        when (mode) {
            CameraMode.DUAL_FOCUS -> {
                focusStacker.startDualLensSequence()
            }
            CameraMode.PRO -> {
                focusStacker.reset()
                cameraController.switchLens(LensType.MAIN_WIDE)
            }
            CameraMode.SUPER_MACRO -> {
                focusStacker.reset()
                // Super Macro locks Periscope at closest optical focus
                cameraController.switchLens(LensType.PERISCOPE)
                cameraController.setFocusDistance(RealmeCameraConstants.FOCUS_MACRO_NEAR)
                cameraController.toggleFocusPeaking(true)
            }
            CameraMode.ASTRO -> {
                focusStacker.reset()
                cameraController.switchLens(LensType.MAIN_WIDE)
                cameraController.setIso(1600)
                cameraController.setShutterSpeed(15_000_000_000L) // 15 seconds
                cameraController.setFocusDistance(RealmeCameraConstants.FOCUS_INFINITY)
            }
            CameraMode.LONG_EXPOSURE -> {
                focusStacker.reset()
                cameraController.switchLens(LensType.MAIN_WIDE)
                cameraController.setIso(100)
                cameraController.setShutterSpeed(2_000_000_000L) // 2 seconds
            }
            else -> {
                focusStacker.reset()
                cameraController.resetAutoExposure()
                cameraController.resetAutoFocus()
                cameraController.toggleFocusPeaking(false)
            }
        }
    }

    val zoomRatio: StateFlow<Float> = cameraController.zoomRatio

    fun switchLens(lens: LensType) {
        cameraController.switchLens(lens)
    }

    fun setZoomRatio(ratio: Float) {
        cameraController.setZoomRatio(ratio)
    }

    fun setGridType(grid: GridType) {
        _gridType.value = grid
    }

    fun toggleRawCapture(enabled: Boolean) {
        _rawCaptureEnabled.value = enabled
    }

    fun toggleZebraStripes(enabled: Boolean) {
        _zebraStripesEnabled.value = enabled
    }

    fun openGuide() {
        _isGuideOpen.value = true
    }

    fun closeGuide() {
        _isGuideOpen.value = false
    }

    fun openDarkroom(bitmap: Bitmap) {
        _darkroomBitmap.value = bitmap
    }

    fun closeDarkroom() {
        _darkroomBitmap.value = null
    }

    fun clearNotification() {
        _capturedNotification.value = null
    }

    /**
     * Master Shutter Trigger:
     * Dispatches action according to active mode (Dual-Lens Stacking sequence, RAW, HDR+, or Pro Still)
     */
    fun onShutterClicked() {
        when (_activeMode.value) {
            CameraMode.DUAL_FOCUS -> {
                when (focusStacker.phase.value) {
                    HybridFocusStacker.StackingPhase.IDLE,
                    HybridFocusStacker.StackingPhase.STEP1_NEAR_PROMPT,
                    HybridFocusStacker.StackingPhase.COMPLETED -> {
                        focusStacker.startDualLensSequence()
                        focusStacker.captureStep1Near { uri ->
                            _lastCapturedUri.value = uri
                            _capturedNotification.value = "100% डीप फोकस फोटो तैयार!"
                        }
                    }
                    else -> {
                        focusStacker.startDualLensSequence()
                        focusStacker.captureStep1Near { uri ->
                            _lastCapturedUri.value = uri
                            _capturedNotification.value = "100% डीप फोकस फोटो तैयार!"
                        }
                    }
                }
            }
            CameraMode.HDR_PLUS -> {
                // Multi-exposure bracketed capture (-2 EV, 0 EV, +2 EV)
                captureHdrBracketSequence()
            }
            CameraMode.LONG_EXPOSURE -> {
                // Accumulate burst frames
                captureLongExposureSequence()
            }
            CameraMode.SCANNER -> {
                cameraController.captureStillBitmap { rawBmp ->
                    viewModelScope.launch(Dispatchers.Default) {
                        val docBmp = ComputationalPhotographyEngine.enhanceDocument(rawBmp)
                        _darkroomBitmap.value = docBmp
                    }
                }
            }
            else -> {
                // Standard PHOTO or PRO Manual Capture: Save directly and keep shutter ready
                cameraController.captureStillBitmap { bmp ->
                    viewModelScope.launch(Dispatchers.IO) {
                        val savedUri = saveBitmapToMediaStore(bmp, "REALME_14PRO_PHOTO")
                        withContext(Dispatchers.Main) {
                            _lastCapturedUri.value = savedUri
                            _capturedNotification.value = "फोटो गैलरी में सुरक्षित!"
                        }
                    }
                    if (_rawCaptureEnabled.value) {
                        val dngFile = File(getApplication<Application>().cacheDir, "RAW_${System.currentTimeMillis()}.dng")
                        cameraController.captureRawDng(dngFile) {
                            _capturedNotification.value = "JPEG + 12-bit RAW DNG Saved"
                        }
                    }
                }
            }
        }
    }

    private fun saveBitmapToMediaStore(bitmap: Bitmap, prefix: String): Uri? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "${prefix}_${timestamp}.jpg"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/ProCam")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = getApplication<Application>().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            getApplication<Application>().contentResolver.openOutputStream(it)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 98, stream)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                getApplication<Application>().contentResolver.update(it, values, null, null)
            }
        }
        return uri
    }

    private fun captureHdrBracketSequence() {
        val frames = mutableListOf<Bitmap>()
        // 1. Underexposed -2 EV
        cameraController.setEvCompensation(-2.0f)
        cameraController.captureStillBitmap { f1 ->
            frames.add(f1)
            // 2. Normal 0 EV
            cameraController.setEvCompensation(0.0f)
            cameraController.captureStillBitmap { f2 ->
                frames.add(f2)
                // 3. Overexposed +2 EV
                cameraController.setEvCompensation(+2.0f)
                cameraController.captureStillBitmap { f3 ->
                    frames.add(f3)
                    cameraController.setEvCompensation(0.0f) // reset
                    viewModelScope.launch {
                        val hdrMerged = ComputationalPhotographyEngine.mergeBracketedHdr(frames[0], frames[1], frames[2])
                        _darkroomBitmap.value = hdrMerged
                    }
                }
            }
        }
    }

    private fun captureLongExposureSequence() {
        val frames = mutableListOf<Bitmap>()
        var count = 0
        fun captureNext() {
            cameraController.captureStillBitmap { bmp ->
                frames.add(bmp)
                count++
                if (count < 5) {
                    captureNext()
                } else {
                    viewModelScope.launch {
                        val result = ComputationalPhotographyEngine.processLongExposure(frames, LongExposureMode.LIGHT_TRAILS)
                        _darkroomBitmap.value = result
                    }
                }
            }
        }
        captureNext()
    }

    override fun onCleared() {
        super.onCleared()
        sensorHelper.stopListening()
        cameraController.close()
    }
}
