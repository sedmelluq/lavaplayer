import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    `maven-publish`
    kotlin("jvm") version "1.5.30"
}

group = "com.sedmelluq"

allprojects {
    group = rootProject.group

    repositories {
        maven("https://dimensional.jfrog.io/artifactory/maven")
        mavenLocal()
        mavenCentral()
        maven("https://m2.dv8tion.net/releases")
    }

    apply(plugin = "kotlin")
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    java {
        sourceCompatibility = JavaVersion.VERSION_16
        targetCompatibility = JavaVersion.VERSION_16
    }

    publishing {
        repositories {
            maven {
                setUrl("https://dimensional.jfrog.io/artifactory/maven")
                credentials {
                    username = System.getenv("JFROG_USERNAME")?.toString()
                    password = System.getenv("JFROG_PASSWORD")?.toString()
                }
            }
        }
    }

    tasks.withType<KotlinCompile> {
        sourceCompatibility = "16"
        targetCompatibility = "16"
        kotlinOptions {
            jvmTarget = "16"
            freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
        }
    }
}
