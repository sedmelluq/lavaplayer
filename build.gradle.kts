plugins {
  java
  `maven-publish`
}

group = "com.sedmelluq"

allprojects {
  group = rootProject.group

  repositories {
    mavenLocal()
    mavenCentral()
    maven(url = "https://m2.dv8tion.net/releases")
  }

  apply(plugin = "java")
  apply(plugin = "maven-publish")

  java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
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
}
