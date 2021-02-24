package konnekt

import arrow.meta.ide.IdeMetaPlugin
import arrow.meta.ide.testing.IdeTest
import arrow.meta.ide.testing.env.IdeTestSetUp
import arrow.meta.ide.testing.env.ideTest
import com.intellij.lang.annotation.HighlightSeverity
import org.junit.Test

class ClientFunWithoutVerbAnnotationInspectionTest : IdeTestSetUp() {

  private object Code {
    val before = """
      |package test
      |import konnekt.prelude.*
      |
      |@Client
      |interface Foo {
      |   suspend fun test()
      |   
      |   companion object
      |}
    """.trimMargin()
  }

  @Test
  fun `verb annotation check for @Client interface`() =
      ideTest(
        myFixture,
        IdeMetaPlugin()
      ) {
        listOf(
          IdeTest(
            code = Code.before,
            test = { code, myFixture, ctx ->
              collectInspections(code, myFixture, listOf(ctx.clientFunWithoutVerbAnnotationInspection))
            },
            result = resolvesWhen("") { result ->
              val highlight = result.firstOrNull { it.description == "Foo".noVerb }
              highlight != null && highlight.severity == HighlightSeverity.ERROR
            }
          )
        )
      }
}