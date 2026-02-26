plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.app_ans"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.app_ans"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:3000/\"")
        buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", "\"881875440634-7di2vtufh0djlub2h298vov6d408aah4.apps.googleusercontent.com\"")
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

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.securityCrypto)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.base)
    implementation(libs.play.services.location)

    // Room
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    
    // WorkManager
    implementation(libs.work.runtime)

    // Glide
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Maps (Leaflet equivalent for Android)
    implementation(libs.osmdroid)

    // Testing - Unit Tests
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    
    // Testing - Instrumented Tests (Android)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("org.mockito:mockito-android:5.7.0")
}