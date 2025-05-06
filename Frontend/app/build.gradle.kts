plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android.plugin)
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.smartparkingsystem"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.smartparkingsystem"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.example.smartparkingsystem.HiltTestRunner"
    }

    buildFeatures {
        viewBinding = true
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.maps)

    // Test dependencies
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.mockk.mockk.android)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.core.testing) // Livedata/Viewmodel Testing
    testImplementation(libs.truth)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.espresso.intents)
    androidTestImplementation(libs.espresso.contrib)
    debugImplementation(libs.androidx.fragment.testing)
    androidTestImplementation(libs.dagger.hilt.android.testing)
    kaptAndroidTest(libs.hilt.compiler)

    //Navigation Component
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    //Dagger-Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    //Lottie
    implementation(libs.lottie)

    //Material
    implementation(libs.material)

    // Google Maps
    implementation(libs.play.services.maps.v1820)
    implementation(libs.play.services.location)

    //Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    // ViewModel ve LiveData
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.fragment.ktx)

    //Serialization
    implementation(libs.retrofit2.kotlinx.serialization.converter)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test"))

    //Glide
    implementation(libs.glide)
}

kapt {
    correctErrorTypes = true
}