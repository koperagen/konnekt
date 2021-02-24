package konnekt

import arrow.meta.ide.IdeMetaPlugin
import arrow.meta.ide.testing.IdeTest
import arrow.meta.ide.testing.env.IdeTestSetUp
import arrow.meta.ide.testing.env.ideTest
import com.intellij.lang.annotation.HighlightSeverity
import org.junit.Test


class SuspendFunDiagnosticTest : IdeTestSetUp() {
  private object Code {
    val before = """
    |package test
    |import konnekt.prelude.*
    |
    |@Client
    |interface Foo {
    |   @GET
    |   fun b<caret>ar()
    |   
    |   companion object
    |}
    """.trimMargin()

    val after = """
    |package test
    |import konnekt.prelude.*
    |
    |@Client
    |interface Foo {
    |   @GET
    |   suspend fun bar()
    |   
    |   companion object
    |}
    """.trimMargin()
  }

  @Test
  fun `suspend fun check for @Client interface`() =
      ideTest(
        myFixture = myFixture,
        ctx = IdeMetaPlugin()
      ) {
        listOf(
          IdeTest(
            code = Code.before,
            test = { code, myFixture, ctx ->
              collectInspections(code, myFixture, listOf(ctx.suspendFunInspection))
            },
            result = resolvesWhen("") { result ->
              val highlight = result.firstOrNull { it.description == "Foo".notSuspended }
              highlight != null && highlight.severity == HighlightSeverity.ERROR
            }
          )
        )
      }

  @Test
  fun `suspend fun quick fix for @Client interface`() =
    ideTest(
      myFixture = myFixture,
      ctx = IdeMetaPlugin(),
    ) {
      listOf(
        IdeTest(
          code = Code.before,
          test = { code, myFixture, ctx ->
            applyQuickFix(code, Code.after, "Make suspend", myFixture, listOf(ctx.suspendFunInspection))
          },
          result = resolves()
        )
      )
    }
}