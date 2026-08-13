import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
}

// Load keystore credentials from local.properties (never committed to VCS)
val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) load(localPropsFile.inputStream())
}

android {
    namespace = "tech.salev.optimum"
    compileSdk = 36
    defaultConfig {
        applicationId = "tech.salev.optimum"
        minSdk = 26
        targetSdk = 36
        versionCode = 33
        versionName = "1.6.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file("optimum_keystore.jks")
            storePassword = localProperties.getProperty("OPTIMUM_STORE_PASSWORD")
                ?: error("OPTIMUM_STORE_PASSWORD not set in local.properties")
            keyAlias = localProperties.getProperty("OPTIMUM_KEY_ALIAS")
                ?: error("OPTIMUM_KEY_ALIAS not set in local.properties")
            keyPassword = localProperties.getProperty("OPTIMUM_KEY_PASSWORD")
                ?: error("OPTIMUM_KEY_PASSWORD not set in local.properties")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    sourceSets {
        getByName("androidTest") {
            assets.srcDirs(files("$projectDir/schemas"))
        }
    }
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose UI & Material 3
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)

  // Navigation
  implementation(libs.androidx.navigation.compose)

  // Room Database
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)
  androidTestImplementation(libs.androidx.room.testing)

  // WorkManager
  implementation(libs.androidx.work.runtime.ktx)

  // Hilt Dependency Injection
  implementation(libs.hilt.android)
  ksp(libs.hilt.android.compiler)
  implementation(libs.hilt.navigation.compose)

  // DataStore
  implementation(libs.androidx.datastore.preferences)

  // Google Fonts
  implementation(libs.androidx.compose.ui.text.google.fonts)

  // Immutable Collections (Compose performance)
  implementation(libs.kotlinx.collections.immutable)

  // Serialization
  implementation(libs.kotlinx.serialization.json)

  // Drag and Drop Reordering
  implementation("sh.calvin.reorderable:reorderable:3.1.0")

  // Google Credential Manager & Identity
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)

  // Tooling & Testing
  debugImplementation(libs.androidx.compose.ui.tooling)
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
