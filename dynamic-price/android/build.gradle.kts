import org.jetbrains.kotlin.gradle.dsl.*

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.dokka.javadoc)
    `maven-publish`
}

val dokkaJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
}

val dokkaHtmlJar by tasks.registering(Jar::class) {
    archiveClassifier.set("html-doc")
    from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
}

android {
    namespace = "com.adsbynimbus.dynamicprice"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        aarMetadata {
            minCompileSdk = 35
            minAgpVersion = "8.5.0" // Min Required for Kotlin 2.0
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing.singleVariant("release") {
        withSourcesJar()
    }

    sourceSets.configureEach {
        java.srcDirs("src/$name/kotlin")
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

kotlin {
    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_2_0
        languageVersion = KotlinVersion.KOTLIN_2_0
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.ads.google)
    implementation(libs.ads.nimbus)
    testImplementation(libs.bundles.test.unit)

    constraints {
        implementation(libs.androidx.core) {
            version {
                require("[1.13.0,)")
                because("BundleCompat.getSerializable added in 1.13.0")
            }
        }
        implementation(libs.okio) {
            version {
                require("[3.4.0,)")
                because("Addresses CVE-2023-3635 reported on Okio 3.2.0")
            }
        }
    }
}

dokka {
    moduleName = "Dynamic Price"
    dokkaGeneratorIsolation = ClassLoaderIsolation()
    dokkaSourceSets.configureEach {
        includes.from("Module.md")

        perPackageOption {
            matchingRegex = """.*\.internal.*"""
            suppress = true
        }

        sourceLink {
            localDirectory = layout.projectDirectory.dir("src/$name/kotlin")
            remoteLineSuffix = "#L"
            remoteUrl("https://github.com/adsbynimbus/dynamic-price/tree/main/android-sdk/src/$name/kotlin")
        }
    }
}
