package konnekt

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import konnekt.dependencies.hasSourceAnnotation
import konnekt.dependencies.isKonnektClient
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.parameterVisitor
import org.jetbrains.kotlin.psi.psiUtil.containingClass

class FunctionArgumentWithoutVariableTypeAnnotation : AbstractKotlinInspection() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor =
      parameterVisitor { parameter ->
        parameter.ownerFunction?.containingClass()?.let { containingClass ->
          if (isKonnektClient(containingClass) && !parameter.hasSourceAnnotation()) {
            val name = parameter.nameAsSafeName
            holder.registerProblem(
                parameter.nameIdentifier ?: parameter,
                "Value parameter $name should be annotated with 1 source annotation",
                ProblemHighlightType.ERROR
            )
          }
        }
      }

}