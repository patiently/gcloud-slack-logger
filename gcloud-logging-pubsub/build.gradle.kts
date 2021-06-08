applyKotlin()

plugins {
    id("com.github.johnrengelman.shadow") version "6.1.0"
    application
}

application {
    mainClass.set("io.patiently.gcloud.pubsub.PubSubEventListenerKt")
}

project.setProperty("mainClassName", "io.patiently.gcloud.pubsub.PubSubEventListenerKt")

dependencies {
    implementation(project(":slack-client"))
    implementation(project(":victorops-client"))
    implementation("com.google.code.gson:gson")
    implementation("com.google.cloud.functions:functions-framework-api:$functionsFrameworkApiVersion")
    implementation("dnsjava:dnsjava:$dnsJavaVersion")
    implementation("com.google.guava:guava:$guavaVersion")
}

tasks.named("build") {
    dependsOn("buildFunction")
}

task("buildFunction") {
    dependsOn("shadowJar")
    copy {
        from("build/libs/${project.name}-${project.version}-all.jar")
        into("build/deploy")
    }
}

tasks.register("deployFunction") {
    dependsOn("buildFunction")
    doLast {
        val envFile = file("env.yaml")
        val yml = org.yaml.snakeyaml.Yaml().loadAs(envFile.readText(), Map::class.java)
        val projectId = yml["projectId"]
        val pubSub = yml["pubSub"]
        exec {
            commandLine(
                listOf(
                    "/usr/bin/env",
                    "gcloud",
                    "functions",
                    "deploy",
                    "gcloud-slack-logger",
                    "--entry-point=io.patiently.gcloud.pubsub.PubSubEventListener",
                    "--runtime=java11",
                    "--source=build/deploy",
                    "--trigger-topic=$pubSub",
                    "--project=$projectId",
                    "--env-vars-file=$envFile"
                )
            )
        }
    }
}
