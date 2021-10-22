plugins {
    `java-library`
    `maven-publish`

    kotlin("plugin.serialization") version "1.5.31"
}

val moduleName = "lavaplayer"
version = "1.4.7_DEV.3"

dependencies {
    /* kotlin */
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.31")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")

    /* other */
    implementation("com.sedmelluq:lavaplayer-natives:1.3.15")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.jsoup:jsoup:1.14.3")
    implementation("io.github.microutils:kotlin-logging:2.0.11")

    api("org.slf4j:slf4j-api:1.7.32")
    api("com.sedmelluq:lava-common:1.1.5")
    api("org.apache.httpcomponents:httpclient:4.5.13")

    /* test */
    testImplementation("ch.qos.logback:logback-classic:1.2.6")
}

tasks.jar {
    exclude("natives")
}

val updateVersion by tasks.registering {
    File("$projectDir/src/main/resources/lavaplayer/tools/version.txt").let {
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
