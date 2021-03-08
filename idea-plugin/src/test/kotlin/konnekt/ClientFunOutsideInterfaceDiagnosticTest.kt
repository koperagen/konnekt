package konnekt

import arrow.meta.ide.IdeMetaPlugin
import arrow.meta.ide.testing.IdeTest
import arrow.meta.ide.testing.env.IdeTestSetUp
import arrow.meta.ide.testing.env.ideTest
import com.intellij.lang.annotation.HighlightSeverity
import org.junit.Test

class ClientFunOutsideInterfaceDiagnosticTest : IdeTestSetUp() {
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
  fun `check verb annotations miss usage`() = ideTest(myFixture, IdeMetaPlugin()) {
    listOf(
      IdeTest(
        code = Code.error,
        test = { code, myFixture, ctx ->
          collectInspections(code, myFixture, listOf(ClientFunOutsideInterfaceInspection()))
        },
        result = resolvesWhen("") { result ->
          result.any {
            it.description == "Verb annotation on global fun has no effect" && it.severity == HighlightSeverity.WARNING
          }
        }
      )
    )
  }

}