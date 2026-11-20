plugins { id("com.android.application") }

android {
    namespace = "es.controlplus.c10"
    compileSdk = 35
    defaultConfig {
        applicationId = "es.controlplus.c10"
        minSdk = 30
        targetSdk = 35
        versionCode = 2
        versionName = "0.2-laboratorio"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.webkit:webkit:1.12.1")
}
