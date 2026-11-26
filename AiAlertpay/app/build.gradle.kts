plugins {
    id("com.android.application") version "8.7.3"
    id("com.google.gms.google-services") version "4.4.2" apply false
}

android {
    namespace = "app.aialertpay.mobile"
    compileSdk = 35
    defaultConfig {
        applicationId = "app.aialertpay.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }
}

if (file("google-services.json").isFile) {
    apply(plugin = "com.google.gms.google-services")
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
}
