buildscript {
    dependencies {
        classpath("gradle.plugin.com.github.sherter.google-java-format:google-java-format-gradle-plugin:0.8")
    }
}
plugins {
    java
}

apply(plugin = "java")
apply(plugin = "java-library")
apply(plugin = "maven-publish")
apply(plugin = "signing")

apply(plugin = "com.github.sherter.google-java-format")

allprojects {
    group = "com.newrelic.telemetry"

    val release: String? by project
    version = project.findProperty("baseVersion") as String + if("true" == release) "" else "-SNAPSHOT"

    repositories {
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
        // this is only needed for the working against unreleased otel-java snapshots
        maven("https://oss.jfrog.org/artifactory/oss-snapshot-local")
    }
    tasks.withType<JavaCompile>().configureEach {
        // compile all projects with Java 8
        javaCompiler.set(javaToolchains.compilerFor {
            languageVersion.set(JavaLanguageVersion.of(8))
        })
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
        // run tests in all projects with Java 11
        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(11))
        })
    }
}

listOf(":opentelemetry-exporters-newrelic", ":opentelemetry-exporters-newrelic-auto").forEach {
    project(it) {
        apply(plugin = "java-library")
        apply(plugin = "maven-publish")
        apply(plugin = "signing")

        tasks {
            val taskScope = this
            val sources = sourceSets

            val sourcesJar by creating(Jar::class) {
                dependsOn(JavaPlugin.CLASSES_TASK_NAME)
                archiveClassifier.set("sources")
                from(sources.main.get().allSource)
            }

            val javadocJar by creating(Jar::class) {
                dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
                archiveClassifier.set("javadoc")
                from(taskScope.javadoc)
            }

            val jar: Jar by taskScope
            jar.apply {
                manifest.attributes["Implementation-Version"] = project.version
                manifest.attributes["Implementation-Vendor"] = "New Relic, Inc"
            }
        }
        val useLocalSonatype = project.properties["useLocalSonatype"] == "true"

        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("mavenJava") {
                    artifact(file("$buildDir/libs/${project.name}-${project.version}.jar"))
                    artifact(tasks["sourcesJar"])
                    artifact(tasks["javadocJar"])
                    pom {
                        name.set(project.name)
                        description.set("Open Telemetry Java Exporters that send data to New Relic ingest.")
                        url.set("https://github.com/newrelic/opentelemetry-exporters-newrelic")
                        licenses {
                            license {
                                name.set("The Apache License, Version 2.0")
                                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                                distribution.set("repo")
                            }
                        }
                        developers {
                            developer {
                                id.set("newrelic")
                                name.set("New Relic")
                                email.set("opensource@newrelic.com")
                            }
                        }
                        scm {
                            url.set("git@github.com:newrelic/opentelemetry-exporters-newrelic.git")
                            connection.set("scm:git@github.com:newrelic/opentelemetry-exporters-newrelicc.git")
                        }
                    }
                }
            }
            repositories {
                maven {
                    if (useLocalSonatype) {
                        val releasesRepoUrl = uri("http://localhost:8081/repository/maven-releases/")
                        val snapshotsRepoUrl = uri("http://localhost:8081/repository/maven-snapshots/")
                        url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
                    } else {
                        val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                        val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots/")
                        url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
                        configure<SigningExtension> {
                            val signingKey: String? by project
                            val signingPassword: String? by project
                            useInMemoryPgpKeys(signingKey, signingPassword)
                            sign(publications["mavenJava"])
                        }
                    }
                    credentials {
                        username = System.getenv("SONATYPE_USERNAME")
                        password = System.getenv("SONATYPE_PASSWORD")
                    }
                }
            }
        }
    }
}
