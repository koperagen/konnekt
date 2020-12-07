import java.nio.file.Paths

plugins {
  kotlin("jvm")
  id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

val KOTLIN_TEST_VERSION: String by project

repositories {
  mavenCentral()
  jcenter()
  maven(url = "https://kotlin.bintray.com/ktor")
  maven(url = "https://jcenter.bintray.com")
  maven(url = "https://oss.jfrog.org/artifactory/oss-snapshot-local/")
}

dependencies {
  val ktor_version = "1.3.0"
  implementation(kotlin("stdlib-jdk8"))

  implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.4.10")
  api(project(":prelude"))
  api("io.arrow-kt:compiler-plugin:1.4.10-SNAPSHOT")
  testImplementation("io.arrow-kt:meta-test:1.4.10-SNAPSHOT")

  testImplementation("junit:junit:4.13") // only for SampleTest
  testImplementation("io.kotest:kotest-framework-api:4.3.1")
  testImplementation("io.kotest:kotest-runner-junit5-jvm:4.3.1")

  // Required for integration test of plugin
  testImplementation("io.ktor:ktor-client-mock:$ktor_version")
  testImplementation("io.ktor:ktor-client-mock-jvm:$ktor_version")
  testImplementation("io.arrow-kt:arrow-meta-prelude:1.4.10-SNAPSHOT")
  testImplementation("io.ktor:ktor-client-core:$ktor_version")
  testImplementation("io.ktor:ktor-http:$ktor_version")
  testImplementation("io.ktor:ktor-client-core-jvm:$ktor_version")
  testImplementation("io.ktor:ktor-client-cio:$ktor_version")
  testImplementation("io.ktor:ktor-client-json-jvm:$ktor_version")
  testImplementation("io.ktor:ktor-client-jackson:$ktor_version")
  testImplementation("io.ktor:ktor-client-logging-jvm:$ktor_version")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.6")
  testImplementation("org.slf4j:slf4j-simple:1.7.30")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.withType<Jar> {
  from(
      zipTree(sourceSets.main.get().compileClasspath.find {
        it.absolutePath.contains(Paths.get("arrow-kt", "compiler-plugin").toString())
      }!!)
  ) {
    exclude("META-INF/services/org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar")
  }
}
