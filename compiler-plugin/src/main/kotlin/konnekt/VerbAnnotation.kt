package konnekt

import arrow.meta.phases.CompilerContext
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.containingClass

internal fun KtNamedFunction.verbs(ctx: CompilerContext): VerbAnnotationModel? {
  val verbAnnotations = annotationEntries.mapNotNull { verbAnnotation(it) }
  return when (verbAnnotations.size) {
    0 -> ctx.parsingError(methodNotAnnotated(), this)
    1 -> {
      val scope = verbAnnotations.first()
      ctx.refine(scope)
    }
    else -> ctx.parsingError(methodHasSeveralAnnotations(verbAnnotations), this)
  }
}

private fun KtNamedFunction.methodNotAnnotated(): String {
  return "Method $name of ${containingClass()?.name} should be annotated with one of [${Source.values().joinToString(",") { it.declaration.simpleName }}]"
}

private fun KtNamedFunction.methodHasSeveralAnnotations(annotations: List<VerbAnnotationScope>): String {
  return """Method $name of ${containingClass()?.name} should be annotated with one of [${Source.values().joinToString(",") { it.declaration.simpleName }}], but got ${annotations.size}: ${annotations.joinToString { it.annotation.name ?: "" }}"""
}

private fun CompilerContext.refine(scope: VerbAnnotationScope): VerbAnnotationModel? {
  return when (scope.verb) {
    Verb.HTTP -> twoString(scope)?.let { (verb, path) ->
      VerbAnnotationModel(verb, path)
    }
    else -> singleString(scope)?.let { path ->
      VerbAnnotationModel(scope.verb.string(), path)
    }
  }
}

private fun CompilerContext.singleString(scope: VerbAnnotationScope): String? {
  return when (scope.arguments.size) {
    1 -> {
      val arg = scope.arguments.single()
      constantStringOrNull(arg)
    }
    else -> parsingError("${scope.verb} should contain exactly 1 argument: URL pattern")
  }
}

private fun CompilerContext.twoString(scope: VerbAnnotationScope): Pair<String, String>? {
  return when (scope.arguments.size) {
    2 -> {
      val arg1 = scope.arguments[0]
      val arg2 = scope.arguments[1]
      constantStringOrNull(arg1)?.let { l -> constantStringOrNull(arg2)?.let { r -> l to r } }
    }
    else -> parsingError("${scope.verb} should contain exactly 2 arguments: HTTP verb and URL pattern")
  }
}

private fun Verb.string(): String = name.toLowerCase()
