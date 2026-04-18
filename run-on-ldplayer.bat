@echo off
echo ========================================
echo Building and Installing stayFinder App
echo ========================================
echo.

echo Step 1: Building APK...
call gradlew.bat assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo Build failed! Please check the errors above.
    pause
    exit /b 1
)

echo.
echo Step 2: Looking for LDPlayer ADB...

REM Common LDPlayer installation paths
set LDPLAYER_PATH1=C:\LDPlayer\LDPlayer4.0
set LDPLAYER_PATH2=C:\LDPlayer\LDPlayer9
set LDPLAYER_PATH3=%LOCALAPPDATA%\LDPlayer
set LDPLAYER_PATH4=D:\LDPlayer\LDPlayer4.0

set ADB_PATH=

if exist "%LDPLAYER_PATH1%\adb.exe" set ADB_PATH=%LDPLAYER_PATH1%\adb.exe
if exist "%LDPLAYER_PATH2%\adb.exe" set ADB_PATH=%LDPLAYER_PATH2%\adb.exe
if exist "%LDPLAYER_PATH3%\adb.exe" set ADB_PATH=%LDPLAYER_PATH3%\adb.exe
if exist "%LDPLAYER_PATH4%\adb.exe" set ADB_PATH=%LDPLAYER_PATH4%\adb.exe

if "%ADB_PATH%"=="" (
    echo LDPlayer ADB not found in common locations.
    echo Please make sure LDPlayer is installed and running.
    echo.
    echo You can manually install the APK from:
    echo app\build\outputs\apk\debug\app-debug.apk
    pause
    exit /b 1
)

echo Found ADB at: %ADB_PATH%
echo.

echo Step 3: Connecting to LDPlayer...
"%ADB_PATH%" connect 127.0.0.1:5555

echo.
echo Step 4: Installing APK...
"%ADB_PATH%" install -r app\build\outputs\apk\debug\app-debug.apk

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo SUCCESS! App installed on LDPlayer
    echo ========================================
    echo.
    echo You can now find "My Application" in LDPlayer
) else (
    echo.
    echo Installation failed. Make sure:
    echo 1. LDPlayer is running
    echo 2. ADB debugging is enabled in LDPlayer settings
)

echo.
pause
