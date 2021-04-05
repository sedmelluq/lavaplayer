plugins {
  java
  id("org.springframework.boot") version "2.1.2.RELEASE"
}

version = "1.2.50"

dependencies {
  implementation(project(":main"))
  implementation("org.springframework.boot:spring-boot-starter-web:2.1.2.RELEASE")
}

tasks.bootJar {
  mainClassName = "com.sedmelluq.discord.lavaplayer.node.NodeApplication"
  archiveClassifier.set("boot")
}
