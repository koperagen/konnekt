plugins {
  kotlin("multiplatform")
  id("org.jetbrains.dokka") version "1.4.10"
  `maven-publish`
}

repositories {
  mavenCentral()
  jcenter()
}

publishing {
  val username by lazy { System.getenv("KONNEKT_MAVEN_USERNAME") }
  val password by lazy { System.getenv("KONNEKT_MAVEN_PASSWORD") }

  repositories {
    maven {
      url = uri("https://maven.pkg.jetbrains.space/klimenko/p/konnekt/maven")
      credentials {
        setUsername(username)
        setPassword(password)
      }
    }
  }
}

kotlin {
  /* Targets configuration omitted.
  *  To find out how to configure the targets, please follow the link:
  *  https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#setting-up-targets */
  jvm()
  js {
    browser()
  }
  linuxX64()
  mingwX64()

  sourceSets {

    val KTOR_VERSION: String by project

    val commonMain by getting {
      dependencies {
        implementation(kotlin("stdlib"))
        api("io.ktor:ktor-client-core:$KTOR_VERSION")
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
      }
    }

  }

}