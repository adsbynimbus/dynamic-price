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
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["appName"] = "Dynamic Price Samples"
        with(providers) {
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

    val adManagerAppId = providers.gradleProperty("sample.admanager.appid")
    flavorDimensions += "gma-sdk"
    productFlavors {
        register("legacy") {
            manifestPlaceholders["appName"] = "Dynamic Price Legacy Samples"
            manifestPlaceholders["gamAppId"] = adManagerAppId.get()
        }
        register("nextgen") {
            buildConfigField("String", "ADMANAGER_APP_ID", "\"${adManagerAppId.get()}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
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
    "legacyImplementation"(projects.androidLegacy)
    "legacyImplementation"(libs.ads.google.legacy)
    "nextgenImplementation"(projects.androidLibrary)
    "nextgenImplementation"(libs.ads.google.nextgen)
    implementation(platform(libs.androidx.compose))
    implementation(libs.bundles.androidx.compose)
    implementation(libs.ads.nimbus)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.startup)

    debugImplementation(libs.leakcanary)

    constraints {
        implementation(libs.okio)
        implementation(libs.protobuf) {
            because("Fixes CVE-2024-7254")
        }
    }
}
