package konnekt

import arrow.meta.ide.IdeMetaPlugin
import arrow.meta.ide.invoke
import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.ProblemHighlightType
import org.jetbrains.kotlin.idea.inspections.AbstractApplicabilityBasedInspection
import org.jetbrains.kotlin.idea.util.textRangeIn
import org.jetbrains.kotlin.psi.KtClass

val IdeMetaPlugin.clientInterfaceWithSupertypeDiagnostic
  get() = "Client interface with supertype diagnostic" {
    meta(
      addLocalInspection(
        clientInterfaceWithSupertypeInspection,
        clientInterfacePath,
        konnektGroupName,
        HighlightDisplayLevel.ERROR,
      )
    )
  }

val IdeMetaPlugin.clientInterfaceWithSupertypeInspection: AbstractApplicabilityBasedInspection<KtClass>
  get() = applicableInspection(
    defaultFixText = "Remove supertypes",
    staticDescription = "Super types inspection",
    fixText = { "Remove supertypes " },
    inspectionHighlightType = { ProblemHighlightType.ERROR },
    kClass = KtClass::class.java,
    highlightingRange = { klass -> klass.getSuperTypeList()?.textRangeIn(klass) },
    inspectionText = { it.nameAsSafeName.asString().superTypesNotAllowed  },
    applyTo = { klass, _, _ ->
      val list = klass.getSuperTypeList() ?: return@applicableInspection
      klass.run {
        deleteChildRange(getColon() ?: list, list)
      }
    },
    isApplicable = { klass ->
      isKonnektClient(klass) && klass.superTypeListEntries.isNotEmpty()
    }
  )