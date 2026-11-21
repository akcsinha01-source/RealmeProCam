# Realme 14 Pro+ 5G ProCam (हाइब्रिड डुअल-लेंस प्रो कैमरा)

> **Tailored specifically for Realme 14 Pro+ 5G (Qualcomm Snapdragon 7s Gen 3)**
> Leveraging its **50MP Sony Main Wide Sensor (1x)** and **50MP Sony IMX882 Periscope Telephoto Lens (3x)** simultaneously for hybrid computational photography, alongside an **Interactive In-App User Guide & Demo Mode (Hindi + English)**.

---

## 🌟 1. Core Signature Feature: Hybrid Dual-Lens Focus Stacking (Main + Telephoto)

आमतौर पर स्मार्टफोन में जब आप पास की किसी वस्तु (Foreground) की फोटो लेते हैं, तो बैकग्राउंड ब्लर (Bokeh) हो जाता है। अगर आप दूर के बैकग्राउंड पर फोकस करते हैं, तो पास की वस्तु धुंधली हो जाती है। 

**Realme 14 Pro+ 5G** के पास एक समर्पित **3x पेरिस्कोप टेलीफोटो लेंस (73mm eq)** और एक बड़ा **1x मेन सेंसर (24mm eq)** है। यह ऐप दोनों लेंसों की अलग-अलग ऑप्टिकल शक्तियों को जोड़कर **हाइब्रिड फोकस स्टैकिंग** करता है:

```
[ Step 1: Main 1x Near ] ──▶ [ Step 2: Ghost Overlay & 3x Switch ] ──▶ [ Step 3: Telephoto 3x Far ] ──▶ [ Step 4: OpenCV & C++ Laplacian Fusion ]
     (10cm - 30cm)                 (3.04x Center Onion-Skin Guide)              (Optical Zoom Detail)              (Infinite Depth of Field Master)
```

1. **Step 1 — Near Subject (Main Lens 1x)**:
   - ऐप स्वचालित रूप से मेन 1x सेंसर को बाइंड करता है।
   - स्क्रीन पर नज़दीकी वस्तु पर टैप करने से `CONTROL_AF_MODE_OFF` और मैनुअल `LENS_FOCUS_DISTANCE` (मैक्रो दूरी) लॉक हो जाती है।
   - पहला उच्च-रिज़ॉल्यूशन फ्रेम मेमोरी में कैप्चर होता है।
2. **Step 2 — Guided Transition & Telephoto Switch**:
   - ऐप तुरंत सक्रिय कैमरा स्ट्रीम को **3x पेरिस्कोप टेलीफोटो लेंस** पर स्विच करता है।
   - व्यूफाइंडर पर पहले शॉट का केंद्र-क्रॉप किया हुआ सेमी-ट्रांसपेरेंट **घोस्ट ओवरले (Onion Skin)** दिखाई देता है, ताकि यूजर दूर के बैकग्राउंड को ठीक उसी रचना में अलाइन कर सके।
   - स्क्रीन पर हिंग्लिश में स्पष्ट संकेत आता है: *"Ab door wale subject par tap karke photo kheenchein (Telephoto Lens Active)"*।
3. **Step 3 — Far Subject (Telephoto Lens 3x)**:
   - पेरिस्कोप लेंस अनंत (Infinity) पर दूर की वस्तु पर ऑप्टिकल ज़ूम के साथ दूसरा सुपर-क्रिस्प फ्रेम कैप्चर करता है।
4. **Step 4 — On-Device Alignment, Scale Matching & Focus Blending**:
   - **OpenCV & C++ Native Pipeline (`focus_stacker.cpp`)**:
     - फोकल लेंग्थ स्केल मिलान (3.04x ऑप्टिकल मैग्निफिकेशन रेशियो)।
     - ORB/AKAZE फीचर डिटेक्शन और RANSAC होमोग्राफी के ज़रिए हाथ के कंपन और सेंसर के बीच के पैरालैक्स (Parallax) को सटीक अलाइन किया जाता है।
     - **मॉडिफाइड लैप्लासियन ऑपरेटर (Modified Laplacian Focus Measure)** दोनों छवियों के शार्पनेस मैप तैयार करता है।
     - **मल्टी-बैंड लैप्लासियन पिरामिड ब्लेंडिंग (Laplacian Pyramid Blending)** से बिना किसी सीमा रेखा (Seam line) या आर्टिफैक्ट के दोनों फ्रेम मिलकर एक अल्ट्रा-शार्प मास्टर फोटो में बदल जाते हैं!

---

## 📷 2. Extensive Pro & Computational Camera Capabilities

### 🔘 Direct Physical Lens Selector
- **`[0.6x UW]`**: 8MP अल्ट्रा-वाइड (16mm eq, ƒ/2.2)
- **`[1x WIDE]`**: 50MP Sony IMX890/LYT-701 मेन सेंसर (24mm eq, ƒ/1.88, OIS)
- **`[3x PERISCOPE]`**: 50MP Sony IMX882 पेरिस्कोप टेलीफोटो (73mm eq, ƒ/2.65, OIS)
- **`[6x LOSSLESS]`**: 50MP इन-सेंसर ऑप्टिकल क्रॉप (146mm eq)

### 🎛️ Full Manual (Pro) Controls (Tactile Haptic Dials)
- **ISO**: ISO 50 से लेकर ISO 12800 तक
- **शटर स्पीड (Shutter Speed)**: 1/8000 सेकंड से लेकर 32 सेकंड तक (`SENSOR_EXPOSURE_TIME`)
- **मैनुअल फोकस और फोकस पीकिंग**: 0.0 (Infinity) से 10.0 डायोप्टर (10cm Super Macro)। **लाइव फोकस पीकिंग (हरा/लाल कंटूर)** रियल-टाइम में फोकस में आई किनारों को चमकाता है।
- **व्हाइट बैलेंस (Kelvin WB)**: 2000K (वार्म टंगस्टन) से 8000K (कूल क्लाउडी)
- **EV एक्सपोज़र कॉम्पन्सेशन**: -3.0 EV से +3.0 EV (0.5 EV स्टेप्स)

### 🤖 AI & Computational Photography Modes
1. **Astro / Starry Sky Mode**: मल्टी-फ्रेम स्टार अलाइनमेंट और डार्क-फ्रेम नॉइज़ रिडक्शन, जिससे रात के आसमान में आकाशगंगा और तारे साफ उभरते हैं।
2. **Long Exposure / Light Trails**: बिना ट्राइपॉड के गाड़ियों की हेडलाइट की चमकीली लकीरें (Max-blend) और झरने का रेशमी पानी (Silky Water Frame Averaging)।
3. **Zero-Shutter-Lag HDR+**: तीन अलग-अलग एक्सपोज़र (-2 EV, 0 EV, +2 EV) का तुरंत ब्रैकेट कैप्चर और टोन-मैपिंग फ्यूज़न।
4. **Super Macro Mode**: 3x पेरिस्कोप टेलीफोटो लेंस को न्यूनतम फोकस दूरी (~15-20cm) पर लॉक करके बिना किसी बैरल डिस्टॉर्शन के अत्यधिक मैग्निफिकेशन।
5. **Document / Map Scanner Mode**: स्वचालित किनारा पहचान (Edge Detection), पर्सपेक्टिव सुधार और हाई-कंट्रास्ट ब्लैक-एंड-व्हाइट एनहांसमेंट।
6. **RAW (DNG) + JPEG Dual Capture**: Android `DngCreator` का उपयोग करके 10-बिट/12-बिट अनकंप्रेस्ड RAW DNG फाइलें सेव करता है।

### 🎬 Cinematography Viewfinder Tools
- **लाइव RGB + Luma हिस्टोग्राम**: 256-बिन रियल-टाइम वेवफॉर्म।
- **जेब्रा स्ट्राइप्स (Zebra Stripes)**: 95% से अधिक ओवरएक्सपोज़्ड हाइलाइट्स पर चेतावनी देने वाली डायगोनल लाइनें।
- **फ्रेमिंग ग्रिड्स**: Rule of Thirds (3x3), Golden Ratio (Fibonacci Spiral/Grid), और Center Crosshairs।
- **इलेक्ट्रॉनिक होराइजन लेवल (Electronic Horizon Level)**: हार्डवेयर जाइरोस्कोप और एक्सेलेरोमीटर से लैस। जब फोन बिल्कुल सीधा (0° ± 0.5°) होता है, तो लाइन हरी हो जाती है और एक हल्का हैप्टिक कंपन (Haptic Snap) मिलता है।

### 🎨 Built-in Mini Darkroom (Post-Editor)
- **3D LUT कलर प्रोफाइल्स**: Cinematic Teal & Gold, Leica Classic B&W (हाई-कंट्रास्ट मोनोक्रोम), Film Portra 400, Vivid Landscape।
- **क्लैरिटी और माइक्रो-कंट्रास्ट स्लाइर्ड्स**।
- **शैडो लिफ्ट और हाइलाइट कम्प्रेशन**।
- **कस्टम EXIF वॉटरमार्क**: *"Shot on Realme 14 Pro+ 5G | 50MP Periscope OIS | ƒ/2.65 73mm eq"*।

---

## 📖 3. Interactive In-App User Guide & Visual Demo Section ("कैसे उपयोग करें / Feature Guide")

शीर्ष बार में `?` आइकन दबाने पर खुलने वाली समर्पित इंटरैक्टिव गाइड:
- **हिंग्लिश + सरल हिंदी इंटरफ़ेस**: हर जटिल तकनीकी शब्द को आसान भाषा में समझाया गया है।
- **इंटरैक्टिव बिफोर/आफ्टर स्प्लिट-स्क्रीन स्लाइडर (`BeforeAfterSlider`)**:
  - यूजर अपनी उंगली से बीच की रेखा को बाएँ-दाएँ खींचकर देख सकता है कि साधारण फोटो का बैकग्राउंड कैसा धुंधला होता है और हाइब्रिड फोकस स्टैक्ड फोटो में सब कुछ कैसे क्रिस्टल-क्लियर हो जाता है।
- **सिमुलेटेड व्यूफाइंडर मिनी-डेमो (`SimulatedControlsDemo`)**:
  - **ISO सिमुलेटर**: स्लाइडर खींचकर देखें कि ISO बढ़ाने से ब्राइटनेस और डिजिटल ग्रेन/नॉइज़ कैसे बदलता है।
  - **शटर स्पीड सिमुलेटर**: देखें कि तेज़ शटर (1/2000s) एक्शन को कैसे फ्रीज़ करता है और धीमा शटर (2s) मोशन ब्लर कैसे बनाता है।
  - **फोकस पीकिंग सिमुलेटर**: स्लाइडर घुमाएँ और देखें कि विषय के फोकस में आते ही हरी चमकती किनारी कैसे प्रकट होती है।
- **"अभी आज़माएं" (Try This Feature Now) बटन**:
  - हर गाइड कार्ड पर मौजूद वन-टैप बटन तुरंत कैमरा व्यूफाइंडर को उसी मोड और सेटिंग्स में प्री-कॉन्फ़िगर करके खोल देता है!

---

## 🏗️ 4. Project Structure & Codebase

```
RealmeProCam/
├── app/
│   ├── build.gradle.kts                      # Gradle with OpenCV & NDK CMake config
│   ├── proguard-rules.pro                   # Keeps JNI native methods
│   └── src/main/
│       ├── AndroidManifest.xml              # Camera2, Sensors, MediaStore permissions
│       ├── cpp/
│       │   ├── CMakeLists.txt               # CMake C++ build config (-O3, OpenMP)
│       │   ├── focus_stacker.cpp            # Native C++: Scale Match, Homography, Laplacian Pyramid
│       │   └── image_proc_native.cpp        # Native Sobel Focus Peaking & 256-bin Histograms
│       ├── java/com/realme/procam/
│       │   ├── MainActivity.kt              # Fullscreen edge-to-edge Compose activity
│       │   ├── theme/                       # OLED Dark Theme (#000000), Gold (#FFB300), Cyan
│       │   ├── camera/
│       │   │   ├── RealmeCameraConstants.kt # Realme 14 Pro+ hardware mapping & parameters
│       │   │   ├── Camera2Controller.kt     # Multi-Camera routing, RAW DNG, Manual exposure & focus
│       │   │   └── SensorOrientationHelper.kt # Gyroscope electronic horizon & haptic snap
│       │   ├── processing/
│       │   │   ├── HybridFocusStacker.kt    # Dual-Lens 4-step focus stacking orchestrator
│       │   │   ├── ComputationalPhotographyEngine.kt # Astro, Light Trails, HDR+, Scanner
│       │   │   └── DarkroomEngine.kt        # 3D LUTs, Clarity, Watermarking
│       │   └── ui/
│       │       ├── CameraScreen.kt          # Master viewfinder, dials, quick lens pills
│       │       ├── components/              # ManualControlsPanel, CinematographyOverlays, GhostOverlay
│       │       ├── guide/                   # FeatureGuideScreen, BeforeAfterSlider, SimulatedDemos
│       │       └── viewmodel/CameraViewModel.kt # Reactive StateFlow controller
│       └── res/values/                      # Strings (Hindi/English), Colors, Themes
├── build.gradle.kts                         # Root Gradle config
└── settings.gradle.kts                      # Module settings
```

---

## 🚀 5. How to Build & Run

1. **Android Studio**:
   - Android Studio (Ladybug / Koala / Hedgehog या नया) में इस प्रोजेक्ट को खोलें।
   - पथ: `C:\Users\ASHWANI\.gemini\antigravity\scratch\RealmeProCam`
2. **SDK & NDK Requirements**:
   - `compileSdk`: **35** (Android 15)
   - `minSdk`: **29** (Android 10 - required for physical multi-camera stream routing)
   - `NDK`: **25.x+** or **26.x+** (CMake 3.22.1)
3. **Run on Realme 14 Pro+ 5G**:
   - अपने Realme 14 Pro+ 5G पर **Developer Options** और **USB Debugging** चालू करें।
   - USB केबल से कनेक्ट करें और **Run 'app' (Shift + F10)** दबाएं।
