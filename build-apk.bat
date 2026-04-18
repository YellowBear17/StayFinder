@echo off
echo ========================================
echo Building stayFinder APK
echo ========================================
echo.

call gradlew.bat assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo BUILD SUCCESSFUL!
    echo ========================================
    echo.
    echo APK Location:
    echo app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo To install on LDPlayer:
    echo 1. Drag and drop the APK file into LDPlayer window
    echo 2. Or use the "Install APK" button in LDPlayer
    echo.
) else (
    echo.
    echo BUILD FAILED! Check errors above.
)

pause
