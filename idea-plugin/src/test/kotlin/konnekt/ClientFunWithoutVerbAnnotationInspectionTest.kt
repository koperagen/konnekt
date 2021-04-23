package konnekt

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import io.kotest.matchers.collections.shouldExist
import org.junit.Test

class ClientFunWithoutVerbAnnotationInspectionTest1 : LightPlatformCodeInsightFixture4TestCase() {

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
  fun `verb annotation check for @Client interface`() {
    myFixture.configureByText("test.kt",  Code.before)
    myFixture.enableInspections(ClientFunWithoutVerbAnnotationInspection())
    val result = myFixture.doHighlighting().filterNotNull()
    result shouldExist { it.description == "Foo".noVerb && it.severity == HighlightSeverity.ERROR }
  }
}