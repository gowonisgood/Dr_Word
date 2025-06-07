plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.dr_word"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.dr_word"
        minSdk = 24
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-firestore-ktx:25.1.3")
    implementation("com.google.firebase:firebase-storage-ktx")

    // Gemini 사용을 위한 google SDK
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // retrofit 사용
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // json 파싱을 위한 gson
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // 네이버 API에서 받은 이미지 띄우기 위함
    implementation ("com.github.bumptech.glide:glide:4.15.1")

    implementation("androidx.recyclerview:recyclerview:1.3.2")


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}