# Keep JNI native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class com.realme.procam.** { *; }

# OpenCV Android SDK
-keep class org.opencv.** { *; }

# Suppress harmless warnings during R8 tree-shaking
-dontwarn **
-ignorewarnings
