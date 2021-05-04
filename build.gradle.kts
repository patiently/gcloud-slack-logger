allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configurations.all {
        resolutionStrategy {
            failOnVersionConflict()
        }
    }

    apply(plugin = "idea")

    extensions.getByType<org.gradle.plugins.ide.idea.model.IdeaModel>().apply {
        module {
            outputDir = file("build/idea-out")
            testOutputDir = file("build/idea-testout")
        }
    }

    repositories {
        mavenLocal()
        jcenter()
        mavenCentral()
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        options.apply {
            isWarnings = true
        }
    }
}

group = "io.patiently.cloud"
