import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.closureOf
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension

fun Project.kotlinSetup() {
    apply(plugin = "kotlin")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    dependencies {
        implementation(enforcedPlatform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$kotlinCoroutineVersion"))
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutineVersion")
        implementation("org.kodein.di:kodein-di:$kodeinVersion")
        implementation("com.google.code.gson:gson:$gsonVersion")
        testImplementation(platform("org.junit:junit-bom:$jUnitVersion"))
        testImplementation("org.junit.jupiter:junit-jupiter")

    }

    configurations.all {
        resolutionStrategy {
            failOnVersionConflict()
            force(
                "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion",
                "org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion",
                "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion"
            )
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            languageVersion = "1.8"
            apiVersion = "1.8"
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=enable")
        }
    }

    extensions.getByType(KtlintExtension::class.java).apply {
        debug.set(false)
        verbose.set(true)
        android.set(false)
        ignoreFailures.set(false)
        disabledRules.set(listOf("final-newline"))
    }

    val sourceJar by tasks.registering(Jar::class) {
        from(sourceSets["main"].allJava)
        archiveClassifier.set("source")
    }
    val assemble by tasks
    assemble.dependsOn(sourceJar)
}

fun DependencyHandler.testImplementation(dependencyNotation: Any): Dependency =
    add("testImplementation", dependencyNotation)!!

fun DependencyHandler.testImplementation(
    dependencyNotation: Any,
    configureClosure: ModuleDependency.() -> Unit
): Dependency =
    add("testImplementation", dependencyNotation, closureOf(configureClosure))

fun DependencyHandler.implementation(dependencyNotation: Any): Dependency =
    add("implementation", dependencyNotation)!!

fun DependencyHandler.implementation(
    dependencyNotation: Any,
    configureClosure: ModuleDependency.() -> Unit
): Dependency =
    add("implementation", dependencyNotation, closureOf(configureClosure))

fun DependencyHandler.api(dependencyNotation: Any): Dependency =
    add("api", dependencyNotation)!!

fun DependencyHandler.api(dependencyNotation: Any, configureClosure: ModuleDependency.() -> Unit): Dependency =
    add("api", dependencyNotation, closureOf(configureClosure))

val Project.sourceSets: org.gradle.api.tasks.SourceSetContainer
    get() =
        (this as org.gradle.api.plugins.ExtensionAware).extensions.getByName("sourceSets") as org.gradle.api.tasks.SourceSetContainer


fun Project.applyKotlin() = kotlinSetup()