import java.nio.file.Paths

plugins {
  kotlin("jvm")
  id("com.github.johnrengelman.shadow") version "5.2.0"
  `maven-publish`
}

val KOTLIN_TEST_VERSION: String by project
val KOTLIN_VERSION: String by project
val OPENAPI_VERSION: String by project
val KTOR_VERSION: String by project

repositories {
  mavenCentral()
  jcenter()
  maven(url = "https://kotlin.bintray.com/ktor")
  maven(url = "https://jcenter.bintray.com")
  maven(url = "https://oss.jfrog.org/artifactory/oss-snapshot-local/")
}

dependencies {

  compileOnly(project(":prelude"))
  compileOnly(kotlin("stdlib-jdk8"))
  compileOnly("com.intellij:openapi:$OPENAPI_VERSION")
  compileOnly("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:1.4.10")
  compileOnly("io.arrow-kt:compiler-plugin:1.4.10-SNAPSHOT")
  compileOnly("org.jetbrains.kotlin:kotlin-script-util:$KOTLIN_VERSION") {
      exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
      exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler")
      exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler-embeddable")
  }

  testImplementation(project(":prelude"))
  testImplementation("io.arrow-kt:compiler-plugin:1.4.10-SNAPSHOT")
  testImplementation("io.arrow-kt:meta-test:1.4.10-SNAPSHOT")

  testImplementation("junit:junit:4.13") // only for SampleTest
  testImplementation("io.kotest:kotest-framework-api:4.3.1")
  testImplementation("io.kotest:kotest-property:4.3.1")
  testImplementation("io.kotest:kotest-runner-junit5-jvm:4.3.1")

  // Required for integration test of plugin
  testImplementation("io.ktor:ktor-client-mock:$KTOR_VERSION")
  testImplementation("io.ktor:ktor-client-mock-jvm:$KTOR_VERSION")
  testImplementation("io.arrow-kt:arrow-meta-prelude:1.4.10-SNAPSHOT")
  testImplementation("io.ktor:ktor-client-core:$KTOR_VERSION")
  testImplementation("io.ktor:ktor-http:$KTOR_VERSION")
  testImplementation("io.ktor:ktor-client-core-jvm:$KTOR_VERSION")
  testImplementation("io.ktor:ktor-client-cio:$KTOR_VERSION")
  testImplementation("io.ktor:ktor-client-json-jvm:$KTOR_VERSION")
  testImplementation("io.ktor:ktor-client-jackson:$KTOR_VERSION")
  testImplementation("io.ktor:ktor-client-logging-jvm:$KTOR_VERSION")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.6")
  testImplementation("org.slf4j:slf4j-simple:1.7.30")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

val createNewPlugin = tasks.create<Jar>("createNewPlugin") {
    archiveBaseName.set("konnekt-plugin")
    dependsOn("classes")
    from("build/classes/kotlin/main")
    from("build/resources/main")
    from(
        zipTree(sourceSets.main.get().compileClasspath.find {
          it.absolutePath.contains(Paths.get("konnekt", "prelude").toString())
        }!!)
    )
    from(
        zipTree(sourceSets.main.get().compileClasspath.find {
            it.absolutePath.contains(Paths.get("arrow-kt", "compiler-plugin").toString())
        }!!)
    ) {
        exclude("META-INF/services/org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar")
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
  configurations = listOf(project.configurations.compileOnly.get())
  relocate("org.jetbrains.kotlin.com.intellij", "com.intellij")
  dependencies {
    exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
    // and its transitive dependencies:
    exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib-common"))
    exclude(dependency("org.jetbrains:annotations"))

    exclude(dependency("com.intellij:openapi"))
    // and its transitive dependencies:
    exclude(dependency("com.intellij:extensions"))
    exclude(dependency("com.intellij:annotations"))
  }
}

val conf = configurations.create("createNewPlugin")
val pluginArtifact = artifacts.add(conf.name, createNewPlugin)

publishing {
  publications {
    val plugin by creating(MavenPublication::class.java) {
      artifactId = "konnekt-plugin"
      artifact(pluginArtifact)
    }
  }
}
