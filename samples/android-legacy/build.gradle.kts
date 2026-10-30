import org.jetbrains.kotlin.gradle.dsl.*

plugins {
    alias(libs.plugins.android.app)
    alias(libs.plugins.kotlin.compose)
}

val githubActions = providers.environmentVariable("GITHUB_ACTIONS")
androidComponents.beforeVariants {
    it.enable = it.name.contains("release", ignoreCase = true) || !githubActions.isPresent
}

android {
    compileSdk = 37
    namespace = "$group.sample"

    defaultConfig {
        applicationId = namespace
        minSdk = 26
        targetSdk = compileSdk
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["appName"] = "Dynamic Price Legacy Samples"
        with(providers) {
            manifestPlaceholders["gamAppId"] = gradleProperty("sample.admanager.appid").get()
            buildConfigField("String", "API_KEY", "\"${gradleProperty("sample.nimbus.apikey").get()}\"")
            buildConfigField("String", "PUBLISHER_KEY", "\"${gradleProperty("sample.nimbus.publisherkey").get()}\"")
            buildConfigField("String", "ADMANAGER_ADUNIT_ID", "\"${gradleProperty("sample.admanager.adunitid").get()}\"")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions.unitTests.isIncludeAndroidResources = true
}

kotlin {
    target {
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions.jvmTarget = JvmTarget.JVM_17
            }
        }
    }
}

dependencies {
    implementation(projects.androidLegacy)
    implementation(platform(libs.androidx.compose))
    implementation(libs.bundles.androidx.compose)
    implementation(libs.ads.google.legacy)
    implementation(libs.ads.nimbus)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.startup)
    implementation(libs.okio)
    implementation(libs.protobuf)

    debugImplementation(libs.leakcanary)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
