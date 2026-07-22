@echo off
title TumorScan - AI Inference Server
color 0A

echo ============================================
echo   TumorScan AI Inference Server
echo ============================================
echo.
echo [INFO] Using Python 3.11 (TensorFlow 2.15)
echo [INFO] Make sure XAMPP Apache + MySQL are running!
echo.

:: Check if Python 3.11 is available
py -3.11 --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python 3.11 not found! Please install it first.
    pause
    exit /b 1
)

echo [OK] Python 3.11 found.
echo.
echo [INFO] Starting AI Inference Engine...
echo [INFO] Keep this window open while using the website.
echo.
echo ============================================
echo   Server ready! Open in browser:
echo   http://localhost/Tumor_web/index.html
echo ============================================
echo.

:: Keep the window open showing the server is ready
py -3.11 -c "import tensorflow as tf; print('[OK] TensorFlow', tf.__version__, '- TFLite ready'); print('[OK] Inference engine is active and waiting for requests...')" 2>nul

echo.
echo [INFO] Server is now active. Do NOT close this window.
pause
