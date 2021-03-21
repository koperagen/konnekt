plugins {
  kotlin("jvm")
  `java-gradle-plugin`
  `maven-publish`
  id("com.gradle.plugin-publish") version "0.12.0"
}

repositories {
  mavenCentral()
}

val KOTLIN_VERSION: String by project
val KTOR_VERSION: String by project

gradlePlugin {
  plugins {
    create("simplePlugin") {
      id = "io.github.koperagen.greeting"
      implementationClass = "konnekt.gradle.MyPlugin"
    }

    create("konnektPlugin") {
      id = "io.github.koperagen.konnekt"
      implementationClass = "konnekt.gradle.KonnektGradlePlugin"
    }
  }
}

tasks.withType<ProcessResources> {
  filesMatching("**/plugin.properties") {
    filter {
      println(it)
      it.replace("%KTOR_VERSION%", KTOR_VERSION) }
  }
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(gradleApi())
  implementation("io.github.classgraph:classgraph:4.8.102")
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:$KOTLIN_VERSION")
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION")

  runtimeOnly(project(":compiler-plugin", "createNewPlugin"))

  testImplementation("io.kotest:kotest-framework-api:4.3.1")
  testImplementation("io.kotest:kotest-property:4.3.1")
  testImplementation("io.kotest:kotest-runner-junit5-jvm:4.3.1")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions {
    jvmTarget = "1.8"
  }
}