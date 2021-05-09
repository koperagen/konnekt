package konnekt

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import io.kotest.matchers.collections.shouldExist
import konnekt.dependencies.typeParametersNotAllowed
import org.junit.Test

class ClientInterfaceWithTypeParametersDiagnosticTest : LightPlatformCodeInsightFixture4TestCase() {

  private companion object {
    val before = """
        package test
        
        import konnekt.prelude.*
        
        @Client interface Test<T<caret>>
    """.trimIndent()

    val after = """
        package test
        
        import konnekt.prelude.*
        
        @Client interface Test
    """.trimIndent()
  }

  @Test
  fun `type parameter check for annotated interface`() {
    myFixture.configureByText("test.kt", before)
    myFixture.enableInspections(ClientInterfaceWithTypeParametersInspection())
    val result = myFixture.doHighlighting().filterNotNull()
    result shouldExist { it.description == "Test".typeParametersNotAllowed && it.severity == HighlightSeverity.ERROR }
  }

  @Test
  fun `type parameter quick fix for annotated interface`() {
    myFixture.configureByText("test.kt", before)
    myFixture.enableInspections(ClientInterfaceWithTypeParametersInspection())
    myFixture.launchAction(myFixture.findSingleIntention("Remove type parameters"))
    myFixture.checkResult(after)
  }

}