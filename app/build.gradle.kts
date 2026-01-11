plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.khchan744.smart_qr_pay"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.khchan744.smart_qr_pay"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
    /*externalNativeBuild {
        // Or for ndk-build:
        ndkBuild {
            //path("src/main/cpp/Android.mk")
            path("../../org.quietmodem.Quiet/quiet/src/main/jni/Android.mk")
        }
    }*/
    ndkVersion = "14.1.3816874"
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.activity)
    implementation(libs.preference)
    implementation(libs.annotation)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation(project(":quiet"))

    // BoofCV (for QR generation)
    api(group = "org.boofcv", name = "boofcv-core", version = "1.2.4")
    implementation(group = "org.boofcv", name = "boofcv-android", version = "1.2.4")

    // Camera scanning dependencies (CameraX + ML Kit barcode scanning)
    implementation("androidx.camera:camera-core:1.5.2")
    implementation("androidx.camera:camera-camera2:1.5.2")
    implementation("androidx.camera:camera-lifecycle:1.5.2")
    implementation("androidx.camera:camera-view:1.5.2")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // for gif
    implementation("com.github.bumptech.glide:glide:5.0.5")
    annotationProcessor("com.github.bumptech.glide:compiler:5.0.5")

    // Retrofit + Gson (HTTP API)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
}