package konnekt

import arrow.meta.ide.IdeMetaPlugin
import arrow.meta.ide.invoke
import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.namedFunctionVisitor
import org.jetbrains.kotlin.psi.psiUtil.containingClass

val IdeMetaPlugin.clientFunWithoutVerbAnnotationDiagnostic
  get() = "Client fun without verb annotation diagnostic" {
    meta(
      addLocalInspection(
        ClientFunWithoutVerbAnnotationInspection(),
        HighlightDisplayLevel.ERROR,
        "ClientFunWithoutVerbAnnotation",
        "Client fun without verb annotation",
        clientInterfacePath,
        konnektGroupName
      )
    )
  }

val IdeMetaPlugin.clientFunWithoutVerbAnnotationInspection
  get() = ClientFunWithoutVerbAnnotationInspection()

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