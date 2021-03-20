package konnekt.gradle

import io.kotest.assertions.asClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

class KonnektPluginTest : FreeSpec({

  val temp = tempdir()
  val buildFile = temp.newFile("build.gradle.kts")

  fun GradleRunner.defaultConfig() {
    withProjectDir(temp)
    withPluginClasspath()
  }

  "simple plugin runs" {
    val result = GradleRunner {
      defaultConfig()
      buildFile.writeText(
          """
          |plugins {
          |    kotlin("jvm") version "1.4.21"
          |    id("io.github.koperagen.greeting")
          |}
          |
          |group = "org.example"
          |version = "1.0-SNAPSHOT"
          |
          |repositories {
          |    mavenLocal()
          |    mavenCentral()
          |}
          |
          |dependencies {
          |    implementation(kotlin("stdlib"))
          |}
          """.trimMargin()
      )
      withArguments("Hello")
    }

    result.asClue {
      it.task(":Hello")?.outcome shouldBe TaskOutcome.SUCCESS
    }
  }

  "sample project builds" {
    val result = GradleRunner {
      defaultConfig()
      buildFile.writeText("""
        |plugins {
        |    kotlin("jvm") version "1.4.21"
        |}
        |
        |group = "org.example"
        |version = "1.0-SNAPSHOT"
        |
        |repositories {
        |    mavenCentral()
        |}
        |
        |dependencies {
        |    implementation(kotlin("stdlib"))
        |}
      """.trimMargin())
      withArguments("build")
    }

    result.asClue {
      it.task(":build")?.outcome shouldBe TaskOutcome.SUCCESS
    }
  }

  "project with plugin" {
    val result = GradleRunner {
      defaultConfig()
      val src = projectDir.resolve("src").let {
        it.mkdir()
        it.resolve("main").let {
          it.mkdir()
          it.resolve("kotlin").let {
            it.mkdir()
            it.resolve("Main.kt")
          }
        }
      }
      src.writeText("""
        |import konnekt.prelude.Client
        |import io.ktor.client.HttpClient
        |
        |@Client
        |interface Test {
        |
        |   companion object
        |}
        |
        |val api = Test(HttpClient())
      """.trimMargin())
      buildFile.writeText("""
        |plugins {
        |    kotlin("jvm") version "1.4.10"
        |    id("io.github.koperagen.konnekt")
        |}
        |
        |group = "org.example"
        |version = "1.0-SNAPSHOT"
        |
        |repositories {
        |    mavenLocal()
        |    mavenCentral()
        |}
        |
        |dependencies {
        |    implementation(kotlin("stdlib"))
        |    implementation("io.github.koperagen:prelude-jvm:1.4.10-SNAPSHOT")
        |}
      """.trimMargin())

      withArguments("build")
      withDebug(true)
      withGradleVersion("6.3")
    }

    result.asClue {
      it.task(":build")?.outcome shouldBe TaskOutcome.SUCCESS
    }
  }
})

fun GradleRunner(config: GradleRunner.() -> Unit): BuildResult = GradleRunner.create().apply(config).build()