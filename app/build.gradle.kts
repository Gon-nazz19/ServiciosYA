import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Shared App Check debug token of the team, kept in local.properties (not in Git). Optional.
val appCheckDebugToken: String = rootProject.file("local.properties")
    .takeIf { it.exists() }
    ?.let { file -> Properties().apply { file.inputStream().use(::load) } }
    ?.getProperty("APP_CHECK_DEBUG_TOKEN")
    .orEmpty()

// AdMob IDs of the release build, kept out of Git (local.properties or ~/.gradle/gradle.properties).
// Debug builds always use Google's test IDs. Without both, release builds show no ads.
fun adMobProperty(name: String): String =
    rootProject.file("local.properties")
        .takeIf { it.exists() }
        ?.let { file -> Properties().apply { file.inputStream().use(::load) } }
        ?.getProperty(name)
        ?: providers.gradleProperty(name).orNull
        ?: ""

val adMobAppId: String = adMobProperty("ADMOB_APP_ID")
val adMobNativeUnitId: String = adMobProperty("ADMOB_NATIVE_UNIT_ID")

android {
    namespace = "com.example.serviciosya"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.serviciosya"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Off until createServiceRequest is deployed: requests are written straight to Firestore.
        buildConfigField("boolean", "CREATE_REQUESTS_WITH_FUNCTION", "false")
    }

    buildTypes {
        debug {
            buildConfigField("String", "APP_CHECK_DEBUG_TOKEN", "\"$appCheckDebugToken\"")
            // Google's public test IDs for Android native ads.
            buildConfigField("boolean", "ADS_ENABLED", "true")
            buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-3940256099942544~3347511713\"")
            buildConfigField("String", "ADMOB_NATIVE_UNIT_ID", "\"ca-app-pub-3940256099942544/2247696110\"")
        }
        release {
            buildConfigField("boolean", "ADS_ENABLED", "${adMobAppId.isNotBlank() && adMobNativeUnitId.isNotBlank()}")
            buildConfigField("String", "ADMOB_APP_ID", "\"$adMobAppId\"")
            buildConfigField("String", "ADMOB_NATIVE_UNIT_ID", "\"$adMobNativeUnitId\"")
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.ai)
    implementation(libs.firebase.appcheck)
    debugImplementation(libs.firebase.appcheck.debug)
    releaseImplementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.play.services.location)
    implementation(libs.ads.mobile.sdk)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
