plugins {
    alias(libs.plugins.android.app) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
}

tasks.register("generateXCWorkspace") {
    group = "build setup"
    layout.projectDirectory.file("dynamic-price.xcworkspace/contents.xcworkspacedata").run {
        outputs.file(this)
        doLast {
            asFile.writeText(
                """
<?xml version="1.0" encoding="UTF-8"?>
<Workspace version = "1.1">
    <Group location = "group:dynamic-price/android/src" name = "android-library">
        <FileRef location = "group:main" />
        <FileRef location = "group:test" />
    </Group>
    <Group location = "group:samples/android/src" name = "android-samples">
        <FileRef location = "group:main" />
    </Group>
    <Group location = "container:dynamic-price/ios/src" name = "ios-library">
        <FileRef location = "group:main" />
        <FileRef location = "group:test" />
    </Group>
    <FileRef location = "group:samples/ios/ios-samples.xcodeproj" />
    <FileRef location = "self:" name = "dynamic-price" />
</Workspace>
        """.trimIndent()
            )
        }
    }
}
