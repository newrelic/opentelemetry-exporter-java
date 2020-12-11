import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "5.1.0"
}

apply(plugin = "com.github.johnrengelman.shadow")

dependencies {
    repositories {
        maven("https://dl.bintray.com/open-telemetry/maven")
        jcenter()
    }

    annotationProcessor("com.google.auto.service:auto-service:1.0-rc7")
    api("com.google.auto.service:auto-service-annotations:1.0-rc7")
    api(project(":opentelemetry-exporters-newrelic"))
    implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-spi:0.11.0")
    implementation("io.opentelemetry:opentelemetry-sdk:0.11.0")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
    testRuntimeOnly("org.slf4j:slf4j-simple:1.7.26")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")
    testImplementation("org.mockito:mockito-core:3.0.0")
    testImplementation("org.mockito:mockito-junit-jupiter:3.0.0")
}

tasks {
    "shadowJar"(ShadowJar::class) {
        classifier = ""
        minimize()
    }
    assemble {
        dependsOn(shadowJar)
    }
    jar {
        enabled = false
    }
    publishMavenJavaPublicationToMavenLocal {
        dependsOn(shadowJar)
    }
    publishMavenJavaPublicationToMavenRepository {
        dependsOn(shadowJar)
    }
}
