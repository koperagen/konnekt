plugins {
  kotlin("jvm")
  `java-gradle-plugin`
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(gradleApi())
  implementation("io.github.classgraph:classgraph:4.8.102")
}
