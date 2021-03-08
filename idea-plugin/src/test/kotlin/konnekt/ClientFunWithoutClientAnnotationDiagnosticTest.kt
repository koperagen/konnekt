package konnekt

import arrow.meta.ide.IdeMetaPlugin
import arrow.meta.ide.testing.IdeTest
import arrow.meta.ide.testing.env.IdeTestSetUp
import arrow.meta.ide.testing.env.ideTest
import com.intellij.lang.annotation.HighlightSeverity
import org.junit.Test

class ClientFunWithoutClientAnnotationDiagnosticTest : IdeTestSetUp() {

  private object InsideInterface {
    val before = """
      |package test
      |
      |import konnekt.prelude.*
      |
      |interface Foo {
      |
      |   @GET("/foo")
      |   suspend fun b<caret>ar(): String
      |}
    """.trimMargin()

    val after = """
      |package test
      |
      |import konnekt.prelude.*
      |
      |@konnekt.prelude.Client
      |interface Foo {
      |
      |   @GET("/foo")
      |   suspend fun bar(): String
      |}
    """.trimMargin()
  }

  @Test
  fun `@Client annotation check for fun inside interface`() = ideTest(myFixture, IdeMetaPlugin()) {
    listOf(
      IdeTest(
        code = InsideInterface.before,
        test = { code, myFixture, ctx ->
          collectInspections(code, myFixture, listOf(ctx.clientFunWithoutClientAnnotationInspection))
        },
        result = resolvesWhen("") {
          val highlight = it.firstOrNull { it.description == "Foo".noClientAnnotation }
          highlight != null && highlight.severity == HighlightSeverity.ERROR
        }
      )
    )
  }

  @Test
  fun `add @Client annotation quick fix for interface`() = ideTest(myFixture, IdeMetaPlugin()) {
    listOf(
      IdeTest(
        code = InsideInterface.before,
        test = { code, myFixture, ctx ->
          applyQuickFix(code, InsideInterface.after, "Annotate interface with ",
              myFixture, listOf(ctx.clientFunWithoutClientAnnotationInspection))
        },
        result = resolves()
      )
    )
  }

}