    plugins {
        id("com.android.application")
        id("org.jetbrains.kotlin.android")
        id("org.jetbrains.kotlin.kapt")          // needed for Room codegen
        id("org.jetbrains.kotlin.plugin.compose")// needed with Kotlin 2.0+
    }

    android {
        namespace = "com.example.todoapp2025"
        compileSdk = 36

        defaultConfig {
            applicationId = "com.example.todoapp2025"
            minSdk = 24
            targetSdk = 36
            versionCode = 1
            versionName = "1.0"
        }

        buildFeatures { compose = true }

        // No composeOptions with Kotlin 2.0+ (the Compose Gradle plugin handles it)

        kotlinOptions { jvmTarget = "17" }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    dependencies {
        // --- Compose UI ---
        implementation("androidx.activity:activity-compose:1.9.2")
        implementation("androidx.compose.ui:ui:1.7.5")
        implementation("androidx.compose.material3:material3:1.3.0")
        implementation("androidx.compose.ui:ui-tooling-preview:1.7.5")
        implementation(libs.androidx.appcompat)
        implementation(libs.material)
        implementation(libs.androidx.activity)
        implementation(libs.androidx.constraintlayout)
        implementation(libs.androidx.compose.material3)
        implementation(libs.androidx.compose.runtime)
        debugImplementation("androidx.compose.ui:ui-tooling:1.7.5")
        implementation("androidx.compose.material:material-icons-extended:1.6.0")
        implementation("androidx.datastore:datastore-preferences:1.1.1")


        // --- Lifecycle / ViewModel (Compose interop) ---
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
        implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

        // --- Room (database) ---
        implementation("androidx.room:room-ktx:2.6.1")
        kapt("androidx.room:room-compiler:2.6.1")

        // --- Coroutines ---
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    }
