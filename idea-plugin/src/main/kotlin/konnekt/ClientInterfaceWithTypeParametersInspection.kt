package konnekt

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import konnekt.dependencies.isKonnektClient
import konnekt.dependencies.typeParametersNotAllowed
import org.jetbrains.kotlin.idea.inspections.AbstractApplicabilityBasedInspection
import org.jetbrains.kotlin.psi.KtClass

class ClientInterfaceWithTypeParametersInspection : AbstractApplicabilityBasedInspection<KtClass>(KtClass::class.java) {
  override val defaultFixText: String = "Remove type parameters"

  override fun inspectionHighlightType(element: KtClass): ProblemHighlightType {
    return ProblemHighlightType.ERROR
  }

  override fun inspectionHighlightRangeInElement(element: KtClass): TextRange? {
    return element.typeParameterList?.textRangeInParent
  }

  override fun applyTo(element: KtClass, project: Project, editor: Editor?) {
    element.typeParameterList?.delete()
    element.typeConstraintList?.delete()
  }

  override fun inspectionText(element: KtClass): String {
    return (element.name ?: "<no name provided>").typeParametersNotAllowed
  }

  override fun isApplicable(element: KtClass): Boolean {
    return isKonnektClient(element) && element.typeParameterList != null
  }
}