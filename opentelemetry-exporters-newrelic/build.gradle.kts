dependencies {
    api("com.newrelic.telemetry:telemetry:0.4.0")
    implementation("io.opentelemetry:opentelemetry-sdk:0.3.0")
    implementation("com.newrelic.telemetry:telemetry-http-okhttp:0.3.1")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
    testRuntimeOnly("org.slf4j:slf4j-simple:1.7.26")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")
    testImplementation("org.mockito:mockito-core:3.0.0")
    testImplementation("org.mockito:mockito-junit-jupiter:3.0.0")
    testImplementation("com.google.guava:guava:28.0-jre")
}
