plugins {
  `java-library`
  `maven-publish`
}

val moduleName = "lavaplayer-ext-format-xm"
version = "0.1.0"

repositories {
  ivy {
    url = uri("https://github.com/")
    patternLayout {
      artifact("martincameron/micromod/raw/master/[module]-[revision].[ext]")
    }
    metadataSources { artifact() }
  }
}

dependencies {
  compileOnly(project(":main"))
  implementation(":ibxm:a74:jar")
  implementation("org.slf4j:slf4j-api:1.7.32")
}

val sourcesJar by tasks.registering(Jar::class) {
  archiveClassifier.set("sources")
  from(sourceSets["main"].allSource)
}

val uberJar by tasks.registering(Jar::class) {
  archiveClassifier.set("uber")

  from(sourceSets.main.get().output)

  dependsOn(configurations.runtimeClasspath)
  from({
    configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
  })
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])
      artifactId = moduleName
      artifact(sourcesJar)
      artifact(uberJar)
    }
  }
}
