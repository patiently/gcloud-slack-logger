allprojects {
    configurations.all {
        resolutionStrategy {
            failOnVersionConflict()
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
