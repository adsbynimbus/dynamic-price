@file:Suppress("UnstableApiUsage")

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

rootProject.name = "dynamic-price"

include("android")
project(":android").projectDir = layout.rootDirectory.file("platforms/android").asFile
