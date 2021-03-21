package konnekt.gradle

import io.kotest.assertions.asClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

class KonnektTest : FreeSpec({
  val temp = tempdir()
  val buildFile = temp.newFile("build.gradle.kts")

  fun GradleRunner.defaultConfig() {
    withProjectDir(temp)
    withPluginClasspath()
  }

  "reference to Konnekt resolved" {
    val result = GradleRunner {
      defaultConfig()
      buildFile.writeText(
          """
          |import konnekt.gradle.Konnekt
          |
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
          |tasks.create("konnektVersion") {
          |   doLast {
          |       println(Konnekt.ktorVersion)
          |   }         
          |}
          """.trimMargin()
      )
      withArguments("konnektVersion")
    }

    result.asClue {
      it.task(":konnektVersion")?.outcome shouldBe TaskOutcome.SUCCESS
    }
  }
})