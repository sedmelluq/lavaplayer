plugins {
    groovy
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin", version = "1.5.30"))
    implementation(kotlin("serialization", version = "1.5.30"))
}
