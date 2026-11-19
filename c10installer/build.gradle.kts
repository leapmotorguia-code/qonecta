plugins { id("com.android.application") }
android {
    namespace = "com.leapmotorguia.c10installer"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.leapmotorguia.c10installer"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes { getByName("release") { isDebuggable=false; signingConfig=signingConfigs.getByName("debug") } }
    compileOptions { sourceCompatibility=JavaVersion.VERSION_1_8; targetCompatibility=JavaVersion.VERSION_1_8 }
}
