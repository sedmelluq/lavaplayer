import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    `maven-publish`

    id("kotlinx-atomicfu") version "0.16.3" apply false
    kotlin("jvm")
}

group = "com.sedmelluq"

allprojects {
    group = rootProject.group

    repositories {
        maven("https://dimensional.jfrog.io/artifactory/maven")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://m2.dv8tion.net/releases")
        mavenLocal()
        mavenCentral()
    }

    apply(plugin = "kotlinx-atomicfu")
    apply(plugin = "kotlin")
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
        }
    }
}
