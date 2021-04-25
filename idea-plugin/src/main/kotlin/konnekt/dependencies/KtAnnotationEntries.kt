package konnekt.dependencies

import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

val KtAnnotationEntry.referencedName: String?
  get() = typeReference
      ?.typeElement
      ?.safeAs<KtUserType>()
      ?.referencedName

fun KtAnnotated.hasAnnotation(
    vararg annotationNames: String
): Boolean {
  val names = annotationNames.toHashSet()
  val predicate: (KtAnnotationEntry) -> Boolean = {
    it.referencedName in names
  }
  return annotationEntries.any(predicate)
}