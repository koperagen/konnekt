plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

val ktor_version = "1.4.1"

dependencies {
    implementation(kotlin("stdlib"))

    testImplementation(project(":prelude"))

    testImplementation("io.ktor:ktor-client-core:$ktor_version")
    testImplementation("io.ktor:ktor-client-mock:$ktor_version")
    testImplementation("io.ktor:ktor-client-mock-jvm:$ktor_version")

    testImplementation("io.kotest:kotest-framework-api:4.3.1")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.3.1")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

kotlin.target {
  compilations.all {
    kotlinOptions {
      freeCompilerArgs = listOf(
          "-Xplugin=${rootDir}/compiler-plugin/build/libs/konnekt-plugin-1.4.10-SNAPSHOT.jar"
      )
    }
  }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  dependsOn(":compiler-plugin:createNewPlugin")
}