plugins {
    `java-library`
    `maven-publish`
}

val moduleName = "lavaplayer-ext-ip-rotator"
version = "0.2.7"

dependencies {
    compileOnly(project(":main"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
    api("org.slf4j:slf4j-api:1.7.32")
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = moduleName
            artifact(sourcesJar)
        }
    }
}
