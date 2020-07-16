dependencies {
    val newRelicTelemetrySdkVersion = "0.6.1"
    val openTelemetryVersion = "0.6.0"

    api("com.newrelic.telemetry:telemetry:$newRelicTelemetrySdkVersion")
    implementation("org.slf4j:slf4j-api:1.7.26")
    implementation("com.newrelic.telemetry:telemetry-http-okhttp:$newRelicTelemetrySdkVersion")
    implementation("io.opentelemetry:opentelemetry-sdk:$openTelemetryVersion")

    testRuntimeOnly("org.slf4j:slf4j-simple:1.7.26")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")
    testImplementation("org.mockito:mockito-core:3.0.0")
    testImplementation("org.mockito:mockito-junit-jupiter:3.0.0")
    testImplementation("com.google.guava:guava:28.0-jre")
}

tasks {
    val propertiesDir = "build/generated/properties"
    val versionFilename = "newrelic.exporter.version"
    sourceSets.get("main").output.dir(mapOf("builtBy" to "generateVersionResource"), propertiesDir)
    register("generateVersionResource") {
        outputs.file(File("$propertiesDir/$versionFilename"))
        doLast {
            val folder = file(propertiesDir)
            folder.mkdirs()
            val propertiesFile = File(folder.getAbsolutePath(), versionFilename)
            propertiesFile.writeText("${project.version}")
        }
    }

    build {
        dependsOn("generateVersionResource")
    }
}


