import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName
import java.net.URI

plugins {
    kotlin("jvm")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("com.github.hierynomus.license")
}

group = "com.epam.drill.compatibility"
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.current().toString()
}

version = rootProject.version

repositories {
    mavenCentral()
}

val microutilsLoggingVersion: String by parent!!.extra

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("io.projectreactor:reactor-test")
    implementation("io.github.microutils:kotlin-logging-jvm:$microutilsLoggingVersion")
    testImplementation(project(":abstract-test"))
    configurations {
        all {
            exclude(group = "ch.qos.logback", module = "logback-classic")
        }
    }
    evaluationDependsOn(":test-agent")
}

val nativeAgentLibName: String by parent!!.extra

tasks {
    test {
        val pathToBinary: String
        val pathToRuntimeJar: String

        val property = providers.systemProperty("test-agent.binaries")
        if (property.isPresent) {
            val binaries = property.get()
            val fileName = when {
                HostManager.hostIsMingw -> "drill_agent.dll"
                HostManager.hostIsMac -> "libdrill_agent.dylib"
                else -> "libdrill_agent.so"
            }
            pathToBinary = "$binaries/$fileName"
            pathToRuntimeJar = "$binaries/drill-runtime.jar"
        } else {
            val kotlinTargets = (project(":test-agent").extensions.getByName("kotlin") as KotlinMultiplatformExtension)
                .targets.withType<KotlinNativeTarget>().getByName(HostManager.host.presetName)
                .binaries.getSharedLib(nativeAgentLibName, NativeBuildType.DEBUG)
            pathToBinary = kotlinTargets.outputFile.path
            pathToRuntimeJar = project(":test-agent").tasks.named("runtimeJar").get().outputs.files.singleFile.path

            dependsOn(project(":test-agent").tasks["runtimeJar"])
            dependsOn(kotlinTargets.linkTask)
        }
        jvmArgs = listOf(
            "-agentpath:$pathToBinary=$pathToRuntimeJar"
        )
    }
    this["bootJar"].enabled = false
    licenseTest.get().enabled = false
}

@Suppress("UNUSED_VARIABLE")
license {
    headerURI = URI("https://raw.githubusercontent.com/Drill4J/drill4j/develop/COPYRIGHT")
    val licenseFormatSources by tasks.registering(LicenseFormat::class) {
        source = fileTree("$projectDir/src").also {
            include("**/*.kt", "**/*.java", "**/*.groovy")
            exclude("**/commonGenerated")
        }
    }
    val licenseCheckSources by tasks.registering(LicenseCheck::class) {
        source = fileTree("$projectDir/src").also {
            include("**/*.kt", "**/*.java", "**/*.groovy")
            exclude("**/commonGenerated")
        }
    }
}