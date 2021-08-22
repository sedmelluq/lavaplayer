plugins {
    java
    `maven-publish`
}

val moduleName = "lavaplayer-test-samples"
version = "1.3.11"

// Sample files are not in repository, but must be present in src/main/resources during publish. Use previous samples
// dependency JAR to obtain them.

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = moduleName
        }
    }
}
