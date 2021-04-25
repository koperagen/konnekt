package konnekt

//import arrow.meta.ide.IdeMetaPlugin
//import arrow.meta.ide.invoke
//import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import konnekt.dependencies.isKonnektClient
import konnekt.dependencies.superTypesNotAllowed
import org.jetbrains.kotlin.idea.inspections.AbstractApplicabilityBasedInspection
import org.jetbrains.kotlin.idea.util.textRangeIn
import org.jetbrains.kotlin.psi.KtClass

//val IdeMetaPlugin.clientInterfaceWithSupertypeDiagnostic
//  get() = "Client interface with supertype diagnostic" {
//    meta(
//      addLocalInspection(
//        ClientInterfaceWithSupertypeInspection(),
//        clientInterfacePath,
//        konnektGroupName,
//        HighlightDisplayLevel.ERROR,
//      )
//    )
//  }
//
//val IdeMetaPlugin.clientInterfaceWithSupertypeInspection: AbstractApplicabilityBasedInspection<KtClass>
//  get() = applicableInspection(
//    defaultFixText = "Remove supertypes",
//    staticDescription = "Super types inspection",
//    fixText = { "Remove supertypes " },
//    inspectionHighlightType = { ProblemHighlightType.ERROR },
//    kClass = KtClass::class.java,
//    highlightingRange = { klass -> klass.getSuperTypeList()?.textRangeIn(klass) },
//    inspectionText = { it.nameAsSafeName.asString().superTypesNotAllowed  },
//    applyTo = { klass, _, _ ->
//      val list = klass.getSuperTypeList() ?: return@applicableInspection
//      klass.run {
//        deleteChildRange(getColon() ?: list, list)
//      }
//    },
//    isApplicable = { klass ->
//      isKonnektClient(klass) && klass.superTypeListEntries.isNotEmpty()
//    }
//  )


class ClientInterfaceWithSupertypeInspection : AbstractApplicabilityBasedInspection<KtClass>(KtClass::class.java) {
  override val defaultFixText: String = "Remove supertypes"
  override fun getStaticDescription(): String = "Super types inspection"
  override fun fixText(element: KtClass): String  = "Remove supertypes"

  override fun inspectionHighlightType(element: KtClass): ProblemHighlightType {
    return ProblemHighlightType.ERROR
  }

  override fun inspectionHighlightRangeInElement(element: KtClass): TextRange? {
    return element.getSuperTypeList()?.textRangeIn(element)
  }

  override fun applyTo(element: KtClass, project: Project, editor: Editor?) {
    val list = element.getSuperTypeList() ?: return
    return element.run {
      deleteChildRange(getColon() ?: list, list)
    }
  }

  override fun inspectionText(element: KtClass): String {
    return element.nameAsSafeName.asString().superTypesNotAllowed
  }

  override fun isApplicable(element: KtClass): Boolean {
    return isKonnektClient(element) && element.superTypeListEntries.isNotEmpty()
  }
}