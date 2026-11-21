#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <cmath>
#include <vector>
#include <algorithm>

#define TAG "RealmeProCam_ImageProc"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

extern "C" JNIEXPORT void JNICALL
Java_com_realme_procam_processing_ComputationalPhotographyEngine_nativeComputeHistogram(
    JNIEnv* env,
    jobject /* thiz */,
    jobject bitmap,
    jintArray outLuma,
    jintArray outRed,
    jintArray outGreen,
    jintArray outBlue
) {
    AndroidBitmapInfo info;
    void* pixels = nullptr;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0 || AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        return;
    }

    int width = info.width;
    int height = info.height;
    const uint32_t* src = static_cast<const uint32_t*>(pixels);

    int lumaHist[256] = {0};
    int rHist[256] = {0};
    int gHist[256] = {0};
    int bHist[256] = {0};

    // Subsample for fast 60fps performance
    int step = std::max(1, (width * height) / 100000);

    for (int i = 0; i < width * height; i += step) {
        uint32_t p = src[i];
        int r = (p & 0xFF);
        int g = ((p >> 8) & 0xFF);
        int b = ((p >> 16) & 0xFF);
        int luma = static_cast<int>(0.299f * r + 0.587f * g + 0.114f * b);

        lumaHist[std::clamp(luma, 0, 255)]++;
        rHist[r]++;
        gHist[g]++;
        bHist[b]++;
    }

    AndroidBitmap_unlockPixels(env, bitmap);

    env->SetIntArrayRegion(outLuma, 0, 256, lumaHist);
    env->SetIntArrayRegion(outRed, 0, 256, rHist);
    env->SetIntArrayRegion(outGreen, 0, 256, gHist);
    env->SetIntArrayRegion(outBlue, 0, 256, bHist);
}

extern "C" JNIEXPORT void JNICALL
Java_com_realme_procam_processing_ComputationalPhotographyEngine_nativeApplyFocusPeaking(
    JNIEnv* env,
    jobject /* thiz */,
    jobject inputBitmap,
    jobject outputBitmap,
    jint threshold,
    jint highlightColor // 0xFF00E676 (Green) or 0xFFFF1744 (Red)
) {
    AndroidBitmapInfo inInfo, outInfo;
    void* inPixels = nullptr;
    void* outPixels = nullptr;

    if (AndroidBitmap_getInfo(env, inputBitmap, &inInfo) < 0 ||
        AndroidBitmap_getInfo(env, outputBitmap, &outInfo) < 0 ||
        AndroidBitmap_lockPixels(env, inputBitmap, &inPixels) < 0 ||
        AndroidBitmap_lockPixels(env, outputBitmap, &outPixels) < 0) {
        return;
    }

    int width = inInfo.width;
    int height = inInfo.height;
    const uint32_t* src = static_cast<const uint32_t*>(inPixels);
    uint32_t* dst = static_cast<uint32_t*>(outPixels);

    for (int y = 1; y < height - 1; ++y) {
        for (int x = 1; x < width - 1; ++x) {
            // Compute Sobel 3x3 gradient on luminance
            auto getLuma = [&](int px, int py) {
                uint32_t p = src[py * width + px];
                return 0.299f * (p & 0xFF) + 0.587f * ((p >> 8) & 0xFF) + 0.114f * ((p >> 16) & 0xFF);
            };

            float gx = -getLuma(x - 1, y - 1) + getLuma(x + 1, y - 1)
                       -2.0f * getLuma(x - 1, y) + 2.0f * getLuma(x + 1, y)
                       -getLuma(x - 1, y + 1) + getLuma(x + 1, y + 1);

            float gy = -getLuma(x - 1, y - 1) - 2.0f * getLuma(x, y - 1) - getLuma(x + 1, y - 1)
                       +getLuma(x - 1, y + 1) + 2.0f * getLuma(x, y + 1) + getLuma(x + 1, y + 1);

            float mag = std::sqrt(gx * gx + gy * gy);

            if (mag > threshold) {
                // In-focus edge: paint highlight color
                dst[y * width + x] = static_cast<uint32_t>(highlightColor);
            } else {
                // Keep original
                dst[y * width + x] = src[y * width + x];
            }
        }
    }

    AndroidBitmap_unlockPixels(env, inputBitmap);
    AndroidBitmap_unlockPixels(env, outputBitmap);
}
