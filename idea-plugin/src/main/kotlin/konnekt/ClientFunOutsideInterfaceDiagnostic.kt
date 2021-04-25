package konnekt

//import arrow.meta.ide.IdeMetaPlugin
//import arrow.meta.ide.invoke
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import konnekt.dependencies.hasVerbAnnotation
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.namedFunctionVisitor
import org.jetbrains.kotlin.psi.psiUtil.containingClass

//val IdeMetaPlugin.clientFunOutsideInterfaceDiagnostic
//  get() = "Client fun outside interface diagnostic" {
//    meta(
//      addLocalInspection(
//        ClientFunOutsideInterfaceInspection(),
//        HighlightDisplayLevel.WARNING,
//        shortName = "ClientFunOutsideInterfaceDiagnostic",
//        displayName = "clientFunOutsideInterfaceDiagnostic",
//        groupPath = clientInterfacePath,
//        groupDisplayName = konnektGroupName
//      )
//    )
//  }

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