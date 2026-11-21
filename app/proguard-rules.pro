# Keep JNI native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class com.realme.procam.processing.HybridFocusStacker { *; }
-keep class com.realme.procam.processing.ComputationalPhotographyEngine { *; }
-keep class com.realme.procam.camera.** { *; }

# OpenCV Android SDK
-keep class org.opencv.** { *; }
