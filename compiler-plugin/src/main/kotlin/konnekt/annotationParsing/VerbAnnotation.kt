package konnekt.annotationParsing

import arrow.meta.phases.CompilerContext
import konnekt.Verb
import konnekt.VerbAnnotationModel
import konnekt.VerbsDeclaration
import konnekt.parsingError
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

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
  return "Method $name of ${containingClass()?.name} should be annotated with one of [${VerbsDeclaration.values().joinToString(",") { it.declaration.simpleName }}]"
}

private fun KtNamedFunction.methodHasSeveralAnnotations(annotations: List<VerbAnnotationScope>): String {
  return """Method $name of ${containingClass()?.name} should be annotated with one of [${VerbsDeclaration.values().joinToString(",") { it.declaration.simpleName }}], but got ${annotations.size}: ${annotations.joinToString { it.annotation.name ?: "" }}"""
}

class VerbAnnotationScope(
    val annotation: KtAnnotationEntry,
    val verb: Verb,
    val arguments: List<KtValueArgument> = annotation.valueArgumentList?.arguments ?: emptyList()
)

fun verbAnnotation(annotationEntry: KtAnnotationEntry): VerbAnnotationScope? {
  val verb = when (annotationEntry.typeReference?.typeElement?.safeAs<KtUserType>()?.referencedName) {
    "GET" -> Verb.GET
    "DELETE" -> Verb.DELETE
    "HEAD" -> Verb.HEAD
    "OPTIONS" -> Verb.OPTIONS
    "PATCH" -> Verb.PATCH
    "POST" -> Verb.POST
    "PUT" -> Verb.PUT
    "HTTP" -> Verb.HTTP
    else -> null
  }
  return verb?.let { VerbAnnotationScope(annotationEntry, it) }
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
