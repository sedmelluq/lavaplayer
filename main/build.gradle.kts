import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `java-library`
  `maven-publish`

  kotlin("jvm") version "1.5.21"
  kotlin("plugin.serialization") version "1.5.21"
}

val moduleName = "lavaplayer"
version = "1.3.82"

dependencies {
  api("com.sedmelluq:lava-common:1.1.2")
  implementation("com.sedmelluq:lavaplayer-natives:1.3.14")
  implementation("org.slf4j:slf4j-api:1.7.32")

  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")

  api("org.apache.httpcomponents:httpclient:4.5.13")
  implementation("commons-io:commons-io:2.11.0")

  implementation("org.jsoup:jsoup:1.14.2")

  testImplementation("ch.qos.logback:logback-classic:1.2.5")
  testImplementation("com.sedmelluq:lavaplayer-test-samples:1.3.11")
}

tasks.jar {
  exclude("natives")
}

val updateVersion by tasks.registering {
  File("$projectDir/src/main/resources/com/sedmelluq/discord/lavaplayer/tools/version.txt").let {
    it.parentFile.mkdirs()
    it.writeText(version.toString())
  }
}

tasks.classes.configure {
  finalizedBy(updateVersion)
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

tasks.withType<KotlinCompile> {
  sourceCompatibility = "16"
  targetCompatibility = "16"
  kotlinOptions.jvmTarget = "16"
}
