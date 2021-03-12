package konnekt

import arrow.meta.ide.IdeMetaPlugin
import arrow.meta.ide.invoke
import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.ProblemHighlightType
import org.jetbrains.kotlin.idea.inspections.AbstractApplicabilityBasedInspection
import org.jetbrains.kotlin.idea.util.nameIdentifierTextRangeInThis
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.containingClass

val IdeMetaPlugin.suspendFunDiagnostic
  get() = "Suspend fun diagnostic" {
    meta(
      addLocalInspection(
        suspendFunInspection,
        clientInterfacePath,
        konnektGroupName,
        HighlightDisplayLevel.ERROR,
      )
    )
  }

val IdeMetaPlugin.suspendFunInspection: AbstractApplicabilityBasedInspection<KtNamedFunction>
  get() = applicableInspection(
    defaultFixText = "Make suspend",
    staticDescription = "Function should be suspend",
    fixText = { "Make suspend " },
    inspectionHighlightType = { ProblemHighlightType.ERROR },
    kClass = KtNamedFunction::class.java,
    highlightingRange = { fn -> fn.nameIdentifierTextRangeInThis() },
    inspectionText = { (it.containingClass()?.nameAsSafeName?.asString() ?: "<no name provided>").notSuspended  },
    applyTo = { fn, _, _ ->
      fn.addModifier(KtTokens.SUSPEND_KEYWORD)
    },
    isApplicable = {
      val klass = it.containingClass()
      klass != null && isKonnektClient(klass) && !it.hasModifier(KtTokens.SUSPEND_KEYWORD)
    }
  )