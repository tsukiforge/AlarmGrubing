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
    applicationId = "com.kukkistudio.alarmgrup"
    minSdk = 24
    targetSdk = 36
    
    // Inject dynamic Version Code and Version Name from properties or CI/CD env
    val paramVersionCode = project.findProperty("versionCode")?.toString()?.toIntOrNull()
      ?: System.getenv("VERSION_CODE")?.toIntOrNull()
      ?: 51
    val paramVersionName = project.findProperty("versionName")?.toString()
      ?: System.getenv("VERSION_NAME")
      ?: "5.1.0"

    versionCode = paramVersionCode
    versionName = paramVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  splits {
    abi {
      isEnable = true
      reset()
      include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
      isUniversalApk = true
    }
  }

  val keystoreFile = file("${rootDir}/debug.keystore")
  val base64File = file("${rootDir}/debug.keystore.base64")
  val hasDebugKeystoreSource = keystoreFile.exists() || base64File.exists()

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD") ?: ""
      keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
      keyPassword = System.getenv("KEY_PASSWORD") ?: ""
    }
    if (hasDebugKeystoreSource) {
      create("debugConfig") {
        if (!keystoreFile.exists() && base64File.exists()) {
            try {
                val base64Text = base64File.readText().replace("\\s".toRegex(), "")
                val decodedBytes = Base64.getDecoder().decode(base64Text)
                keystoreFile.writeBytes(decodedBytes)
                println("Reconstructed keystore from debug.keystore.base64 using Standard Decoder.")
            } catch (e: Exception) {
                try {
                    val decodedBytes = Base64.getMimeDecoder().decode(base64File.readBytes())
                    keystoreFile.writeBytes(decodedBytes)
                    println("Reconstructed keystore from debug.keystore.base64 using MIME Decoder.")
                } catch (e2: Exception) {
                    println("Failed to reconstruct keystore from base64 file: ${e2.message}")
                }
            }
        }
        storeFile = keystoreFile
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
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
      if (hasDebugKeystoreSource) {
        signingConfig = signingConfigs.getByName("debugConfig")
      }
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
  lint {
    abortOnError = false
    checkReleaseBuilds = false
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
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.text.google.fonts)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.zxing.core)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.play.services.location)
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

// CI/CD Compatibility Task:
// When ABI splits are enabled, standard filenames like app-debug.apk are replaced with app-universal-debug.apk etc.
// This task clones the universal/compiled APK back to app-debug.apk & app-release.apk to keep CI/CD scripts fully functional.
abstract class CopyApkTask : DefaultTask() {
  @get:InputDirectory
  abstract val sourceDir: DirectoryProperty

  @get:OutputFile
  abstract val targetFile: RegularFileProperty

  @get:Input
  abstract val filterKeyword: Property<String>

  @TaskAction
  fun run() {
    val dir = sourceDir.get().asFile
    if (dir.exists()) {
      val allFiles = dir.listFiles()?.filter { it.name.endsWith(".apk") && it.name != targetFile.get().asFile.name } ?: emptyList()
      if (allFiles.isNotEmpty()) {
        val sourceFile = allFiles.find { it.name.contains(filterKeyword.get()) } ?: allFiles.first()
        sourceFile.copyTo(targetFile.get().asFile, overwrite = true)
        println("CI/CD Helper Task: Copied ${sourceFile.name} -> ${targetFile.get().asFile.name}")
      }
    }
  }
}

val copyDebugApk = tasks.register<CopyApkTask>("copyDebugApk") {
  sourceDir.set(layout.buildDirectory.dir("outputs/apk/debug"))
  targetFile.set(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
  filterKeyword.set("universal")
}

val copyReleaseApk = tasks.register<CopyApkTask>("copyReleaseApk") {
  sourceDir.set(layout.buildDirectory.dir("outputs/apk/release"))
  targetFile.set(layout.buildDirectory.file("outputs/apk/release/app-release.apk"))
  filterKeyword.set("universal")
}

tasks.configureEach {
  if (name == "assembleDebug") {
    finalizedBy(copyDebugApk)
  }
  if (name == "assembleRelease") {
    finalizedBy(copyReleaseApk)
  }
}


