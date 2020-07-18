plugins {
    kotlin("jvm") version "1.3.61"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

val KOTLIN_TEST_VERSION: String by project

repositories {
//    mavenLocal()
    mavenCentral()
    maven(url="https://oss.jfrog.org/artifactory/oss-snapshot-local/")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api("io.arrow-kt:compiler-plugin:1.3.61-SNAPSHOT")

    testImplementation("io.github.classgraph:classgraph:4.8.87")
    testImplementation("junit:junit:4.13")
    testImplementation("io.arrow-kt:testing-plugin:1.3.61-SNAPSHOT")
    testRuntimeOnly("io.arrow-kt:arrow-meta-prelude:1.3.61-SNAPSHOT")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    configurations = listOf(project.configurations.compile.get())
    relocate("org.jetbrains.kotlin.com.intellij", "com.intellij")
}