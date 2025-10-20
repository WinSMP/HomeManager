import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

plugins {
    id("com.gradleup.shadow") version "9.2.2"
    java
}

group = "org.winlogon.homemanager"


fun getTime(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HHmmss'Z'").apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return sdf.format(Date()).toString()
}

val shortVersion: String? = if (project.hasProperty("ver")) {
    val ver: String = project.property("ver") as String
    if (ver.startsWith("v")) {
        ver.substring(1).uppercase()
    } else {
        ver.uppercase()
    }
} else {
    null
}

version = when {
    shortVersion.isNullOrBlank() -> "${getTime()}-SNAPSHOT"
    shortVersion.contains("-RC-") -> shortVersion.substringBefore("-RC-") + "-SNAPSHOT"
    else -> shortVersion
}

val minecraftBase = rootProject.extra["minecraftBase"] as String
val minecraftPatch = rootProject.extra["minecraftPatch"] as String
val projectDescription = rootProject.extra["projectDescription"] as String

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()

    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
        content {
            includeModule("io.papermc.paper", "paper-api")
            includeModule("net.md-5", "bungeecord-chat")
        }
    }

    maven {
        name = "minecraft"
        url = uri("https://libraries.minecraft.net")
        content {
            includeModule("com.mojang", "brigadier")
        }
    }

    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-snapshots/")
    maven("https://artifactory.papermc.io/artifactory/universe/")
}

dependencies {
    implementation("dev.jorel:commandapi-paper-shade:11.0.0")
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")

    compileOnly("com.github.walker84837:JResult:1.4.0")
    compileOnly("org.postgresql:postgresql:42.7.8")
    compileOnly("net.kyori:adventure-api:4.25.0")
    compileOnly("org.xerial:sqlite-jdbc:3.45.3.0")

    testImplementation("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    testImplementation("net.kyori:adventure-api:4.25.0")
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    filesMatching("**/paper-plugin.yml") {
        expand(mapOf(
            "NAME" to rootProject.name,
            "VERSION" to version,
            "PACKAGE" to project.group.toString(),
            "DESCRIPTION" to projectDescription,
            "MCVERSION" to minecraftBase,
        ))
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    minimize()
    relocate("dev.jorel.commandapi", "org.winlogon.homemanager.commandapi")
}

tasks.jar {
    enabled = false
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.register("printProjectName") {
    doLast {
        println(rootProject.name)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("--enable-preview")
}

tasks.register("release") {
    dependsOn(tasks.build)

    doLast {
        if (!version.toString().endsWith("-SNAPSHOT")) {
            val shadowJarFile = tasks.shadowJar.get().archiveFile.get().asFile
            val newFile = layout.buildDirectory.file("libs/${rootProject.name}.jar").get().asFile
            shadowJarFile.renameTo(newFile)
        }
    }
}
