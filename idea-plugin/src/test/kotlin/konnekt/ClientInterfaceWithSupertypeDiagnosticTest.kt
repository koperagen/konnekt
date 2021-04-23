package konnekt

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import io.kotest.matchers.collections.shouldExist
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

class ClientInterfaceWithSupertypeDiagnosticTest1 : LightPlatformCodeInsightFixture4TestCase() {

  @Test
  fun `supertypes check for annotated interface`() {
    myFixture.configureByText("test.kt", Code.before)
    myFixture.enableInspections(ClientInterfaceWithSupertypeInspection())
    val result = myFixture.doHighlighting().filterNotNull()
    result shouldExist { it.description == "Bar".superTypesNotAllowed && it.severity == HighlightSeverity.ERROR }
  }

  @Test
  fun `remove supertypes quick fix for annotated interface`() {
    myFixture.configureByText("test.kt", Code.before)
    myFixture.enableInspections(ClientInterfaceWithSupertypeInspection())
    myFixture.launchAction(myFixture.findSingleIntention("Remove supertypes"))
    myFixture.checkResult(Code.after)
  }
}
