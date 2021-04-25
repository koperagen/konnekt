package konnekt

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import konnekt.dependencies.isKonnektClient
import konnekt.dependencies.notSuspended
import org.jetbrains.kotlin.idea.inspections.AbstractApplicabilityBasedInspection
import org.jetbrains.kotlin.idea.util.nameIdentifierTextRangeInThis
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.containingClass

class SuspendFunInspection : AbstractApplicabilityBasedInspection<KtNamedFunction>(KtNamedFunction::class.java) {
  override val defaultFixText: String = "Make suspend"
  override fun getStaticDescription(): String  = "Function should be suspend"
  override fun fixText(element: KtNamedFunction): String = "Make suspend"

  override fun inspectionHighlightType(element: KtNamedFunction): ProblemHighlightType {
    return ProblemHighlightType.ERROR
  }

  override fun inspectionHighlightRangeInElement(element: KtNamedFunction): TextRange? {
    return element.nameIdentifierTextRangeInThis()
  }

  override fun applyTo(element: KtNamedFunction, project: Project, editor: Editor?) {
    return element.addModifier(KtTokens.SUSPEND_KEYWORD)
  }

  override fun inspectionText(element: KtNamedFunction): String {
    return (element.containingClass()?.nameAsSafeName?.asString() ?: "<no name provided>").notSuspended
  }

  override fun isApplicable(element: KtNamedFunction): Boolean {
    val klass = element.containingClass()
    return klass != null && isKonnektClient(klass) && !element.hasModifier(KtTokens.SUSPEND_KEYWORD)
  }
}