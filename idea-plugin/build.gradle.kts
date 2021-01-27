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
    implementation(project(":compiler-plugin", configuration = "shadow"))
    implementation("io.arrow-kt:idea-plugin:1.4.10-SNAPSHOT") {
      exclude(group = "io.arrow-kt", module = "compiler-plugin")
    }
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version = "2020.2.1"
    setPlugins(
        "gradle", "gradle-java", "java", "org.jetbrains.kotlin:${KOTLIN_IDEA_VERSION}",
        "git4idea", "io.arrow-kt.arrow:1.4.10-SNAPSHOT-1606149534"
    )

    pluginsRepo {
        custom("https://meta.arrow-kt.io/idea-plugin/latest-snapshot/updatePlugins.xml")
        maven("https://plugins.jetbrains.com/maven")
    }
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