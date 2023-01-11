import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.7.21"
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "pl.bnowakowski"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("org.facebook4j:facebook4j-core:2.4.13")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")
    implementation("ch.qos.logback:logback-classic:1.4.5")
//    implementation("org.seleniumhq.selenium:selenium-chromium-driver:4.7.2")
    implementation("org.seleniumhq.selenium:selenium-chrome-driver:4.7.2")
    implementation("org.seleniumhq.selenium:selenium-java:4.7.2")
    implementation("org.seleniumhq.selenium:selenium-support:4.7.2")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
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