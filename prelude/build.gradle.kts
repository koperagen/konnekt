plugins {
  kotlin("multiplatform")
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
  mingwX86()

  sourceSets {

    val commonMain by getting {
      dependencies {
        implementation(kotlin("stdlib"))
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
      }
    }

  }

}