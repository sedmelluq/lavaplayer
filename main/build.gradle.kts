plugins {
    `java-library`
    `maven-publish`

    kotlin("plugin.serialization") version "1.5.30"
}

val moduleName = "lavaplayer"
version = "1.4.6"

dependencies {
    /* kotlin */
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.30")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")

    /* other */
    implementation("com.sedmelluq:lavaplayer-natives:1.3.15")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("org.jsoup:jsoup:1.14.2")

    api("com.sedmelluq:lava-common:1.1.4")
    api("org.apache.httpcomponents:httpclient:4.5.13")

    /* test */
    testImplementation("ch.qos.logback:logback-classic:1.1.8")
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
