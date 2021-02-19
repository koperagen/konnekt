package konnekt

import arrow.meta.phases.CompilerContext
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun KtNamedFunction.headers(ctx: CompilerContext): HeadersAnnotation? {
  val scoped = annotationEntries.mapNotNull { headersAnnotation(it) }

  return when (scoped.size) {
    0 -> null
    1 -> ctx.refine(scoped[0])
    else -> ctx.parsingError("Repeating @Headers annotation is not yet supported")
  }
}

fun headersAnnotation(annotation: KtAnnotationEntry): HeadersAnnotationScope? {
  return when (annotation.typeReference?.typeElement?.safeAs<KtUserType>()?.referencedName) {
    in HeadersDeclaration.names -> HeadersAnnotationScope(annotation)
    else -> null
  }
}

class HeadersAnnotationScope(val annotation: KtAnnotationEntry)

data class HeadersAnnotation(val headers: List<String>)

private fun CompilerContext.refine(scope: HeadersAnnotationScope): HeadersAnnotation? {
  return scope.annotation.valueArguments.mapIndexed { index, valueArgument ->
    constantStringOrNull(valueArgument) ?: return parsingError("")
  }.let {
    HeadersAnnotation(it)
  }
}
