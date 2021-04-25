plugins {
    id("org.jetbrains.intellij") version "0.6.5"
    kotlin("jvm")
}

val KOTLIN_IDEA_VERSION: String by project

repositories {
//    mavenLocal()
    mavenCentral()
//    maven(url = "https://dl.bintray.com/celtric/maven")
//    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
//    implementation("io.arrow-kt:idea-plugin:1.4.10-SNAPSHOT") {
//      exclude(group = "io.arrow-kt", module = "compiler-plugin")
//    }
    implementation(project(":prelude"))
    testImplementation("io.kotest:kotest-assertions-core:4.3.1")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
  version = "2020.3"
  setPlugins("java", "org.jetbrains.kotlin")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}
tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
    changeNotes("""
      Add change notes here.<br>
      <em>most HTML tags may be used</em>""")
}