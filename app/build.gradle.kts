plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
kotlin { jvmToolchain(17) }
android { compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }; namespace = "dev.nova.assistant"; compileSdk = 35
 defaultConfig { applicationId = "dev.nova.assistant"; minSdk = 29; targetSdk = 35; versionCode = 2; versionName = "0.2.0"; ndk { abiFilters += listOf("arm64-v8a") } } }

android { androidResources { noCompress += "onnx" } }
dependencies { implementation(files("libs/sherpa-onnx-1.13.3.aar")) }
