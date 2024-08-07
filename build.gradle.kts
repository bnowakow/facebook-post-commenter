import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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
    implementation("com.restfb:restfb:2024.9.0")
    implementation("commons-io:commons-io:2.16.1")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("org.seleniumhq.selenium:selenium-chrome-driver:4.22.0")
    implementation("org.seleniumhq.selenium:selenium-firefox-driver:4.22.0")
    implementation("org.seleniumhq.selenium:selenium-safari-driver:4.22.0")
    implementation("org.seleniumhq.selenium:selenium-java:4.22.0")
    implementation("org.seleniumhq.selenium:selenium-support:4.22.0")
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
    jvmToolchain(17)
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