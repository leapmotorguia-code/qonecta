plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "es.c10.complemento"
 compileSdk = 35
 defaultConfig { applicationId = "es.c10.complemento"; minSdk = 28; targetSdk = 35; versionCode = 13; versionName = "1.3-prueba" }
 buildTypes { getByName("release") { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("debug") } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies { testImplementation("junit:junit:4.13.2") }
