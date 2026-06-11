import java.util.Base64

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.Xu.AlarmGrubbing"
    minSdk = 24
    targetSdk = 36
    
    // Inject dynamic Version Code and Version Name from properties or CI/CD env
    val paramVersionCode = project.findProperty("versionCode")?.toString()?.toIntOrNull()
      ?: System.getenv("VERSION_CODE")?.toIntOrNull()
      ?: 1
    val paramVersionName = project.findProperty("versionName")?.toString()
      ?: System.getenv("VERSION_NAME")
      ?: "1.0"

    versionCode = paramVersionCode
    versionName = paramVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      val keystoreFile = file("${rootDir}/debug.keystore")
      if (!keystoreFile.exists()) {
          try {
              val base64Content = "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"
              val decodedBytes = Base64.getMimeDecoder().decode(base64Content)
              keystoreFile.writeBytes(decodedBytes)
          } catch (e: Exception) {
              println("Failed to reconstruct keystore from base64 string: ${e.message}")
          }
      }
      storeFile = keystoreFile
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = true
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  // implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
