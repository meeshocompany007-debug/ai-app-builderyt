plugins {
    id("com.android.application") version "8.7.3"
    id("com.google.gms.google-services") version "4.4.2"
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

androidComponents.onVariants(androidComponents.selector().withBuildType("debug")) { variant ->
    variant.applicationId.set("com.aicandy.aialertpay")
}

// Release keeps its original package until its Firebase client is registered.
tasks.matching { it.name == "processReleaseGoogleServices" }.configureEach {
    onlyIf { file("src/release/google-services.json").isFile || file("google-services.json").isFile }
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
