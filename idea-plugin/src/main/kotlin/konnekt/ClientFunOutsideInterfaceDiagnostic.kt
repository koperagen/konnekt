package konnekt

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import konnekt.dependencies.hasVerbAnnotation
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.namedFunctionVisitor
import org.jetbrains.kotlin.psi.psiUtil.containingClass

class ClientFunOutsideInterfaceInspection : AbstractKotlinInspection() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor =
      namedFunctionVisitor { fn ->
        if (fn.hasVerbAnnotation() && fn.containingClass() == null) {
          holder.registerProblem(
              fn.nameIdentifier ?: fn,
              "Verb annotation on global fun has no effect",
              ProblemHighlightType.WARNING
          )
        }
      }
}