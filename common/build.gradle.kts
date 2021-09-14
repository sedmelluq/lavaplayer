plugins {
    `java-library`
    `maven-publish`
}

val moduleName = "lava-common"
version = "1.1.5"

dependencies {
    implementation("io.github.microutils:kotlin-logging:1.12.5")
    implementation("commons-io:commons-io:2.11.0")

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
