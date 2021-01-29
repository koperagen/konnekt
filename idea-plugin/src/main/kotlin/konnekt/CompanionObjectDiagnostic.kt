package konnekt

import arrow.meta.ide.IdeMetaPlugin
import arrow.meta.ide.invoke
import arrow.meta.phases.analysis.companionObject
import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.ProblemHighlightType
import org.jetbrains.kotlin.idea.inspections.AbstractApplicabilityBasedInspection
import org.jetbrains.kotlin.idea.util.nameIdentifierTextRangeInThis
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtPsiFactory

val IdeMetaPlugin.companionObjectDiagnostic
  get() = "Companion object diagnostic" {
    meta(
      addLocalInspection(
        inspectionTool = companionObjectInspection,
        level = HighlightDisplayLevel.ERROR,
        groupPath = arrayOf("Konnekt", "CompanionObject"),
        groupDisplayName = "CompanionObject",
        displayName = "Companion object diagnostic",
        shortName = "Companion object diagnostic"
      )
    )
  }

val IdeMetaPlugin.companionObjectInspection: AbstractApplicabilityBasedInspection<KtClass>
  get() = applicableInspection(
    defaultFixText = "Companion object",
    staticDescription = "Companion object inspection",
    fixText = { "companion object required on interface ${it.name} annotated with @Client" },
    inspectionHighlightType = { ProblemHighlightType.ERROR },
    kClass = KtClass::class.java,
    highlightingRange = { klass -> klass.nameIdentifierTextRangeInThis() },
    inspectionText = { "interface ${it.name} should have companion object" },
    applyTo = { klass, _, _ ->
      klass.addDeclaration(KtPsiFactory(klass).createCompanionObject())
    },
    isApplicable = {
      isKonnektClient(it) && it.companionObject == null
    }
  )