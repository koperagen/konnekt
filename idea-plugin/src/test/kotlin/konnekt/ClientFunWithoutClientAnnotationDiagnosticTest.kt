package konnekt

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import io.kotest.matchers.collections.shouldExist
import org.junit.Test

class ClientFunWithoutClientAnnotationDiagnosticTest1 : LightPlatformCodeInsightFixture4TestCase() {

  private object InsideInterface {
    val before = """
       package test
       
       import konnekt.prelude.*
       
       interface Foo {
       
          @GET("/foo")
          suspend fun b<caret>ar(): String
       }
    """.trimIndent()

    val after = """
       package test
       
       import konnekt.prelude.*
       
       @konnekt.prelude.Client
       interface Foo {
       
          @GET("/foo")
          suspend fun bar(): String
       }
    """.trimIndent()
  }

  @Test
  fun `@Client annotation check for fun inside interface`() {
    myFixture.configureByText("test.kt", InsideInterface.before)
    myFixture.enableInspections(ClientFunWithoutClientAnnotationInspection())
    val result = myFixture.doHighlighting().filterNotNull()
    result shouldExist { it.description == "Foo".noClientAnnotation && it.severity == HighlightSeverity.ERROR }
  }

  @Test
  fun `add @Client annotation quick fix for interface`() {
    myFixture.configureByText("test.kt", InsideInterface.before)
    myFixture.enableInspections(ClientFunWithoutClientAnnotationInspection())
    myFixture.launchAction(myFixture.findSingleIntention("Annotate interface with "))
    myFixture.checkResult(InsideInterface.after)
  }

}