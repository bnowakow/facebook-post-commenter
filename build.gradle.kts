import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val selenium_version: String = "4.32.0"

// https://kotlinlang.org/docs/get-started-with-jvm-gradle-project.html#explore-the-build-script

plugins {
    kotlin("jvm") version "2.0.0"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "pl.bnowakowski"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.facebook4j:facebook4j-core:2.4.13")
    implementation("com.restfb:restfb:2025.9.0")
    implementation("commons-io:commons-io:2.19.0")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("org.seleniumhq.selenium:selenium-chrome-driver:$selenium_version")
    implementation("org.seleniumhq.selenium:selenium-firefox-driver:$selenium_version")
    implementation("org.seleniumhq.selenium:selenium-safari-driver:$selenium_version")
    implementation("org.seleniumhq.selenium:selenium-java:$selenium_version")
    implementation("org.seleniumhq.selenium:selenium-support:$selenium_version")
    // TODO migrate to https://mvnrepository.com/artifact/io.github.oshai/kotlin-logging
    // https://github.com/oshai/kotlin-logging?tab=readme-ov-file#version-5-vs-previous-versions
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

//tasks.withType<KotlinCompile> {
//    kotlinOptions.jvmTarget = "1.8"
//}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("MainKt")
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("shadow")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "pl.bnowakowski.Main"))
        }
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}
