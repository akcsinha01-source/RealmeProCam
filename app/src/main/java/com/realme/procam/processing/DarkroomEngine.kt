package com.realme.procam.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Built-in Post-Capture Darkroom Engine:
 * - 3D LUT emulation (Cinematic Teal-Orange, Leica Classic B&W, Film Portra, Vivid Landscape)
 * - Micro-contrast, clarity, highlights and shadow adjustments
 * - Elegant Leica/Hasselblad style bottom EXIF metadata watermark
 */
object DarkroomEngine {

    enum class PresetLut(val displayName: String) {
        NATURAL("Natural Neutral"),
        CINEMATIC_TEAL_ORANGE("Cinematic Teal & Gold"),
        LEICA_MONOCHROME("Leica Classic B&W"),
        FILM_PORTRA("Film Portra 400"),
        VIBRANT_LANDSCAPE("Vivid Landscape")
    }

    suspend fun applyAdjustments(
        source: Bitmap,
        lut: PresetLut,
        clarity: Float,      // -100 to +100
        shadows: Float,      // 0 to 100
        highlights: Float,   // -100 to 0
        addWatermark: Boolean,
        lensInfo: String = "50MP Periscope OIS | 73mm eq ƒ/2.65"
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val clarityFactor = (clarity / 100f) * 0.4f
        val shadowLift = (shadows / 100f) * 40f
        val highlightCompress = (highlights / 100f) * 40f

        for (i in pixels.indices) {
            val p = pixels[i]
            var r = (p shr 16) and 0xFF
            var g = (p shr 8) and 0xFF
            var b = p and 0xFF

            // Shadows / Highlights adjustment
            val luma = 0.299f * r + 0.587f * g + 0.114f * b
            if (luma < 128f) {
                val factor = (128f - luma) / 128f
                r = (r + shadowLift * factor).toInt()
                g = (g + shadowLift * factor).toInt()
                b = (b + shadowLift * factor).toInt()
            } else {
                val factor = (luma - 128f) / 128f
                r = (r + highlightCompress * factor).toInt()
                g = (g + highlightCompress * factor).toInt()
                b = (b + highlightCompress * factor).toInt()
            }

            // LUT Color Transform
            when (lut) {
                PresetLut.CINEMATIC_TEAL_ORANGE -> {
                    // Push shadows towards cyan/teal, highlights towards orange/gold
                    if (luma < 128f) {
                        b = (b * 1.15f).toInt()
                        g = (g * 1.05f).toInt()
                    } else {
                        r = (r * 1.15f).toInt()
                        g = (g * 1.02f).toInt()
                        b = (b * 0.90f).toInt()
                    }
                }
                PresetLut.LEICA_MONOCHROME -> {
                    val bw = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
                    // High-contrast S-curve
                    val curved = if (bw < 128) (bw * bw) / 128 else 255 - ((255 - bw) * (255 - bw)) / 128
                    r = curved; g = curved; b = curved
                }
                PresetLut.FILM_PORTRA -> {
                    r = (r * 1.08f + 5f).toInt()
                    g = (g * 1.02f).toInt()
                    b = (b * 0.95f + 8f).toInt()
                }
                PresetLut.VIBRANT_LANDSCAPE -> {
                    r = (r * 1.12f).toInt()
                    g = (g * 1.18f).toInt()
                    b = (b * 1.14f).toInt()
                }
                PresetLut.NATURAL -> { /* no color grade */ }
            }

            r = r.coerceIn(0, 255)
            g = g.coerceIn(0, 255)
            b = b.coerceIn(0, 255)

            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)

        if (addWatermark) {
            renderExifWatermark(output, lensInfo)
        }

        output
    }

    /**
     * Renders a sophisticated, high-end camera watermark at the bottom of the photo.
     */
    private fun renderExifWatermark(bitmap: Bitmap, lensInfo: String) {
        val canvas = Canvas(bitmap)
        val h = bitmap.height
        val w = bitmap.width

        val barHeight = (h * 0.055f).toInt()
        val bgPaint = Paint().apply {
            color = Color.parseColor("#E60A0B0E") // Subtle deep black bar
        }
        canvas.drawRect(0f, (h - barHeight).toFloat(), w.toFloat(), h.toFloat(), bgPaint)

        // Accent gold branding dot
        val dotPaint = Paint().apply {
            color = Color.parseColor("#FFB300")
            isAntiAlias = true
        }
        val dotRadius = barHeight * 0.14f
        val dotX = w * 0.05f
        val dotY = h - (barHeight * 0.5f)
        canvas.drawCircle(dotX, dotY, dotRadius, dotPaint)

        // Title text
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = barHeight * 0.38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("REALME 14 PRO+ 5G", dotX + dotRadius * 2.2f, dotY + titlePaint.textSize * 0.35f, titlePaint)

        // Lens metadata text on right
        val metaPaint = Paint().apply {
            color = Color.parseColor("#D0D0D0")
            textSize = barHeight * 0.32f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(lensInfo, w * 0.95f, dotY + metaPaint.textSize * 0.35f, metaPaint)
    }
}
