import java.net.URI

plugins {
    kotlin("jvm")
    id("com.github.hierynomus.license")
}

version = rootProject.version
group = rootProject.group

repositories {
    mavenCentral()
}

val microutilsLoggingVersion: String by parent!!.extra
val serverVersion = "11.0.0-M18"

dependencies {
    implementation("io.github.microutils:kotlin-logging-jvm:$microutilsLoggingVersion")
    implementation("org.apache.tomcat:tomcat-websocket:$serverVersion")
    implementation("org.apache.tomcat:tomcat-api:$serverVersion")
    testImplementation(kotlin("test-junit"))
    testImplementation(project(":common-test"))
    testImplementation("org.glassfish.tyrus:tyrus-server:1.20")
    testImplementation("org.glassfish.tyrus:tyrus-container-grizzly-server:1.20")
}

license {
    headerURI = URI("https://raw.githubusercontent.com/Drill4J/drill4j/develop/COPYRIGHT")
    include("**/*.kt")
    include("**/*.java")
    include("**/*.groovy")
}
