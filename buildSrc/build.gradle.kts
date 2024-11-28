import org.gradle.plugins.ide.idea.model.IdeaModel
import java.net.URI

val kotlinVersion = "1.7.10"
val kotlinCoroutineVersion = "1.4.2"

dependencies {
    implementation("org.jlleitschuh.gradle:ktlint-gradle:9.4.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.yaml:snakeyaml:1.33")
}


configurations.all {
    resolutionStrategy {
        failOnVersionConflict()
        force(
            "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion",
            "org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion",
            "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion"
        )
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
    maven { url = URI("https://plugins.gradle.org/m2/") }
}


plugins {
    idea
    `kotlin-dsl`
    `embedded-kotlin`
}

kotlinDslPluginOptions {
    @Suppress("UnstableApiUsage")
    experimentalWarning.set(false)
}

group = "io.patiently.gcloud"

configure<IdeaModel> {
    module {
        outputDir = file("build/idea-out")
        testOutputDir = file("build/idea-testout")
    }
}