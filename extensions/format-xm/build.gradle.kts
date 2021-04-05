plugins {
  `java-library`
  `maven-publish`
}

val moduleName = "lavaplayer-ext-format-xm"
version = "0.1.0"

repositories {
  maven(url = "https://dl.bintray.com/gabixdev/kyoko")
}

dependencies {
  compileOnly(project(":main"))
  implementation("com.github.micromod:ibxm:a73")
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
