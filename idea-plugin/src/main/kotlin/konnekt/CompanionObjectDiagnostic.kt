package konnekt

import arrow.meta.ide.IdeMetaPlugin
import arrow.meta.ide.invoke
import arrow.meta.phases.analysis.companionObject
import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.inspections.AbstractApplicabilityBasedInspection
import org.jetbrains.kotlin.idea.util.nameIdentifierTextRangeInThis
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtPsiFactory

val IdeMetaPlugin.companionObjectDiagnostic
  get() = "Companion object diagnostic" {
    meta(
      addLocalInspection(
        inspection = CompanionObjectInspection(),
        groupPath = arrayOf("Konnekt", "CompanionObject"),
        groupDisplayName = "CompanionObject",
        level = HighlightDisplayLevel.ERROR,
      )
    )
  }

val IdeMetaPlugin.companionObjectInspection: AbstractApplicabilityBasedInspection<KtClass>
  get() = applicableInspection(
    defaultFixText = "Companion object",
    staticDescription = "Companion object inspection",
    fixText = { "Add companion object " },
    inspectionHighlightType = { ProblemHighlightType.ERROR },
    kClass = KtClass::class.java,
    highlightingRange = { klass -> klass.nameIdentifierTextRangeInThis() },
    inspectionText = { (it.name ?: "<no name provided>").noCompanion  },
    applyTo = { klass, _, _ ->
      klass.addDeclaration(KtPsiFactory(klass).createCompanionObject())
    },
    isApplicable = {
      isKonnektClient(it) && it.companionObject == null
    }
  )

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
    return isKonnektClient(element) && element.companionObject == null
  }
}