rootProject.name = "internal-compatibility-matrix-tests"

pluginManagement {
    val kotlinVersion: String by extra
    val kotlinMultiplatformVersion: String by extra
    val shadowPluginVersion: String by extra
    val licenseVersion: String by extra

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("multiplatform") version kotlinMultiplatformVersion
        kotlin("plugin.serialization") version kotlinVersion

        id("com.github.johnrengelman.shadow") version shadowPluginVersion
        id("com.github.hierynomus.license") version licenseVersion
    }

    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

val sharedLibsLocalPath: String by extra
val includeSharedLib: Settings.(String) -> Unit = {
    include(it)
    project(":$it").projectDir = file(sharedLibsLocalPath).resolve(it)
}

includeSharedLib("logging-native")
includeSharedLib("knasm")
includeSharedLib("jvmapi")
includeSharedLib("common")
includeSharedLib("logging")
includeSharedLib("agent-instrumentation")
include("spring-jetty")
include("spring-tomcat")
include("spring-undertow")
include("spring-webflux-jetty")
include("spring-webflux-netty")
include("spring-webflux-tomcat")
include("spring-webflux-undertow")
include("abstract-test")
include("test-agent")
