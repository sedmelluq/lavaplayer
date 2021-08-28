plugins {
    java
    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")

    implementation(project(":main"))
    implementation("net.dv8tion:JDA:4.3.0_285")
    implementation("net.iharder:base64:2.3.9")

    runtimeOnly("ch.qos.logback:logback-classic:1.1.8")
}

application {
    mainClass.set("lavaplayer.demo.Bootstrap")
}
