package konnekt

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import konnekt.dependencies.ClientDeclaration
import konnekt.dependencies.fqEntry
import konnekt.dependencies.hasVerbAnnotation
import konnekt.dependencies.noClientAnnotation
import org.jetbrains.kotlin.idea.inspections.AbstractApplicabilityBasedInspection
import org.jetbrains.kotlin.idea.util.nameIdentifierTextRangeInThis
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.containingClass

class ClientFunWithoutClientAnnotationInspection : AbstractApplicabilityBasedInspection<KtNamedFunction>(KtNamedFunction::class.java) {
  override val defaultFixText: String = "Annotate interface with ${ClientDeclaration.fqEntry}}"
  override fun getStaticDescription(): String = "Client fun without ${ClientDeclaration.fqEntry}}"

  override fun inspectionHighlightRangeInElement(element: KtNamedFunction): TextRange? {
    return element.nameIdentifierTextRangeInThis()
  }

  override fun inspectionHighlightType(element: KtNamedFunction): ProblemHighlightType {
    return ProblemHighlightType.ERROR
  }

  override fun applyTo(element: KtNamedFunction, project: Project, editor: Editor?) {
    val klass = element.containingClass() ?: return
    val entry = KtPsiFactory(project).createAnnotationEntry(ClientDeclaration.fqEntry)
    klass.addAnnotationEntry(entry)
  }

  override fun inspectionText(element: KtNamedFunction): String {
    val klass = element.containingClass() ?: return ""
    return klass.nameAsSafeName.asString().noClientAnnotation
  }

  override fun isApplicable(element: KtNamedFunction): Boolean {
    val klass = element.containingClass() ?: return false
    return element.hasVerbAnnotation() && !klass.isKonnektClient() && klass.isInterface()
  }

}

private fun KtClass.isKonnektClient() = konnekt.dependencies.isKonnektClient(this)