plugins {
    java
    application
}

dependencies {
    implementation(project(":main"))
    implementation("net.dv8tion:JDA:4.3.0_285")
    implementation("net.iharder:base64:2.3.9")
    runtimeOnly("ch.qos.logback:logback-classic:1.1.8")
}

application {
    mainClass.set("com.sedmelluq.discord.lavaplayer.demo.Bootstrap")
}
