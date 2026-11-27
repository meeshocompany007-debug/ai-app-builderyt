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
    implementation("com.google.zxing:core:3.5.3")
    implementation("androidx.core:core:1.13.1")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
}

val voiceAssets = layout.buildDirectory.dir("generated/receiptAudio")
val generateReceiptAudio by tasks.registering(Exec::class) {
    val generator = rootProject.file("voices/generate.py")
    inputs.files(generator, rootProject.file("voices/catalog.json"))
    outputs.dir(voiceAssets)
    commandLine("python3", generator.absolutePath, voiceAssets.get().asFile.absolutePath)
}
android.sourceSets["main"].assets.srcDir(voiceAssets)
tasks.named("preBuild").configure { dependsOn(generateReceiptAudio) }
