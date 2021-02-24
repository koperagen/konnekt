package konnekt

import arrow.meta.ide.testing.dsl.IdeTestSyntax
import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import arrow.meta.ide.testing.Source

fun IdeTestSyntax.applyQuickFix(
    code: Source,
    result: Source,
    hint: String,
    myFixture: CodeInsightTestFixture,
    inspections: List<InspectionProfileEntry>
): Unit = lightTest {
  inspections.forEach { myFixture.enableInspections(it) }
  myFixture.configureByText("test.kt", code)
  myFixture.launchAction(myFixture.findSingleIntention(hint))
  myFixture.checkResult(result)
} ?: Unit