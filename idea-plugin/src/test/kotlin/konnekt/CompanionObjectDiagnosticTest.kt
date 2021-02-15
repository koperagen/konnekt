package konnekt

import arrow.meta.ide.IdeMetaPlugin
import arrow.meta.ide.testing.IdeTest
import arrow.meta.ide.testing.dsl.IdeTestSyntax
import arrow.meta.ide.testing.env.IdeTestSetUp
import arrow.meta.ide.testing.env.ideTest
import arrow.meta.ide.testing.Source
import arrow.meta.plugins.optics.internals.noCompanion
import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.junit.Test

object CompanionObjectInspectionCode {
  val interfaceDeclaration = """
    package test
    
    import konnekt.prelude.*
    
    @Client interface Tes<caret>t
  """.trimIndent()

  val expectedDeclaration = """
    package test
    
    import konnekt.prelude.*
    
    @Client interface Test {
        companion object {
        }
    }
  """.trimIndent()
}

class CompanionObjectDiagnosticTest : IdeTestSetUp() {

  @Test
  fun `companion object check for annotated interface`() =
    ideTest(
      myFixture = myFixture,
      ctx = IdeMetaPlugin()
    ) {
      listOf(
        IdeTest(
          code = CompanionObjectInspectionCode.interfaceDeclaration,
          test = { code, myFixture, ctx ->
            collectInspections(code, myFixture, inspections = listOf(ctx.companionObjectInspection))
          },
          result = resolvesWhen("") { result ->
            val highlight = result.firstOrNull { it.description == "Test".noCompanion }
            highlight != null && highlight.severity == HighlightSeverity.ERROR
          }
        )
      )
    }

  @Test
  fun `companion object quick fix for annotated interface`() =
    ideTest(
      myFixture = myFixture,
      ctx = IdeMetaPlugin()
    ) {
      listOf(
        IdeTest(
          code = CompanionObjectInspectionCode.interfaceDeclaration,
          test = { code, myFixture, ctx ->
            applyQuickFix(code, CompanionObjectInspectionCode.expectedDeclaration, myFixture, listOf(ctx.companionObjectInspection))
          },
          result = resolves()
        )
      )
    }

}

fun IdeTestSyntax.applyQuickFix(
    code: Source,
    result: Source,
    myFixture: CodeInsightTestFixture,
    inspections: List<InspectionProfileEntry>
): Unit = lightTest {
  inspections.forEach { myFixture.enableInspections(it) }
  val file = myFixture.configureByText("test.kt", code)
  myFixture.launchAction(myFixture.findSingleIntention("Add companion object "))
  myFixture.checkResult(result)
} ?: Unit