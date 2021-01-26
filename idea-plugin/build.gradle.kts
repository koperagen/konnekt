plugins {
    id("org.jetbrains.intellij") version "0.5.0"
    kotlin("jvm")
}

group = "org.example"
version = "1.0-SNAPSHOT"

val KOTLIN_IDEA_VERSION: String by project

repositories {
//    mavenLocal()
    mavenCentral()
    maven(url = "https://dl.bintray.com/celtric/maven")
    maven(url = "https://oss.jfrog.org/artifactory/oss-snapshot-local/")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api(project(":compiler-plugin", configuration = "shadow"))
    api("io.arrow-kt:idea-plugin:1.4.10-SNAPSHOT")
    api("io.arrow-kt:compiler-plugin:1.4.10-SNAPSHOT")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version = "2020.2.1"
    setPlugins("gradle", "gradle-java", "java", "org.jetbrains.kotlin:${KOTLIN_IDEA_VERSION}", "git4idea")
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