import org.jetbrains.kotlin.gradle.dsl.*

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.dokka.javadoc)
    `maven-publish`
}

val dokkaJavadocJar = tasks.register<Jar>("dokkaJavadocJar") {
    archiveClassifier = "javadoc"
    description = "Creates a javadoc jar for bundling with an Android Library"
    from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
}

val dokkaHtmlJar = tasks.register<Jar>("dokkaHtmlJar") {
    archiveClassifier = "html-doc"
    description = "Creates a jar containing html docs for bundling with an Android Library"
    from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
}

kotlin {
    android {
        namespace = "$group.legacy"
        compileSdk = 36
        minSdk = 23

        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions.jvmTarget = JvmTarget.JVM_11
            }
        }

        aarMetadata {
            minCompileSdk = 36
            minAgpVersion = "8.5.0" // Min Required for Kotlin 2.0
        }

        mavenPublication {
            artifact(dokkaJavadocJar)
            artifact(dokkaHtmlJar)
        }

        compilerOptions {
            apiVersion = KotlinVersion.KOTLIN_2_0
            languageVersion = KotlinVersion.KOTLIN_2_0
        }

        withHostTest { }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(libs.bundles.test.unit)
        }
        androidMain.dependencies {
            implementation(libs.ads.google.legacy.flatMap{ library ->
                providers.provider {
                    library.copy().apply {
                        version {
                            require("[24.9.0,)")
                            library.version?.let { prefer(it) }
                        }
                    }
                }
            })
            implementation(libs.ads.nimbus)
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies.constraints {
    androidMainImplementation(libs.androidx.core) {
        version {
            require("[1.13.0,)")
            because("BundleCompat.getSerializable added in 1.13.0")
        }
    }
    androidMainImplementation(libs.bundles.androidx.media3)
}

dokka {
    moduleName = "Dynamic Price Legacy"
    dokkaGeneratorIsolation = ClassLoaderIsolation()
    dokkaSourceSets.configureEach {
        includes.from("README.md")

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

publishing {
    // Rename root publication to nextgen and android publication to nextgen-android
    publications.withType<MavenPublication>().configureEach {
        artifactId = "dynamicprice-legacy" + if (name != "kotlinMultiplatform") "-$name" else ""
    }
    repositories {
        providers.environmentVariable("GITHUB_REPOSITORY").orNull?.let {
            maven("s3://adsbynimbus-public/android/sdks") {
                name = "aws"
                authentication {
                    create<AwsImAuthentication>("awsIm")
                }
            }
            maven("https://maven.pkg.github.com/$it") {
                name = "github"
                credentials(PasswordCredentials::class)
            }
        }
    }
}
