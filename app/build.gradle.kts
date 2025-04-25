plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("kotlin-kapt")

}

android {
    namespace = "com.example.parkingtimerapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.parkingtimerapp"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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


    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
    }
    buildFeatures {
        viewBinding = true
        compose = true

    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    kotlin {
        sourceSets.all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }
    }
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas".toString())
    }

}

dependencies {
    // Material Icons dependency
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    
    // Compose dependencies
    implementation("androidx.compose.foundation:foundation:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.runtime:runtime:1.5.4")
    implementation("androidx.compose.animation:animation:1.5.4")
    implementation("androidx.compose.ui:ui-util:1.5.4")
    
    // Room Database Dependencies
    implementation ("androidx.room:room-runtime:2.6.1")
    implementation(libs.firebase.perf.ktx)
    ksp ("androidx.room:room-compiler:2.6.1") // ✅ Make sure this is the same version
    implementation ("androidx.room:room-ktx:2.6.1")

    // If using coroutines
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    //lifecycles
    implementation ("androidx.lifecycle:lifecycle-runtime-compose:2.6.1")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    // ✅ Ensure KSP version matches Kotlin
    implementation ("com.google.code.gson:gson:2.8.9")

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.ui)
    implementation(libs.material3)
    implementation(libs.ui.tooling.preview)
    implementation(libs.androidx.activity.compose.v180)

    debugImplementation(libs.ui.tooling)
    implementation(libs.androidx.core.ktx.v1101)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.espresso.core.v351)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation ("androidx.navigation:navigation-compose:2.7.2")
    implementation ("androidx.compose.ui:ui:1.5.0")
    implementation ("com.airbnb.android:lottie-compose:6.0.0")

    // Testing dependencies
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Fragment KTX
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    
    // View Binding
    implementation("androidx.databinding:viewbinding:8.3.0")
}
