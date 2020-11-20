plugins {
  kotlin("jvm")
  id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

val KOTLIN_TEST_VERSION: String by project

repositories {
//    mavenLocal()
  mavenCentral()
  jcenter()
  maven(url = "https://kotlin.bintray.com/ktor")
  maven(url = "https://jcenter.bintray.com")
  maven(url = "https://oss.jfrog.org/artifactory/oss-snapshot-local/")
}

dependencies {
  val ktor_version = "1.3.0"
  implementation(kotlin("stdlib-jdk8"))
  implementation("io.ktor:ktor-client-core:$ktor_version")
  implementation("io.ktor:ktor-client-core-jvm:$ktor_version")
  implementation("io.ktor:ktor-client-cio:$ktor_version")
  implementation("io.ktor:ktor-client-json-jvm:$ktor_version")
  implementation("io.ktor:ktor-client-jackson:$ktor_version")
  implementation("io.ktor:ktor-client-logging-jvm:$ktor_version")

  implementation("org.slf4j:slf4j-simple:1.7.30")
  implementation(kotlin("script-runtime", version = "1.4.10"))
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.4.10")
  api(project(":prelude"))
  api("io.arrow-kt:compiler-plugin:1.4.10-SNAPSHOT")
  testImplementation("io.arrow-kt:meta-test:1.4.10-SNAPSHOT")

  testImplementation("io.github.classgraph:classgraph:4.8.87")
  testImplementation("junit:junit:4.13")

  testImplementation("io.kotest:kotest-framework-api:4.3.1")
  testImplementation("io.kotest:kotest-runner-junit5-jvm:4.3.1")

  testImplementation("io.ktor:ktor-client-mock:$ktor_version")
  testImplementation("io.ktor:ktor-client-mock-jvm:$ktor_version")

  testRuntimeOnly("io.arrow-kt:arrow-meta-prelude:1.4.10-SNAPSHOT")

  testRuntimeOnly("io.ktor:ktor-client-core:$ktor_version")
  testRuntimeOnly("io.ktor:ktor-http:$ktor_version")
  testRuntimeOnly("io.ktor:ktor-client-core-jvm:$ktor_version")
  testRuntimeOnly("io.ktor:ktor-client-cio:$ktor_version")
  testRuntimeOnly("io.ktor:ktor-client-json-jvm:$ktor_version")
  testRuntimeOnly("io.ktor:ktor-client-logging-jvm:$ktor_version")
  testRuntimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.6")
}

//kotlin {
//    /* Targets configuration omitted.
//    *  To find out how to configure the targets, please follow the link:
//    *  https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#setting-up-targets */
//
//    sourceSets {
//         jvm()
//
//        val jvmMain by getting {
//            dependencies {
//
//            }
//        }
//
//        val jvmTest by getting {
//            dependencies {
//
//            }
//        }
//    }
//
//
//}

tasks.withType<Test> {
  useJUnitPlatform()
}
