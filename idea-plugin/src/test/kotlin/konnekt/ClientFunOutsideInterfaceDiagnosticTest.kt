package konnekt

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import io.kotest.matchers.collections.shouldExist
import org.junit.Test

class ClientFunOutsideInterfaceDiagnosticTest : LightPlatformCodeInsightFixture4TestCase() {

  private object Code {
    val error = """
      |package test
      |
      |import konnekt.prelude.*
      |
      |@GET("/bar")
      |fun foo(): String = TODO()
    """.trimMargin()
  }

  @Test
  fun `check verb annotations miss usage`() {
    myFixture.configureByText("test.kt", Code.error)
    myFixture.enableInspections(ClientFunOutsideInterfaceInspection())
    val results = myFixture.doHighlighting().filterNotNull()
    results shouldExist { it.description == "Verb annotation on global fun has no effect" && it.severity == HighlightSeverity.WARNING }
  }

}