@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo   Realme 14 Pro+ 5G ProCam - Automated APK Builder
echo ========================================================
echo.

:: 1. Check for Java
where java >nul 2>nul
if %errorlevel% neq 0 (
    if "%JAVA_HOME%"=="" (
        echo [!] Java (JDK 17) nahi mila.
        echo [i] Kripya CMD/PowerShell me yeh command chalayein:
        echo     winget install Microsoft.OpenJDK.17
        echo.
        pause
        exit /b 1
    )
)

:: 2. Check for Android SDK
if "%ANDROID_HOME%"=="" (
    if exist "%LOCALAPPDATA%\Android\Sdk" (
        set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
        echo [OK] Android SDK mila: %LOCALAPPDATA%\Android\Sdk
    ) else (
        echo [!] Android SDK nahi mila.
        echo [i] Android Studio install karein ya yeh command chalayein:
        echo     winget install Google.AndroidStudio
        echo.
    )
)

echo [*] Building Realme 14 Pro+ ProCam APK...
call gradlew.bat assembleDebug

if %errorlevel% equ 0 (
    echo.
    echo ========================================================
    echo   APK SUCCESS! Badhai ho, APK taiyaar hai!
    echo ========================================================
    echo   File Path:
    echo   app\build\outputs\apk\debug\app-debug.apk
    echo ========================================================
    start explorer app\build\outputs\apk\debug
) else (
    echo.
    echo [!] Build me samasya aayi. Kripya Android Studio me project kholein.
)

pause
