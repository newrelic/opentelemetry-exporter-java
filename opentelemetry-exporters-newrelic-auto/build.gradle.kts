import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "5.1.0"
}

apply(plugin = "com.github.johnrengelman.shadow")

dependencies {
    api(project(":opentelemetry-exporters-newrelic"))
    implementation("io.opentelemetry:opentelemetry-sdk-contrib-auto-config:0.3.0")
    implementation("com.google.guava:guava:20.0")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
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
    build {
        dependsOn(shadowJar)
    }
    jar {
        enabled = false
    }
    publish {
        dependsOn(shadowJar)
    }
}
