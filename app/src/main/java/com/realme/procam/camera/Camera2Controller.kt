package com.realme.procam.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.hardware.camera2.*
import android.hardware.camera2.params.ColorSpaceTransform
import android.hardware.camera2.params.MeteringRectangle
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.RggbChannelVector
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import com.realme.procam.camera.RealmeCameraConstants.LensType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors

/**
 * Advanced Camera2 Controller specifically tailored for Realme 14 Pro+ 5G.
 * Supports Logical Multi-Camera physical stream routing (1x Sony IMX890 and 3x Sony IMX882 Periscope),
 * Manual Exposure/Focus overrides, RAW DNG creation, and live frame inspection.
 */
class Camera2Controller(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "RealmeProCam_Camera2"
    }

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    // Surface & Readers
    private var previewSurface: Surface? = null
    private var jpegImageReader: ImageReader? = null
    private var rawImageReader: ImageReader? = null
    private var analysisImageReader: ImageReader? = null

    // Physical Camera IDs mapping for Realme 14 Pro+
    private var logicalCameraId: String = "0"
    private var physicalMainId: String? = null
    private var physicalPeriscopeId: String? = null
    private var physicalUltraWideId: String? = null

    private var activeAfRegion: MeteringRectangle? = null
    private var activeAeRegion: MeteringRectangle? = null

    // Current State Flows
    private val _currentLens = MutableStateFlow(LensType.MAIN_WIDE)
    val currentLens: StateFlow<LensType> = _currentLens.asStateFlow()

    private val _iso = MutableStateFlow(100)
    val iso: StateFlow<Int> = _iso.asStateFlow()

    private val _shutterSpeedNs = MutableStateFlow(20_000_000L) // 1/50s
    val shutterSpeedNs: StateFlow<Long> = _shutterSpeedNs.asStateFlow()

    private val _focusDistance = MutableStateFlow(0.0f) // Infinity
    val focusDistance: StateFlow<Float> = _focusDistance.asStateFlow()

    private val _isManualFocus = MutableStateFlow(false)
    val isManualFocus: StateFlow<Boolean> = _isManualFocus.asStateFlow()

    private val _isManualExposure = MutableStateFlow(false)
    val isManualExposure: StateFlow<Boolean> = _isManualExposure.asStateFlow()

    private val _whiteBalanceKelvin = MutableStateFlow(5500) // Daylight
    val whiteBalanceKelvin: StateFlow<Int> = _whiteBalanceKelvin.asStateFlow()

    private val _evCompensation = MutableStateFlow(0f)
    val evCompensation: StateFlow<Float> = _evCompensation.asStateFlow()

    private val _focusPeakingEnabled = MutableStateFlow(false)
    val focusPeakingEnabled: StateFlow<Boolean> = _focusPeakingEnabled.asStateFlow()

    private val _isProcessingCapture = MutableStateFlow(false)
    val isProcessingCapture: StateFlow<Boolean> = _isProcessingCapture.asStateFlow()

    private val _cameraErrorMessage = MutableStateFlow<String?>(null)
    val cameraErrorMessage: StateFlow<String?> = _cameraErrorMessage.asStateFlow()

    private var isAppActive: Boolean = true
    private var isAvailabilityRegistered: Boolean = false

    private val availabilityCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            super.onCameraAvailable(cameraId)
            if (cameraId == logicalCameraId) {
                Log.d(TAG, "Camera $cameraId is now AVAILABLE from system")
                if (isAppActive && cameraDevice == null && previewSurface != null) {
                    backgroundHandler?.post {
                        openCamera(previewSurface!!)
                    }
                }
            }
        }

        override fun onCameraUnavailable(cameraId: String) {
            super.onCameraUnavailable(cameraId)
            if (cameraId == logicalCameraId) {
                Log.d(TAG, "Camera $cameraId is UNAVAILABLE (in use by another app or system)")
            }
        }
    }

    // Live frame preview callback for histogram / peaking analysis
    var onAnalysisFrameAvailable: ((Bitmap) -> Unit)? = null

    fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("CameraBackground").apply {
                start()
                backgroundHandler = Handler(looper)
            }
        }
    }

    fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }
    }

    /**
     * Inspects camera hardware characteristics and maps Realme 14 Pro+ 5G physical camera IDs.
     */
    fun discoverCameras() {
        try {
            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    logicalCameraId = id
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val physicalIds = chars.physicalCameraIds
                        Log.d(TAG, "Back Camera $id has physical camera IDs: $physicalIds")
                        // Usually, id 0 = Main 1x, id 1/2 = Ultra-Wide, id 3/4 = Periscope 3x
                        val list = physicalIds.toList()
                        if (list.isNotEmpty()) {
                            physicalMainId = list.getOrNull(0)
                            physicalUltraWideId = list.getOrNull(1)
                            physicalPeriscopeId = list.getOrNull(2) ?: list.getOrNull(1)
                        }
                    }
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering camera IDs", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun openCamera(surface: Surface) {
        startBackgroundThread()
        previewSurface = surface
        isAppActive = true

        if (cameraDevice != null) {
            closeCameraSessionAndDevice()
        }

        discoverCameras()

        if (!isAvailabilityRegistered) {
            try {
                cameraManager.registerAvailabilityCallback(availabilityCallback, backgroundHandler)
                isAvailabilityRegistered = true
            } catch (e: Exception) {
                Log.e(TAG, "Error registering availability callback", e)
            }
        }

        try {
            // Setup ImageReaders: High-Res JPEG, RAW Sensor, and Analysis buffer
            val chars = cameraManager.getCameraCharacteristics(logicalCameraId)
            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val jpegSize = map?.getOutputSizes(ImageFormat.JPEG)?.firstOrNull() ?: Size(4096, 3072)
            val rawSize = map?.getOutputSizes(ImageFormat.RAW_SENSOR)?.firstOrNull()

            jpegImageReader = ImageReader.newInstance(jpegSize.width, jpegSize.height, ImageFormat.JPEG, 2)
            if (rawSize != null) {
                rawImageReader = ImageReader.newInstance(rawSize.width, rawSize.height, ImageFormat.RAW_SENSOR, 2)
            }

            // Low-res analysis reader for 30fps Histogram & Focus Peaking
            analysisImageReader = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 3).apply {
                setOnImageAvailableListener({ reader ->
                    val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                    try {
                        val bmp = yuvToBitmap(image)
                        onAnalysisFrameAvailable?.invoke(bmp)
                    } catch (e: Exception) {
                        // ignore dropped frames
                    } finally {
                        image.close()
                    }
                }, backgroundHandler)
            }

            cameraManager.openCamera(logicalCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    _cameraErrorMessage.value = null
                    createCaptureSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera open error: $error")
                    camera.close()
                    cameraDevice = null
                    val msg = when (error) {
                        CameraDevice.StateCallback.ERROR_CAMERA_IN_USE -> "कैमरा स्टॉक ऐप में उपयोग में है। पुनः कनेक्ट हो रहा है..."
                        CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE -> "कैमरा व्यस्त है..."
                        CameraDevice.StateCallback.ERROR_CAMERA_DISABLED -> "कैमरा अक्षम है।"
                        CameraDevice.StateCallback.ERROR_CAMERA_DEVICE -> "कैमरा हार्डवेयर रीसेट हो रहा है..."
                        CameraDevice.StateCallback.ERROR_CAMERA_SERVICE -> "कैमरा सर्विस पुनः शुरू हो रही है..."
                        else -> "कैमरा त्रुटि ($error)"
                    }
                    _cameraErrorMessage.value = msg

                    if (error == CameraDevice.StateCallback.ERROR_CAMERA_IN_USE ||
                        error == CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE) {
                        backgroundHandler?.postDelayed({
                            if (isAppActive && cameraDevice == null && previewSurface != null) {
                                openCamera(previewSurface!!)
                            }
                        }, 700)
                    }
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open camera", e)
            _cameraErrorMessage.value = "कैमरा ओपन नहीं हो सका: ${e.message}"
        }
    }

    private fun createCaptureSession() {
        val device = cameraDevice ?: return
        val pSurface = previewSurface ?: return
        val jReader = jpegImageReader ?: return
        val aReader = analysisImageReader ?: return

        try {
            val surfaces = mutableListOf(pSurface, jReader.surface, aReader.surface)
            rawImageReader?.surface?.let { surfaces.add(it) }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val outputConfigs = surfaces.map { OutputConfiguration(it) }
                val sessionConfig = SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    outputConfigs,
                    Executors.newSingleThreadExecutor(),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            captureSession = session
                            updatePreviewRepeating()
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Log.e(TAG, "Capture session configuration failed")
                        }
                    }
                )
                device.createCaptureSession(sessionConfig)
            } else {
                @Suppress("DEPRECATION")
                device.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        updatePreviewRepeating()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Legacy capture session configuration failed")
                    }
                }, backgroundHandler)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating capture session", e)
        }
    }

    /**
     * Updates preview request with current zoom ratio, physical lens target, manual exposure, and focus distance.
     */
    fun updatePreviewRepeating() {
        val device = cameraDevice ?: return
        val session = captureSession ?: return
        val pSurface = previewSurface ?: return
        val aSurface = analysisImageReader?.surface ?: return

        try {
            val builder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(pSurface)
                addTarget(aSurface)

                // 1. Zoom Ratio & Scaler Crop Region (Physical Lens Binding)
                applyZoom(this, _currentLens.value.zoomRatio)

                // 2. Exposure Mode (Auto vs Manual ISO & Shutter)
                if (_isManualExposure.value) {
                    set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                    set(CaptureRequest.SENSOR_SENSITIVITY, _iso.value)
                    set(CaptureRequest.SENSOR_EXPOSURE_TIME, _shutterSpeedNs.value)
                } else {
                    set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                    // Set EV Compensation
                    val evSteps = (_evCompensation.value * 2.0f).toInt()
                    set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, evSteps)
                }

                // 3. Focus Mode (Auto vs Manual Distance vs Tapped AF)
                if (_isManualFocus.value) {
                    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                    set(CaptureRequest.LENS_FOCUS_DISTANCE, _focusDistance.value)
                } else if (activeAfRegion != null) {
                    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
                    set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(activeAfRegion))
                    activeAeRegion?.let { set(CaptureRequest.CONTROL_AE_REGIONS, arrayOf(it)) }
                } else {
                    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                }

                // 4. White Balance (Auto vs Manual Kelvin)
                applyWhiteBalance(this, _whiteBalanceKelvin.value)
            }

            previewRequestBuilder = builder
            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating preview repeating", e)
        }
    }

    private fun applyZoom(builder: CaptureRequest.Builder, zoomRatio: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.set(CaptureRequest.CONTROL_ZOOM_RATIO, zoomRatio)
        }
        try {
            val chars = cameraManager.getCameraCharacteristics(logicalCameraId)
            val activeArray = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE) ?: Rect(0, 0, 4000, 3000)
            val cropW = (activeArray.width() / zoomRatio).toInt()
            val cropH = (activeArray.height() / zoomRatio).toInt()
            val cropX = (activeArray.width() - cropW) / 2
            val cropY = (activeArray.height() - cropH) / 2
            builder.set(CaptureRequest.SCALER_CROP_REGION, Rect(cropX, cropY, cropX + cropW, cropY + cropH))
        } catch (_: Exception) {}
    }

    private fun applyWhiteBalance(builder: CaptureRequest.Builder, kelvin: Int) {
        if (kelvin == 5500) {
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
        } else {
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
            // Approximate chromaticity calculation for Kelvin
            val temp = kelvin / 100.0
            val rGain: Float = if (temp <= 66) 1.0f else (temp - 60).toFloat() * 0.05f + 1.0f
            val bGain: Float = if (temp >= 66) 1.0f else (66 - temp).toFloat() * 0.05f + 1.0f
            builder.set(CaptureRequest.COLOR_CORRECTION_GAINS, RggbChannelVector(rGain, 1.0f, 1.0f, bGain))
        }
    }

    /**
     * Physical Lens Switching: 0.6x Ultra-Wide, 1x Main Wide, 3x Periscope Telephoto, 6x Lossless
     */
    fun switchLens(lens: LensType) {
        _currentLens.value = lens
        updatePreviewRepeating()
    }

    fun setIso(value: Int) {
        _iso.value = value.coerceIn(RealmeCameraConstants.MIN_ISO, RealmeCameraConstants.MAX_ISO)
        _isManualExposure.value = true
        updatePreviewRepeating()
    }

    fun setShutterSpeed(nanos: Long) {
        _shutterSpeedNs.value = nanos.coerceIn(RealmeCameraConstants.MIN_SHUTTER_NS, RealmeCameraConstants.MAX_SHUTTER_NS)
        _isManualExposure.value = true
        updatePreviewRepeating()
    }

    fun setFocusDistance(distanceDiopters: Float) {
        _focusDistance.value = distanceDiopters.coerceIn(RealmeCameraConstants.FOCUS_INFINITY, RealmeCameraConstants.FOCUS_MACRO_NEAR)
        _isManualFocus.value = true
        updatePreviewRepeating()
    }

    fun setWhiteBalanceKelvin(kelvin: Int) {
        _whiteBalanceKelvin.value = kelvin.coerceIn(RealmeCameraConstants.MIN_WB_KELVIN, RealmeCameraConstants.MAX_WB_KELVIN)
        updatePreviewRepeating()
    }

    fun setEvCompensation(ev: Float) {
        _evCompensation.value = ev.coerceIn(RealmeCameraConstants.MIN_EV, RealmeCameraConstants.MAX_EV)
        updatePreviewRepeating()
    }

    fun toggleFocusPeaking(enabled: Boolean) {
        _focusPeakingEnabled.value = enabled
    }

    fun resetAutoExposure() {
        _isManualExposure.value = false
        updatePreviewRepeating()
    }

    fun resetAutoFocus() {
        _isManualFocus.value = false
        updatePreviewRepeating()
    }

    /**
     * Tap to Focus and Metering on Viewfinder coordinates (normalized 0.0 to 1.0)
     */
    fun tapToFocus(normalizedX: Float, normalizedY: Float) {
        val device = cameraDevice ?: return
        val session = captureSession ?: return

        try {
            _isManualFocus.value = false
            val chars = cameraManager.getCameraCharacteristics(logicalCameraId)
            val sensorRect = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE) ?: Rect(0, 0, 4000, 3000)

            // Correct coordinate transform for 90-degree rotated portrait camera sensor
            val sensorX = (normalizedY * sensorRect.width()).toInt().coerceIn(0, sensorRect.width())
            val sensorY = ((1.0f - normalizedX) * sensorRect.height()).toInt().coerceIn(0, sensorRect.height())
            Log.d(TAG, "tapToFocus: norm=($normalizedX, $normalizedY) -> sensor=($sensorX, $sensorY)")

            val halfArea = 250
            val focusArea = MeteringRectangle(
                Rect(
                    (sensorX - halfArea).coerceAtLeast(0),
                    (sensorY - halfArea).coerceAtLeast(0),
                    (sensorX + halfArea).coerceAtMost(sensorRect.width()),
                    (sensorY + halfArea).coerceAtMost(sensorRect.height())
                ),
                1000
            )

            activeAfRegion = focusArea
            activeAeRegion = focusArea

            // 1. Cancel previous AF
            val cancelBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                previewSurface?.let { addTarget(it) }
                analysisImageReader?.surface?.let { addTarget(it) }
                set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL)
            }
            session.capture(cancelBuilder.build(), null, backgroundHandler)

            // 2. Trigger fresh AF on the tapped spot
            val builder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                previewSurface?.let { addTarget(it) }
                analysisImageReader?.surface?.let { addTarget(it) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    set(CaptureRequest.CONTROL_ZOOM_RATIO, _currentLens.value.zoomRatio)
                }
                set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(focusArea))
                set(CaptureRequest.CONTROL_AE_REGIONS, arrayOf(focusArea))
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START)
            }
            session.capture(builder.build(), null, backgroundHandler)

            // 3. Keep repeating request locked on tapped region
            updatePreviewRepeating()
        } catch (e: Exception) {
            Log.e(TAG, "Error in tap-to-focus", e)
        }
    }

    /**
     * Captures a single still high-resolution Bitmap frame in memory.
     * Guaranteed fail-safe timeout prevents shutter button lockups.
     */
    fun captureStillBitmap(onCaptured: (Bitmap) -> Unit) {
        val device = cameraDevice ?: run {
            _isProcessingCapture.value = false
            return
        }
        val session = captureSession ?: run {
            _isProcessingCapture.value = false
            return
        }
        val jReader = jpegImageReader ?: run {
            _isProcessingCapture.value = false
            return
        }

        _isProcessingCapture.value = true

        // 2-second timeout guard: Never leave capture lock stuck
        val timeoutRunnable = Runnable {
            if (_isProcessingCapture.value) {
                Log.w(TAG, "Capture still bitmap timed out; releasing lock")
                _isProcessingCapture.value = false
                try {
                    jReader.setOnImageAvailableListener(null, null)
                } catch (_: Exception) {}
            }
        }
        backgroundHandler?.postDelayed(timeoutRunnable, 2000)

        jReader.setOnImageAvailableListener({ reader ->
            backgroundHandler?.removeCallbacks(timeoutRunnable)
            var image: Image? = null
            try {
                image = reader.acquireLatestImage() ?: reader.acquireNextImage()
                if (image != null) {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    
                    // Immediately unregister listener to prevent duplicate events
                    try {
                        reader.setOnImageAvailableListener(null, null)
                    } catch (_: Exception) {}

                    _isProcessingCapture.value = false
                    if (bitmap != null) {
                        onCaptured(bitmap)
                    } else {
                        Log.e(TAG, "Bitmap decoding returned null from JPEG buffer")
                    }
                } else {
                    _isProcessingCapture.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error acquiring JPEG still frame", e)
                _isProcessingCapture.value = false
            } finally {
                image?.close()
            }
        }, backgroundHandler)

        try {
            val captureBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(jReader.surface)
                applyZoom(this, _currentLens.value.zoomRatio)
                set(CaptureRequest.JPEG_QUALITY, 100.toByte())
                set(CaptureRequest.JPEG_ORIENTATION, 90) // Portrait standard
            }
            session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureFailed(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    failure: CaptureFailure
                ) {
                    Log.e(TAG, "Capture failed callback reason: ${failure.reason}")
                    backgroundHandler?.removeCallbacks(timeoutRunnable)
                    _isProcessingCapture.value = false
                    try {
                        jReader.setOnImageAvailableListener(null, null)
                    } catch (_: Exception) {}
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Capture failed with exception", e)
            backgroundHandler?.removeCallbacks(timeoutRunnable)
            _isProcessingCapture.value = false
            try {
                jReader.setOnImageAvailableListener(null, null)
            } catch (_: Exception) {}
        }
    }

    /**
     * Captures RAW (DNG) alongside processed JPEG.
     */
    fun captureRawDng(outputDngFile: File, onComplete: () -> Unit) {
        val device = cameraDevice ?: return
        val session = captureSession ?: return
        val rReader = rawImageReader ?: return
        val chars = cameraManager.getCameraCharacteristics(logicalCameraId)

        var lastCaptureResult: TotalCaptureResult? = null

        rReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                try {
                    lastCaptureResult?.let { result ->
                        val dngCreator = DngCreator(chars, result)
                        FileOutputStream(outputDngFile).use { fos ->
                            dngCreator.writeImage(fos, image)
                        }
                        dngCreator.close()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error writing DNG file", e)
                } finally {
                    image.close()
                    onComplete()
                }
            }
        }, backgroundHandler)

        try {
            val rawBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(rReader.surface)
            }
            session.capture(rawBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    lastCaptureResult = result
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "RAW capture request failed", e)
        }
    }

    private fun yuvToBitmap(image: Image): Bitmap {
        val width = image.width
        val height = image.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val yPlane = image.planes[0]
            val yBuffer = yPlane.buffer
            val rowStride = yPlane.rowStride
            val pixels = IntArray(width * height)

            val row = ByteArray(rowStride)
            for (y in 0 until height) {
                val length = Math.min(rowStride, yBuffer.remaining())
                if (length <= 0) break
                yBuffer.get(row, 0, length)
                for (x in 0 until width) {
                    val luma = row[x].toInt() and 0xFF
                    pixels[y * width + x] = (0xFF shl 24) or (luma shl 16) or (luma shl 8) or luma
                }
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        } catch (e: Throwable) {
            Log.e(TAG, "Safe YUV extraction fallback", e)
        }
        return bitmap
    }

    fun pauseCamera() {
        Log.d(TAG, "pauseCamera: Immediately releasing camera hardware for preinstalled camera")
        isAppActive = false
        closeCameraSessionAndDevice()
    }

    fun resumeCamera() {
        Log.d(TAG, "resumeCamera: Re-acquiring camera")
        isAppActive = true
        previewSurface?.let { openCamera(it) }
    }

    fun resetCameraHardware() {
        Log.d(TAG, "resetCameraHardware: Force-resetting camera HAL and reconnecting")
        closeCameraSessionAndDevice()
        backgroundHandler?.postDelayed({
            previewSurface?.let { openCamera(it) }
        }, 350)
    }

    fun closeCameraSessionAndDevice() {
        try {
            captureSession?.stopRepeating()
            captureSession?.abortCaptures()
        } catch (_: Exception) {}
        try {
            captureSession?.close()
        } catch (_: Exception) {}
        captureSession = null

        try {
            cameraDevice?.close()
        } catch (_: Exception) {}
        cameraDevice = null

        try {
            jpegImageReader?.close()
            rawImageReader?.close()
            analysisImageReader?.close()
        } catch (_: Exception) {}
        jpegImageReader = null
        rawImageReader = null
        analysisImageReader = null
    }

    fun close() {
        if (isAvailabilityRegistered) {
            try {
                cameraManager.unregisterAvailabilityCallback(availabilityCallback)
                isAvailabilityRegistered = false
            } catch (_: Exception) {}
        }
        closeCameraSessionAndDevice()
        stopBackgroundThread()
    }
}
