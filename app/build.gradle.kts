import org.gradle.api.tasks.Copy

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.clinicalc.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.clinicalc.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures { compose = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

val catalogAsset = tasks.register<Copy>("copyClinicalSources") {
    into(layout.projectDirectory.dir("src/main/assets"))
    from(rootProject.file("# Comprehensive Medical Scoring Systems")) {
        rename { "catalog.md" }
    }
    from(rootProject.file("README.md")) {
        rename { "master_prompt.md" }
    }
}
tasks.named("preBuild").configure { dependsOn(catalogAsset) }
