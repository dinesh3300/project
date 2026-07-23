# NeuroCheck — AI-Powered Medical Diagnostics Platform

[![Comprehensive QA & Automated Test Suite](https://github.com/dinesh3300/project/actions/workflows/all-tests.yml/badge.svg)](https://github.com/dinesh3300/project/actions/workflows/all-tests.yml)
![Platform - Android](https://img.shields.io/badge/Platform-Android%20%7C%20Web-blue)
![Android SDK](https://img.shields.io/badge/Android%20SDK-API%2035-brightgreen)
![Java Version](https://img.shields.io/badge/Java-17-orange)
![Node Version](https://img.shields.io/badge/Node.js-v20-green)
![Python Version](https://img.shields.io/badge/Python-3.11-yellow)
![PHP Version](https://img.shields.io/badge/PHP-8.2-777BB4)

**NeuroCheck** is a full-stack medical analysis platform designed for brain hemorrhage detection, CT scan analysis, patient record management, and automated diagnostic report generation. It integrates an Android mobile application, a modern React web application, a secure PHP/MySQL REST API backend, and a YOLO TensorFlow Lite deep learning inference engine.

---

## 🏗 Recommended Repository Structure

```
nuerocheck/
├── .github/
│   └── workflows/
│       └── all-tests.yml        # CI/CD pipeline for Android, Web, Selenium & Backend
├── app/                         # Android Mobile Application (Java 17, Android SDK 35)
│   ├── src/
│   │   ├── main/                # Android UI, Fragments, Retrofit Client, YOLO TFLite
│   │   ├── test/                # Android JUnit Unit Tests
│   │   └── androidTest/         # Instrumented Android Tests
│   └── build.gradle.kts
├── web/                         # React / Vite Web Application
│   ├── src/                     # React components & UI views
│   ├── selenium-tests/          # E2E Web Automation Test Suite (Mocha + Selenium)
│   └── package.json
├── backend/                     # PHP REST API & Python AI Inference Engine
│   ├── db.php                   # Database connection configuration
│   ├── config.php               # System & SMTP config settings
│   ├── inference.py             # Python AI Scan Analysis Engine
│   ├── *.php                    # Authentication, Scans, & Patient APIs
│   ├── uploads/                 # Uploaded CT Scan Image directory
│   └── tests/                   # Backend QA test specifications
├── QA_Documentation/            # 100% Passed QA Excel Workbooks & Audit Logs
├── setup_nuerocheck_dbs.sql     # Database Schema & Initial Tables
├── build.gradle.kts             # Root Gradle build configuration
├── settings.gradle.kts          # Gradle settings configuration
├── gradlew                      # Gradle Wrapper script for Unix/Linux
├── gradlew.bat                  # Gradle Wrapper script for Windows
├── .gitignore                   # Version control ignore rules
└── README.md                    # Project Documentation
```

---

## 🛠 System Prerequisites & Dependencies

### 1. Android Application
- **JDK**: Java 17 (Temurin / OpenJDK)
- **Android SDK**: API Level 35 (Minimum SDK: 27)
- **Build System**: Gradle 8.x (Gradle Wrapper included)
- **Libraries**: Retrofit 2, Gson, Glide, CircleImageView, TensorFlow Lite

### 2. Web Frontend Application
- **Node.js**: v20.x or higher
- **Package Manager**: npm v10.x
- **Framework**: React 19 + Vite 8
- **E2E Testing**: Selenium WebDriver, ChromeDriver, Mocha, Chai

### 3. Backend & AI Inference Engine
- **Web Server**: Apache / Nginx (XAMPP, WAMP, or standalone)
- **PHP**: PHP 8.1 / 8.2 with `mysqli`, `pdo`, and `json` extensions
- **Database**: MySQL / MariaDB
- **Python**: Python 3.11+ with PyTorch / TensorFlow Lite, OpenCV, Pillow

---

## 🚀 Quick Start & Setup Guide

### 1. Clone the Repository
```bash
git clone https://github.com/dinesh3300/project.git
cd project
```

### 2. Database Configuration
1. Open your MySQL Database Management tool (phpMyAdmin, MySQL Workbench, or CLI).
2. Create a new database named `nuerocheck_db` (or `nuerocheck_web_db`).
3. Import the SQL schema:
   ```bash
   mysql -u root -p nuerocheck_db < setup_nuerocheck_dbs.sql
   ```
4. Update database credentials in [backend/db.php](file:///c:/Users/ADMIN/AndroidStudioProjects/nuerocheck/backend/db.php):
   ```php
   $host = "localhost";
   $username = "root";
   $password = "";
   $database = "nuerocheck_db";
   ```

### 3. Backend Setup
1. Host the `backend/` directory on your local Apache server (e.g. `C:\xampp\htdocs\nuerocheck_api`).
2. Verify API accessibility by opening `http://localhost/nuerocheck_api/check_user.php` in your browser.

### 4. Running the Web Application
```bash
cd web
npm install
npm run dev
```
The web application will launch at `http://localhost:5173`.

### 5. Running the Android Application
- Open the project folder in **Android Studio**.
- Ensure `RetrofitClient.java` points to your backend URL:
  - **Android Emulator**: `http://10.0.2.2/nuerocheck_api/`
  - **Physical Device**: `http://<YOUR_LOCAL_IP>/nuerocheck_api/`
- Build and run the app, or compile the APK from CLI:
  ```bash
  ./gradlew assembleDebug
  ```
  The generated APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🧪 Running Automated Tests

### Android Unit Tests
```bash
./gradlew test
```

### Selenium E2E Web Tests
```bash
cd web/selenium-tests
npm install
npm test
```

### PHP Backend Syntax Checks
```bash
for file in backend/*.php; do php -l "$file"; done
```

---

## 🔄 CI/CD Pipeline (GitHub Actions)

This repository includes an automated GitHub Actions CI workflow ([.github/workflows/all-tests.yml](file:///c:/Users/ADMIN/AndroidStudioProjects/nuerocheck/.github/workflows/all-tests.yml)) that runs on every push and pull request to `main`:

- 🟢 **Android Build & Unit Tests**: Compiles the Android project, runs unit tests, and uploads the debug APK artifact.
- 🟢 **Selenium E2E Automation**: Boots a headless Chrome browser, launches the Vite dev server, runs end-to-end tests, and uploads Excel QA reports.
- 🟢 **Web Frontend Build**: Verifies clean production bundling with Vite.
- 🟢 **Backend API Diagnostics**: Lints all PHP backend endpoints and verifies Python AI script syntax.
