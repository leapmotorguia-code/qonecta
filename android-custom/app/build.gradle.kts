plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "com.qonecta.custom"
 compileSdk = 35
 defaultConfig { applicationId = "com.qonecta.custom"; minSdk = 28; targetSdk = 35; versionCode = 12; versionName = "1.2-prueba" }
 buildTypes { getByName("release") { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("debug") } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies { testImplementation("junit:junit:4.13.2") }
