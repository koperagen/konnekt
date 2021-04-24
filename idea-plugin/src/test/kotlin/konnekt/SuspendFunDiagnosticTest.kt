package konnekt

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import io.kotest.matchers.collections.shouldExist
import org.junit.Test

class SuspendFunDiagnosticTest1 : LightPlatformCodeInsightFixture4TestCase() {
  private object Code {
    val before = """
     package test
     import konnekt.prelude.*
     
     @Client
     interface Foo {
        @GET("/foo")
        fun b<caret>ar()
        
        companion object
     }
    """.trimMargin()

    val after = """
     package test
     import konnekt.prelude.*
     
     @Client
     interface Foo {
        @GET("/foo")
        suspend fun bar()
        
        companion object
     }
    """.trimMargin()
  }

  @Test
  fun `suspend fun check for @Client interface`() {
    myFixture.configureByText("test.kt", Code.before)
    myFixture.enableInspections(SuspendFunInspection())
    val result = myFixture.doHighlighting().filterNotNull()
    result shouldExist { it.description == "Foo".notSuspended && it.severity == HighlightSeverity.ERROR }
  }


  @Test
  fun `suspend fun quick fix for @Client interface`() {
    myFixture.configureByText("test.kt", Code.before)
    myFixture.enableInspections(SuspendFunInspection())
    myFixture.launchAction(myFixture.findSingleIntention("Make suspend"))
    myFixture.checkResult(Code.after)
  }
}