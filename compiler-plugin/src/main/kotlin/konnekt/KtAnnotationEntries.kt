package konnekt

import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

val KtAnnotationEntry.referencedName: String?
  get() = typeReference
      ?.typeElement
      ?.safeAs<KtUserType>()
      ?.referencedName