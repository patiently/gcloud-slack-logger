applyKotlin()

dependencies {
    implementation(project(":slack-client"))
    implementation(project(":victorops-client"))
    implementation("com.google.code.gson:gson")
    implementation("com.google.cloud.functions:functions-framework-api:$functionsFrameworkApiVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
}
