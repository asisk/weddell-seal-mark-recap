plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}
android {
    namespace = "weddellseal.markrecap"
    compileSdk = 34

    defaultConfig {
        applicationId = "weddellseal.markrecap"
        minSdk = 29
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true"
                )
            }
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
        testOptions {
            unitTests.isIncludeAndroidResources
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
        debug {
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
dependencies {
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.activity:activity-ktx:1.8.0")

    implementation("androidx.core:core-ktx:1.12.0")

    implementation("androidx.camera:camera-camera2:1.4.0-alpha01")
    implementation("androidx.camera:camera-lifecycle:1.4.0-alpha01")
    implementation("androidx.camera:camera-view:1.4.0-alpha01")

    implementation(platform("androidx.compose:compose-bom:2023.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material:1.6.0-alpha07")
    implementation("androidx.compose.material:material-icons-extended:1.6.0-alpha07")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3:1.2.0-alpha09")
    implementation("androidx.compose.runtime:runtime-livedata:1.5.3")
    implementation("androidx.compose.runtime:runtime-rxjava2:1.5.3")
    implementation("androidx.compose.runtime:runtime-rxjava3:1.5.3")
    implementation("androidx.compose.runtime:runtime:1.0.0")

    implementation("androidx.fragment:fragment-ktx:1.7.0-alpha06")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("androidx.test:runner:1.5.2")
    implementation("androidx.test:core:1.5.0")
    implementation("androidx.test.ext:junit:1.2.0-alpha01")
    implementation("com.opencsv:opencsv:5.8")
    implementation("commons-io:commons-io:2.6")

    val lifecycleVersion = "2.6.2"
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycleVersion")

    implementation("androidx.navigation:navigation-compose:2.7.4")

    val roomVersion = "2.5.2"
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")

    // To use Kotlin Symbol Processing (KSP)
    ksp("androidx.room:room-compiler:$roomVersion")

    // optional - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$roomVersion")

    // optional - RxJava2 support for Room
    implementation("androidx.room:room-rxjava2:$roomVersion")

    // optional - RxJava3 support for Room
    implementation("androidx.room:room-rxjava3:$roomVersion")

    // optional - Guava support for Room, including Optional and ListenableFuture
    implementation("androidx.room:room-guava:$roomVersion")

    // optional - Paging 3 Integration
    implementation("androidx.room:room-paging:$roomVersion")
    implementation("androidx.documentfile:documentfile:1.0.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation(files("androidx.test.core"))
    // optional - Test helpers
    testImplementation("androidx.room:room-testing:$roomVersion")
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    // Optional -- UI testing with Espresso
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    // Optional -- UI testing with UI Automator
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0-alpha04")

    // Optional -- UI testing with Compose
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}