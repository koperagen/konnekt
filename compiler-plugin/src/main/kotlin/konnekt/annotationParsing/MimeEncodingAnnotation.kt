package konnekt.annotationParsing

import arrow.meta.phases.CompilerContext
import konnekt.FormUrlEncoded
import konnekt.MimeEncoding
import konnekt.MimeEncodingsDeclaration
import konnekt.Multipart
import konnekt.names
import konnekt.parsingError
import konnekt.referencedName
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.containingClass

fun KtNamedFunction.mimeEncoding(ctx: CompilerContext): MimeEncoding? {
  val scoped = annotationEntries.mapNotNull { mimeEncoding(it) }

  return when (scoped.size) {
    0 -> null
    1 -> ctx.refine(scoped[0])
    else -> ctx.parsingError(methodHasSeveralAnnotations(scoped))
  }
}

private fun KtNamedFunction.methodHasSeveralAnnotations(annotations: List<MimeEncodingScope>): String {
  return """Method $name of ${containingClass()?.name} should be annotated with one of [${MimeEncodingsDeclaration.values().joinToString(",") { it.declaration.simpleName }}], but got ${annotations.size}: ${annotations.joinToString { it.annotation.name ?: "" }}"""
}

class MimeEncodingScope(val annotation: KtAnnotationEntry, val encoding: MimeEncodingsDeclaration)

fun mimeEncoding(annotationEntry: KtAnnotationEntry): MimeEncodingScope? {
  val name = annotationEntry.referencedName
  return MimeEncodingsDeclaration.values()
    .firstOrNull { name in it.names }
    ?.let { encoding ->
      MimeEncodingScope(annotationEntry, encoding)
    }
}

private fun CompilerContext.refine(scope: MimeEncodingScope): MimeEncoding {
  return when (scope.encoding) {
    MimeEncodingsDeclaration.MULTIPART -> Multipart
    MimeEncodingsDeclaration.FORM_URL_ENCODED -> FormUrlEncoded
  }
}