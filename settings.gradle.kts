@file:Suppress("UnstableApiUsage")

enableFeaturePreview("ENHANCED_GRAPH_ORDERING")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        exclusiveContent {
            forRepository {
                google()
            }
            filter {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        exclusiveContent {
            forRepository {
                google()
            }
            filter {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google.android")
                includeGroupAndSubgroups("com.google.net.cronet")
                includeGroupAndSubgroups("org.chromium.net")
            }
        }
        exclusiveContent {
            forRepository {
                maven("https://adsbynimbus-public.s3.amazonaws.com/android/sdks")
            }
            filter {
                includeGroupAndSubgroups("com.adsbynimbus.android")
                includeGroup("com.iab.omid.library.adsbynimbus")
                includeGroup("com.adsbynimbus.openrtb")
            }
        }
        mavenCentral()
    }
}

fun enforceJacksonVersion(configurations: ConfigurationContainer) {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.module.name == "jackson-bom") {
                useVersion("2.22.1")
                because("Fixes CWE-918 (SSRF)")
            }
        }
    }
}

gradle.beforeProject {
    enforceJacksonVersion(buildscript.configurations)
    enforceJacksonVersion(configurations)
}

rootProject.name = "dynamic-price"

include("android-samples")
project(":android-samples").projectDir = layout.rootDirectory.file("samples/android").asFile
include("android-library")
project(":android-library").projectDir = layout.rootDirectory.file("library/android").asFile
include("android-legacy")
project(":android-legacy").projectDir = layout.rootDirectory.file("library/android-legacy").asFile
