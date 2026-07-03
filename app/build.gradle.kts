plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "ai.os.cloneapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "ai.os.cloneapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

    gradle.projectsEvaluated {
        tasks.withType<JavaCompile>().configureEach {
            val originalBootstrap = options.bootstrapClasspath?.files
            val customJars = listOf(
                file("..\\libs\\framework-dex2jar.jar")
            )
            options.bootstrapClasspath = files(customJars + originalBootstrap)
        }
    }



    applicationVariants.all {
        outputs.all {
            val appName = "CloneApp" // your app name
            val versionName = versionName
            val buildType = buildType.name

            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "$appName.apk"
        }
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    compileOnly(files("..\\libs\\framework-dex2jar.jar"))

    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    implementation("com.google.code.gson:gson:2.13.2")


    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

// Credential Manager (modern approach, API 34+)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    implementation("com.google.android.gms:play-services-auth:21.0.0")



}