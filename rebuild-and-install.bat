@echo off
echo ========================================
echo Rebuilding and Installing stayFinder
echo ========================================
echo.

echo Step 1: Building APK...
call gradlew.bat assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo BUILD FAILED!
    pause
    exit /b 1
)

echo.
echo ========================================
echo BUILD SUCCESSFUL!
echo ========================================
echo.
echo APK Location: app\build\outputs\apk\debug\app-debug.apk
echo.

echo Step 2: Installing to LDPlayer...
echo.

C:\Android\Sdk\platform-tools\adb.exe connect 127.0.0.1:5555
C:\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo SUCCESS! App installed on LDPlayer
    echo ========================================
    echo.
    echo Open "My Application" in LDPlayer
) else (
    echo.
    echo ========================================
    echo INSTALLATION FAILED
    echo ========================================
    echo.
    echo MANUAL INSTALLATION:
    echo 1. Drag app\build\outputs\apk\debug\app-debug.apk
    echo 2. Drop it into LDPlayer window
    echo 3. Click Install
    echo.
    echo OR enable ADB debugging in LDPlayer:
    echo - Menu → Settings → Other settings → ADB debugging ON
    echo - Restart LDPlayer
    echo - Run this script again
)

echo.
pause
