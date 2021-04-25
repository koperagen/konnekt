package konnekt

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import io.kotest.matchers.collections.shouldExist
import konnekt.dependencies.noCompanion
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

class CompanionObjectDiagnosticTest1 : LightPlatformCodeInsightFixture4TestCase() {

  @Test
  fun `companion object check for annotated interface`() {
    myFixture.configureByText("test.kt", CompanionObjectInspectionCode.interfaceDeclaration)
    myFixture.enableInspections(CompanionObjectInspection())
    val result = myFixture.doHighlighting().filterNotNull()
    result shouldExist { it.description == "Test".noCompanion && it.severity == HighlightSeverity.ERROR }
  }

  @Test
  fun `companion object quick fix for annotated interface`() {
    myFixture.configureByText("test.kt", CompanionObjectInspectionCode.interfaceDeclaration)
    myFixture.enableInspections(CompanionObjectInspection())
    myFixture.launchAction(myFixture.findSingleIntention("Add companion object"))
    myFixture.checkResult(CompanionObjectInspectionCode.expectedDeclaration)
  }

}