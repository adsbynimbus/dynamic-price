import org.jetbrains.kotlin.gradle.dsl.*

plugins {
    alias(libs.plugins.android.app)
    alias(libs.plugins.kotlin.compose)
}

android {
    compileSdk = 36
    namespace = "com.adsbynimbus.dynamicprice.sample"

    defaultConfig {
        applicationId = namespace
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["appName"] = "Dynamic Price Samples"
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
    implementation(projects.androidLibrary)
    implementation(platform(libs.androidx.compose))
    implementation(libs.bundles.androidx.compose)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.startup)
}
