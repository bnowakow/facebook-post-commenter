import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

// https://kotlinlang.org/docs/get-started-with-jvm-gradle-project.html#explore-the-build-script

plugins {
    kotlin("jvm") version "1.9.21"
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
    implementation("commons-io:commons-io:2.15.1")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.7")
    implementation("org.seleniumhq.selenium:selenium-chrome-driver:4.16.1")
    implementation("org.seleniumhq.selenium:selenium-firefox-driver:4.16.1")
    implementation("org.seleniumhq.selenium:selenium-safari-driver:4.16.1")
    implementation("org.seleniumhq.selenium:selenium-java:4.16.1")
    implementation("org.seleniumhq.selenium:selenium-support:4.16.1")
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