package konnekt

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import konnekt.dependencies.isKonnektClient
import konnekt.dependencies.noCompanion
import org.jetbrains.kotlin.idea.inspections.AbstractApplicabilityBasedInspection
import org.jetbrains.kotlin.idea.util.nameIdentifierTextRangeInThis
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtPsiFactory

class CompanionObjectInspection : AbstractApplicabilityBasedInspection<KtClass>(KtClass::class.java) {
  override val defaultFixText: String = "Companion object"
  override fun getStaticDescription(): String = "Companion object inspection"
  override fun fixText(element: KtClass): String = "Add companion object"

  override fun inspectionHighlightType(element: KtClass): ProblemHighlightType {
    return ProblemHighlightType.ERROR
  }

  override fun inspectionHighlightRangeInElement(element: KtClass): TextRange? {
    return element.nameIdentifierTextRangeInThis()
  }

  override fun applyTo(element: KtClass, project: Project, editor: Editor?) {
    element.addDeclaration(KtPsiFactory(element).createCompanionObject())
  }

  override fun inspectionText(element: KtClass): String {
    return (element.name ?: "<no name provided>").noCompanion
  }

  override fun isApplicable(element: KtClass): Boolean {
    return isKonnektClient(element) && element.companionObjects.firstOrNull() == null
  }
}