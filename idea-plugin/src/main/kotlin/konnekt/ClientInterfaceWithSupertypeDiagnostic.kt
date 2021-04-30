package konnekt

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import konnekt.dependencies.isKonnektClient
import konnekt.dependencies.superTypesNotAllowed
import org.jetbrains.kotlin.idea.inspections.AbstractApplicabilityBasedInspection
import org.jetbrains.kotlin.idea.util.textRangeIn
import org.jetbrains.kotlin.psi.KtClass

class ClientInterfaceWithSupertypeInspection : AbstractApplicabilityBasedInspection<KtClass>(KtClass::class.java) {
  override val defaultFixText: String = "Remove supertypes"
  override fun getStaticDescription(): String = "Super types inspection"

  override fun inspectionHighlightType(element: KtClass): ProblemHighlightType {
    return ProblemHighlightType.ERROR
  }

  override fun inspectionHighlightRangeInElement(element: KtClass): TextRange? {
    return element.getSuperTypeList()?.textRangeIn(element)
  }

  override fun applyTo(element: KtClass, project: Project, editor: Editor?) {
    val list = element.getSuperTypeList() ?: return
    return element.run {
      deleteChildRange(getColon() ?: list, list)
    }
  }

  override fun inspectionText(element: KtClass): String {
    return element.nameAsSafeName.asString().superTypesNotAllowed
  }

  override fun isApplicable(element: KtClass): Boolean {
    return isKonnektClient(element) && element.superTypeListEntries.isNotEmpty()
  }
}