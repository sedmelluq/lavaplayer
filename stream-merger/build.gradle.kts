plugins {
  `java-library`
  `maven-publish`
}

val moduleName = "lavaplayer-stream-merger"
version = "0.1.0"

dependencies {
  implementation(project(":main"))
  implementation("org.slf4j:slf4j-api:1.7.32")
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
