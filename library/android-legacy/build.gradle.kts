import org.jetbrains.kotlin.gradle.dsl.*

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.dokka.javadoc)
    `maven-publish`
}

val dokkaJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier = "javadoc"
    description = "Creates a javadoc jar for bundling with an Android Library"
    from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
}

val dokkaHtmlJar by tasks.registering(Jar::class) {
    archiveClassifier = "html-doc"
    description = "Creates a jar containing html docs for bundling with an Android Library"
    from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
}

val githubActions = providers.environmentVariable("GITHUB_ACTIONS")
androidComponents.beforeVariants {
    it.enable = it.name.contains("release", ignoreCase = true) || !githubActions.isPresent
}

android {
    namespace = "com.adsbynimbus.google"
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
    api(libs.ads.google) {
        version {
            require("24.5.0") // To be used for publishing
        }
    }
    api(libs.ads.nimbus)
    testImplementation(libs.bundles.test.unit)

    constraints {
        implementation(libs.androidx.core) {
            version {
                require("[1.13.0,)")
                because("BundleCompat.getSerializable added in 1.13.0")
            }
        }
    }
}

dokka {
    moduleName = "Dynamic Price Legacy"
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
            remoteUrl("https://github.com/adsbynimbus/dynamic-price/tree/main/library/android-legacy/src/$name/kotlin")
        }
    }
}
