package konnekt

import arrow.meta.phases.CompilerContext
import org.jetbrains.kotlin.psi.KtNamedFunction

internal fun KtNamedFunction.refactor1(ctx: CompilerContext): VerbAnnotation? {
  val verbAnnotations = annotationEntries.mapNotNull { verbAnnotation(it) }
  return when (verbAnnotations.size) {
    0 -> ctx.parsingError("Client method should be annotated with some Verb Annotation", this)
    1 -> {
      val scope = verbAnnotations.first()
      ctx.refine(scope)
    }
    else -> ctx.parsingError("Client method should be annotated with exactly 1 Verb Annotation", this)
  }
}

private fun CompilerContext.refine(scope: VerbAnnotationScope): VerbAnnotation? {
  return when (scope.verb) {
    Verb.HTTP -> TODO()
    else -> {
      val argument = singleString(scope)
      TODO()
    }
  }
}

private fun CompilerContext.singleString(scope: VerbAnnotationScope): String? {
  return when (scope.arguments.size) {
    1 -> {
      val arg = scope.arguments.single()
      TODO()
    }
    else -> TODO()/*parsingError("", scope.annotation)*/
  }
}