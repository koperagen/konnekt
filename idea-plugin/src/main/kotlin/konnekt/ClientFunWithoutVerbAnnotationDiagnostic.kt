package konnekt

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import konnekt.dependencies.hasVerbAnnotation
import konnekt.dependencies.isKonnektClient
import konnekt.dependencies.noVerb
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.namedFunctionVisitor
import org.jetbrains.kotlin.psi.psiUtil.containingClass

class ClientFunWithoutVerbAnnotationInspection : AbstractKotlinInspection() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor =
    namedFunctionVisitor { fn ->
      val klass = fn.containingClass() ?: return@namedFunctionVisitor
      if (isKonnektClient(klass) && !fn.hasVerbAnnotation()) {
        holder.registerProblem(
          fn.nameIdentifier ?: fn,
          klass.nameAsSafeName.asString().noVerb,
          ProblemHighlightType.ERROR
        )
      }
    }

}