plugins {
  kotlin("jvm")
  `java-gradle-plugin`
  `maven-publish`
  id("com.gradle.plugin-publish") version "0.12.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

gradlePlugin {
  plugins {
    create("simplePlugin") {
      id = "org.samples.greeting"
      implementationClass = "org.example.MyPlugin"
    }

    create("konnektPlugin") {
      id = "org.samples.konnekt"
      implementationClass = "org.example.KonnektGradlePlugin"
    }
  }
}

val KOTLIN_VERSION: String by project

dependencies {
  implementation(kotlin("stdlib"))
  implementation(gradleApi())
  implementation("io.github.classgraph:classgraph:4.8.102")
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:$KOTLIN_VERSION")
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION")

  runtimeOnly(project(":compiler-plugin"))
}
