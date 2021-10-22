plugins {
    java
    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")

    implementation(project(":main"))
    implementation(project(":extensions:ip-rotator"))
    implementation("net.dv8tion:JDA:4.3.0_285")
    implementation("net.iharder:base64:2.3.9")

    runtimeOnly("ch.qos.logback:logback-classic:1.2.6")
}

application {
    mainClass.set("lavaplayer.demo.Bootstrap")
}
