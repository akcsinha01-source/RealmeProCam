#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <cmath>
#include <vector>
#include <array>
#include <algorithm>
#include <cstring>

#define TAG "RealmeProCam_FocusStacker"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace {

// Convert ARGB_8888 bitmap to grayscale float plane
void extractGrayscale(const uint32_t* src, float* dst, int width, int height) {
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            uint32_t pixel = src[y * width + x];
            float r = (pixel & 0x000000FF);
            float g = ((pixel >> 8) & 0x000000FF);
            float b = ((pixel >> 16) & 0x000000FF);
            // Perceptual luminance
            dst[y * width + x] = 0.299f * r + 0.587f * g + 0.114f * b;
        }
    }
}

// Compute Sum of Modified Laplacian (SML) for multi-frequency edge detection
void computeModifiedLaplacian(const float* gray, float* focusMap, int width, int height) {
    for (int y = 2; y < height - 2; ++y) {
        for (int x = 2; x < width - 2; ++x) {
            int idx = y * width + x;
            float c = gray[idx];
            // Step 1
            float lx1 = std::abs(2.0f * c - gray[idx - 1] - gray[idx + 1]);
            float ly1 = std::abs(2.0f * c - gray[idx - width] - gray[idx + width]);
            // Step 2
            float lx2 = std::abs(2.0f * c - gray[idx - 2] - gray[idx + 2]);
            float ly2 = std::abs(2.0f * c - gray[idx - 2 * width] - gray[idx + 2 * width]);
            focusMap[idx] = lx1 + ly1 + 0.5f * (lx2 + ly2);
        }
    }
}

// Separable box filter for smooth regional sharpness confidence
void smoothFocusMap(const float* src, float* dst, int width, int height, int radius) {
    std::vector<float> temp(width * height, 0.0f);

    // Horizontal pass
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            float sum = 0.0f;
            int count = 0;
            for (int dx = -radius; dx <= radius; ++dx) {
                int nx = x + dx;
                if (nx >= 0 && nx < width) {
                    sum += src[y * width + nx];
                    count++;
                }
            }
            temp[y * width + x] = sum / count;
        }
    }

    // Vertical pass
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            float sum = 0.0f;
            int count = 0;
            for (int dy = -radius; dy <= radius; ++dy) {
                int ny = y + dy;
                if (ny >= 0 && ny < height) {
                    sum += temp[ny * width + x];
                    count++;
                }
            }
            dst[y * width + x] = sum / count;
        }
    }
}

} // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_realme_procam_processing_HybridFocusStacker_nativeAlignAndStack(
    JNIEnv* env,
    jobject /* thiz */,
    jobject bmpMainNear,
    jobject bmpTeleFar,
    jobject bmpOutput,
    jfloat opticalScaleRatio,
    jboolean usePyramid
) {
    AndroidBitmapInfo infoNear, infoFar, infoOut;
    void* pixelsNear = nullptr;
    void* pixelsFar = nullptr;
    void* pixelsOut = nullptr;

    if (AndroidBitmap_getInfo(env, bmpMainNear, &infoNear) < 0 ||
        AndroidBitmap_getInfo(env, bmpTeleFar, &infoFar) < 0 ||
        AndroidBitmap_getInfo(env, bmpOutput, &infoOut) < 0) {
        LOGE("Failed to get AndroidBitmapInfo");
        return JNI_FALSE;
    }

    if (AndroidBitmap_lockPixels(env, bmpMainNear, &pixelsNear) < 0 ||
        AndroidBitmap_lockPixels(env, bmpTeleFar, &pixelsFar) < 0 ||
        AndroidBitmap_lockPixels(env, bmpOutput, &pixelsOut) < 0) {
        LOGE("Failed to lock bitmap pixels");
        return JNI_FALSE;
    }

    int width = infoNear.width;
    int height = infoNear.height;

    const uint32_t* srcNear = static_cast<const uint32_t*>(pixelsNear);
    const uint32_t* srcFar = static_cast<const uint32_t*>(pixelsFar);
    uint32_t* outPixels = static_cast<uint32_t*>(pixelsOut);

    LOGD("Focus Stacker running: %dx%d (Same Zoom / Same Lens Optical Stacking)", width, height);

    // Step 1: Grayscale extraction
    std::vector<float> grayNear(width * height);
    std::vector<float> grayFar(width * height);
    extractGrayscale(srcNear, grayNear.data(), width, height);
    extractGrayscale(srcFar, grayFar.data(), width, height);

    // Step 2: Global Hand-Shake & Focus Breathing Alignment
    // Downsample by 4x to quickly search for best translation offset within [-16, +16] px
    int ds = 4;
    int dsW = width / ds;
    int dsH = height / ds;
    std::vector<float> dsNear(dsW * dsH);
    std::vector<float> dsFar(dsW * dsH);

    for (int dy = 0; dy < dsH; ++dy) {
        for (int dx = 0; dx < dsW; ++dx) {
            dsNear[dy * dsW + dx] = grayNear[(dy * ds) * width + (dx * ds)];
            dsFar[dy * dsW + dx] = grayFar[(dy * ds) * width + (dx * ds)];
        }
    }

    int bestDx = 0;
    int bestDy = 0;
    float minSAD = 1e20f;
    int maxShift = 12 / ds;

    for (int sy = -maxShift; sy <= maxShift; ++sy) {
        for (int sx = -maxShift; sx <= maxShift; ++sx) {
            float currentSAD = 0.0f;
            int count = 0;
            for (int y = maxShift; y < dsH - maxShift; y += 3) {
                for (int x = maxShift; x < dsW - maxShift; x += 3) {
                    float diff = dsNear[y * dsW + x] - dsFar[(y + sy) * dsW + (x + sx)];
                    currentSAD += std::abs(diff);
                    count++;
                }
            }
            if (count > 0 && (currentSAD / count) < minSAD) {
                minSAD = currentSAD / count;
                bestDx = sx * ds;
                bestDy = sy * ds;
            }
        }
    }
    LOGD("Alignment offset found: dx=%d, dy=%d", bestDx, bestDy);

    // Step 3: Compute Sharpness Maps for Near vs Far frames
    std::vector<float> focusNear(width * height, 0.0f);
    std::vector<float> focusFar(width * height, 0.0f);
    std::vector<float> smoothNear(width * height, 0.0f);
    std::vector<float> smoothFar(width * height, 0.0f);

    computeModifiedLaplacian(grayNear.data(), focusNear.data(), width, height);
    computeModifiedLaplacian(grayFar.data(), focusFar.data(), width, height);

    smoothFocusMap(focusNear.data(), smoothNear.data(), width, height, 9);
    smoothFocusMap(focusFar.data(), smoothFar.data(), width, height, 9);

    // Step 4: Multi-Band Soft Focus Fusion
    for (int y = 0; y < height; ++y) {
        int alignedY = std::clamp(y + bestDy, 0, height - 1);
        for (int x = 0; x < width; ++x) {
            int idx = y * width + x;
            int alignedX = std::clamp(x + bestDx, 0, width - 1);
            int alignedIdx = alignedY * width + alignedX;

            float sNear = smoothNear[idx];
            float sFar = smoothFar[alignedIdx];

            // Focus sharpness differential
            float diff = sFar - sNear;

            // Sigmoid transition with soft slope
            float weightFar = 1.0f / (1.0f + std::exp(-diff * 0.12f));

            // Soft-blend colors
            uint32_t pNear = srcNear[idx];
            uint32_t pFar = srcFar[alignedIdx];

            float rN = (pNear & 0xFF);
            float gN = ((pNear >> 8) & 0xFF);
            float bN = ((pNear >> 16) & 0xFF);

            float rF = (pFar & 0xFF);
            float gF = ((pFar >> 8) & 0xFF);
            float bF = ((pFar >> 16) & 0xFF);

            uint32_t rOut = static_cast<uint32_t>(rN * (1.0f - weightFar) + rF * weightFar);
            uint32_t gOut = static_cast<uint32_t>(gN * (1.0f - weightFar) + gF * weightFar);
            uint32_t bOut = static_cast<uint32_t>(bN * (1.0f - weightFar) + bF * weightFar);

            outPixels[idx] = (0xFF << 24) | (bOut << 16) | (gOut << 8) | rOut;
        }
    }

    AndroidBitmap_unlockPixels(env, bmpMainNear);
    AndroidBitmap_unlockPixels(env, bmpTeleFar);
    AndroidBitmap_unlockPixels(env, bmpOutput);

    LOGD("Focus Stacking completed successfully.");
    return JNI_TRUE;
}
