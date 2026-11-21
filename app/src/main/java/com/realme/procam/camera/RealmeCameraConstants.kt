package com.realme.procam.camera

/**
 * Hardware specifications & camera mapping specifically calibrated for
 * Realme 14 Pro+ 5G (Snapdragon 7s Gen 3 Mobile Platform).
 */
object RealmeCameraConstants {
    const val DEVICE_NAME = "Realme 14 Pro+ 5G"
    const val SOC_NAME = "Qualcomm Snapdragon 7s Gen 3"

    // Lens Optical Multipliers
    const val ZOOM_0_6X = 0.6f // Ultra-Wide
    const val ZOOM_1X = 1.0f   // Main Wide (Sony IMX890 / LYT-701)
    const val ZOOM_3X = 3.04f  // Periscope Telephoto (Sony IMX882 73mm eq)
    const val ZOOM_6X = 6.08f  // In-Sensor Lossless Crop (146mm eq)

    // Manual Pro Control Boundaries
    const val MIN_ISO = 50
    const val MAX_ISO = 12800

    // Shutter speed in nanoseconds (1/8000s to 32s)
    const val MIN_SHUTTER_NS = 125_000L          // 1/8000s
    const val MAX_SHUTTER_NS = 32_000_000_000L   // 32 seconds

    // Focus distance in diopters (1/meter)
    const val FOCUS_INFINITY = 0.0f
    const val FOCUS_MACRO_NEAR = 10.0f // ~10cm

    // White Balance Kelvin Range
    const val MIN_WB_KELVIN = 2000
    const val MAX_WB_KELVIN = 8000

    // Exposure Compensation
    const val MIN_EV = -3.0f
    const val MAX_EV = +3.0f

    enum class LensType(val label: String, val zoomRatio: Float, val focalLengthMm: String, val sensorName: String) {
        ULTRA_WIDE("0.6x", ZOOM_0_6X, "16mm eq ƒ/2.2", "8MP Ultra-Wide"),
        MAIN_WIDE("1x", ZOOM_1X, "24mm eq ƒ/1.88 OIS", "Sony 50MP Main"),
        PERISCOPE("3x", ZOOM_3X, "73mm eq ƒ/2.65 OIS", "Sony IMX882 50MP Periscope"),
        IN_SENSOR("6x", ZOOM_6X, "146mm eq Lossless", "50MP In-Sensor Optical Crop")
    }

    enum class CameraMode(val title: String, val hindiTitle: String, val description: String) {
        PHOTO("PHOTO", "फोटो", "Standard AI Smart Capture"),
        PRO("PRO MANUAL", "प्रो मैनुअल", "Full Shutter, ISO, WB, Manual Focus & Peaking"),
        DUAL_FOCUS("DUAL-LENS STACK", "हाइब्रिड फोकस स्टैक", "1x Foreground + 3x Periscope Background Fusion"),
        ASTRO("ASTRO SKY", "एस्ट्रो नाइट", "Multi-frame Star Alignment & Denoise"),
        LONG_EXPOSURE("LIGHT TRAILS", "लॉन्ग एक्सपोज़र", "Tripod-free Vehicle Lights & Water Smoothing"),
        HDR_PLUS("HDR+ RAW", "एचडीआर+ रॉ", "Multi-bracketed Exposure Fusion with DNG"),
        SUPER_MACRO("SUPER MACRO", "सुपर मैक्रो", "Periscope Telephoto Close-up with Focus Peaking"),
        SCANNER("DOC SCANNER", "डॉक्यूमेंट स्कैनर", "Perspective Rectification & Text Contrast Boost")
    }
}
