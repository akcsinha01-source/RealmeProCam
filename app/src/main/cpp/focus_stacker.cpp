#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <cmath>
#include <vector>
#include <algorithm>
#include <cstring>
#include <omp.h>

#define TAG "RealmeProCam_FocusStacker"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace {

struct PixelRGB {
    uint8_t r, g, b, a;
};

// Convert ARGB_8888 bitmap to grayscale float plane
void extractGrayscale(const uint32_t* src, float* dst, int width, int height) {
    #pragma omp parallel for collapse(2)
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

// Compute Modified Laplacian Focus Measure (SML)
void computeModifiedLaplacian(const float* gray, float* focusMap, int width, int height, int step = 1) {
    #pragma omp parallel for collapse(2)
    for (int y = step; y < height - step; ++y) {
        for (int x = step; x < width - step; ++x) {
            float center = gray[y * width + x];
            float lx = std::abs(2.0f * center - gray[y * width + (x - step)] - gray[y * width + (x + step)]);
            float ly = std::abs(2.0f * center - gray[(y - step) * width + x] - gray[(y + step) * width + x]);
            focusMap[y * width + x] = lx + ly;
        }
    }
}

// Box filter / Gaussian blur on focus map to obtain region confidence
void smoothFocusMap(const float* src, float* dst, int width, int height, int radius) {
    std::vector<float> temp(width * height, 0.0f);

    // Horizontal pass
    #pragma omp parallel for
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
    #pragma omp parallel for
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

// Bilinear sampling from ARGB_8888 buffer
uint32_t sampleBilinear(const uint32_t* src, int w, int h, float x, float y) {
    if (x < 0 || x >= w - 1 || y < 0 || y >= h - 1) {
        return 0xFF000000; // Transparent or black boundary
    }
    int x0 = static_cast<int>(x);
    int y0 = static_cast<int>(y);
    int x1 = x0 + 1;
    int y1 = y0 + 1;
    float fx = x - x0;
    float fy = y - y0;

    auto unpack = [](uint32_t p) {
        return std::array<float, 4>{
            static_cast<float>(p & 0xFF),
            static_cast<float>((p >> 8) & 0xFF),
            static_cast<float>((p >> 16) & 0xFF),
            static_cast<float>((p >> 24) & 0xFF)
        };
    };

    auto c00 = unpack(src[y0 * w + x0]);
    auto c10 = unpack(src[y0 * w + x1]);
    auto c01 = unpack(src[y1 * w + x0]);
    auto c11 = unpack(src[y1 * w + x1]);

    uint32_t out = 0;
    for (int i = 0; i < 4; ++i) {
        float top = c00[i] * (1.0f - fx) + c10[i] * fx;
        float bottom = c01[i] * (1.0f - fx) + c11[i] * fx;
        float val = top * (1.0f - fy) + bottom * fy;
        uint32_t ival = static_cast<uint32_t>(std::clamp(val, 0.0f, 255.0f));
        out |= (ival << (i * 8));
    }
    return out;
}

} // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_realme_procam_processing_HybridFocusStacker_nativeAlignAndStack(
    JNIEnv* env,
    jobject /* thiz */,
    jobject bmpMainNear,
    jobject bmpTeleFar,
    jobject bmpOutput,
    jfloat opticalScaleRatio, // ~3.04f for Realme 14 Pro+ (73mm eq / 24mm eq)
    jboolean usePyramid
) {
    AndroidBitmapInfo infoMain, infoTele, infoOut;
    void* pixelsMain = nullptr;
    void* pixelsTele = nullptr;
    void* pixelsOut = nullptr;

    if (AndroidBitmap_getInfo(env, bmpMainNear, &infoMain) < 0 ||
        AndroidBitmap_getInfo(env, bmpTeleFar, &infoTele) < 0 ||
        AndroidBitmap_getInfo(env, bmpOutput, &infoOut) < 0) {
        LOGE("Failed to get AndroidBitmapInfo");
        return JNI_FALSE;
    }

    if (AndroidBitmap_lockPixels(env, bmpMainNear, &pixelsMain) < 0 ||
        AndroidBitmap_lockPixels(env, bmpTeleFar, &pixelsTele) < 0 ||
        AndroidBitmap_lockPixels(env, bmpOutput, &pixelsOut) < 0) {
        LOGE("Failed to lock bitmap pixels");
        return JNI_FALSE;
    }

    int width = infoMain.width;
    int height = infoMain.height;
    int teleW = infoTele.width;
    int teleH = infoTele.height;

    const uint32_t* srcMain = static_cast<const uint32_t*>(pixelsMain);
    const uint32_t* srcTele = static_cast<const uint32_t*>(pixelsTele);
    uint32_t* outPixels = static_cast<uint32_t*>(pixelsOut);

    LOGD("Focus Stacker running: Main %dx%d, Tele %dx%d, scale: %.2f", width, height, teleW, teleH, opticalScaleRatio);

    // Step 1: Compute coordinate mapping for Telephoto (3x) onto Main (1x) frame
    // The telephoto covers the center region [cx - w/(2*scale), cy - h/(2*scale)]
    float cx = width * 0.5f;
    float cy = height * 0.5f;
    float tcx = teleW * 0.5f;
    float tcy = teleH * 0.5f;

    // Buffer for telephoto warped into Main image's frame
    std::vector<uint32_t> warpedTele(width * height, 0);

    #pragma omp parallel for collapse(2)
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            // Map main (x, y) to telephoto coordinates (tx, ty)
            float tx = tcx + (x - cx) * opticalScaleRatio;
            float ty = tcy + (y - cy) * opticalScaleRatio;

            if (tx >= 0 && tx < teleW && ty >= 0 && ty < teleH) {
                warpedTele[y * width + x] = sampleBilinear(srcTele, teleW, teleH, tx, ty);
            } else {
                warpedTele[y * width + x] = 0; // Out of telephoto FOV
            }
        }
    }

    // Step 2: Compute Sharpness Maps for Foreground (Main 1x) vs Background (Telephoto 3x)
    std::vector<float> grayMain(width * height);
    std::vector<float> grayTele(width * height);
    std::vector<float> focusMain(width * height, 0.0f);
    std::vector<float> focusTele(width * height, 0.0f);
    std::vector<float> smoothMain(width * height, 0.0f);
    std::vector<float> smoothTele(width * height, 0.0f);

    extractGrayscale(srcMain, grayMain.data(), width, height);
    extractGrayscale(warpedTele.data(), grayTele.data(), width, height);

    computeModifiedLaplacian(grayMain.data(), focusMain.data(), width, height, 2);
    computeModifiedLaplacian(grayTele.data(), focusTele.data(), width, height, 2);

    smoothFocusMap(focusMain.data(), smoothMain.data(), width, height, 7);
    smoothFocusMap(focusTele.data(), smoothTele.data(), width, height, 7);

    // Step 3: Multi-Band Blending / Soft Focus Fusion
    #pragma omp parallel for collapse(2)
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            int idx = y * width + x;
            uint32_t telePixel = warpedTele[idx];

            // If pixel is outside telephoto lens coverage, keep 1x main wide lens
            if (telePixel == 0) {
                outPixels[idx] = srcMain[idx];
                continue;
            }

            float sMain = smoothMain[idx];
            float sTele = smoothTele[idx];

            // Weight calculation: Sigmoid transition based on focus sharpness differential
            float diff = sTele - sMain;
            float weightTele = 1.0f / (1.0f + std::exp(-diff * 0.15f));

            // Soft-blend boundary to prevent hard seam line
            uint32_t pMain = srcMain[idx];
            float rm = (pMain & 0xFF);
            float gm = ((pMain >> 8) & 0xFF);
            float bm = ((pMain >> 16) & 0xFF);

            float rt = (telePixel & 0xFF);
            float gt = ((telePixel >> 8) & 0xFF);
            float bt = ((telePixel >> 16) & 0xFF);

            uint32_t rOut = static_cast<uint32_t>(rm * (1.0f - weightTele) + rt * weightTele);
            uint32_t gOut = static_cast<uint32_t>(gm * (1.0f - weightTele) + gt * weightTele);
            uint32_t bOut = static_cast<uint32_t>(bm * (1.0f - weightTele) + bt * weightTele);

            outPixels[idx] = (0xFF << 24) | (bOut << 16) | (gOut << 8) | rOut;
        }
    }

    AndroidBitmap_unlockPixels(env, bmpMainNear);
    AndroidBitmap_unlockPixels(env, bmpTeleFar);
    AndroidBitmap_unlockPixels(env, bmpOutput);

    LOGD("Focus Stacking completed successfully.");
    return JNI_TRUE;
}
