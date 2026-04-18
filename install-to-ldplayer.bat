@echo off
echo ========================================
echo Installing stayFinder to LDPlayer
echo ========================================
echo.

echo Checking if LDPlayer is connected...
C:\Android\Sdk\platform-tools\adb.exe devices

echo.
echo Connecting to LDPlayer...
C:\Android\Sdk\platform-tools\adb.exe connect 127.0.0.1:5555

echo.
echo Installing APK...
C:\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo SUCCESS! App installed on LDPlayer
    echo ========================================
    echo.
    echo Open "My Application" in LDPlayer to test
) else (
    echo.
    echo ========================================
    echo INSTALLATION FAILED
    echo ========================================
    echo.
    echo Make sure:
    echo 1. LDPlayer is running
    echo 2. ADB debugging is enabled in LDPlayer:
    echo    - Click menu (three lines) in LDPlayer
    echo    - Go to Settings ^> Other settings
    echo    - Enable "ADB debugging"
    echo 3. Restart LDPlayer after enabling ADB
    echo.
    echo Then run this script again
)

echo.
pause
