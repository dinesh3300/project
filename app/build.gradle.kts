plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.brainhemorrhage"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.brainhemorrhage"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    androidResources {
        noCompress += "tflite"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.cardview)
    implementation(libs.recyclerview)
    implementation(libs.glide)
    implementation(libs.circleimageview)
    
    // Retrofit & Gson for PHP backend
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)

    // TensorFlow Lite for YOLO logic
    implementation(libs.tflite)
    implementation(libs.tflite.support)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}