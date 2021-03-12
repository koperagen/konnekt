package konnekt

import arrow.meta.ide.IdeMetaPlugin
import arrow.meta.ide.invoke
import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.ProblemHighlightType
import org.jetbrains.kotlin.idea.inspections.AbstractApplicabilityBasedInspection
import org.jetbrains.kotlin.idea.util.nameIdentifierTextRangeInThis
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.containingClass

val IdeMetaPlugin.clientFunWithoutClientAnnotationDiagnostic
  get() = "ClientFunWithoutClientAnnotationDiagnostic" {
    meta(
      addLocalInspection(
        clientFunWithoutClientAnnotationInspection,
        clientInterfacePath,
        konnektGroupName,
        HighlightDisplayLevel.ERROR
      )
    )
  }

val IdeMetaPlugin.clientFunWithoutClientAnnotationInspection: AbstractApplicabilityBasedInspection<KtNamedFunction>
  get() = applicableInspection(
    defaultFixText = "Annotate interface with ${ClientDeclaration.fqEntry}}",
    staticDescription = "Client fun without ${ClientDeclaration.fqEntry}}",
    fixText = { "Annotate interface with ${ClientDeclaration.fqEntry}}" },
    kClass = KtNamedFunction::class.java,
    highlightingRange = { it.nameIdentifierTextRangeInThis() },
    inspectionHighlightType = { ProblemHighlightType.ERROR },
    inspectionText = { fn ->
      val klass = fn.containingClass() ?: return@applicableInspection ""
      klass.nameAsSafeName.asString().noClientAnnotation
    },
    applyTo = { fn, _, _ ->
      val klass = fn.containingClass() ?: return@applicableInspection
      val entry = createAnnotationEntry(ClientDeclaration.fqEntry)
      klass.addAnnotationEntry(entry)
    },
    isApplicable = { fn ->
      val klass = fn.containingClass() ?: return@applicableInspection false
      fn.hasVerbAnnotation() && !klass.isKonnektClient() && klass.isInterface()
    }
  )

private fun KtClass.isKonnektClient() = isKonnektClient(this)