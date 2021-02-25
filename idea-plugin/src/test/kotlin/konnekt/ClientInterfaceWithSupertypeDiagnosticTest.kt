package konnekt

import arrow.meta.ide.IdeMetaPlugin
import arrow.meta.ide.testing.IdeTest
import arrow.meta.ide.testing.env.IdeTestSetUp
import arrow.meta.ide.testing.env.ideTest
import com.intellij.lang.annotation.HighlightSeverity
import org.junit.Test

private object Code {
  val before = """
    |package test
    |import konnekt.prelude.*
    |
    |interface Foo
    |
    |@Client
    |interface Bar : F<caret>oo {
    |
    |   companion object
    |}
  """.trimMargin()

  val after = """
    |package test
    |import konnekt.prelude.*
    |
    |interface Foo
    |
    |@Client
    |interface Bar {
    |
    |   companion object
    |}
  """.trimMargin()
}

class ClientInterfaceWithSupertypeDiagnosticTest : IdeTestSetUp() {

  @Test
  fun `supertypes check for annotated interface`() =
    ideTest(
        myFixture = myFixture,
        ctx = IdeMetaPlugin()
    ) {
      listOf(
        IdeTest(
          code = Code.before,
          test = { code, myFixture, ctx ->
            collectInspections(code, myFixture, inspections = listOf(ctx.clientInterfaceWithSupertypeInspection))
          },
          result = resolvesWhen("") { result ->
            val highlight = result.firstOrNull { it.description == "Bar".superTypesNotAllowed }
            highlight != null && highlight.severity == HighlightSeverity.ERROR
          }
        )
      )
    }

  @Test
  fun `remove supertypes quick fix for annotated interface`() =
    ideTest(
        myFixture = myFixture,
        ctx = IdeMetaPlugin()
    ) {
      listOf(
        IdeTest(
          code = Code.before,
          test = { code, myFixture, ctx ->
            applyQuickFix(code, Code.after, "Remove supertypes", myFixture, listOf(ctx.clientInterfaceWithSupertypeInspection))
          },
          result = resolves()
        )
      )
    }

}
