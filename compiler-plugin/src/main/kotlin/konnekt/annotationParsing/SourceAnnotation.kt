package konnekt.annotationParsing

import arrow.meta.phases.CompilerContext
import konnekt.Body
import konnekt.Field
import konnekt.Header
import konnekt.Parameter
import konnekt.Part
import konnekt.Path
import konnekt.Query
import konnekt.SourceAnnotation
import konnekt.SourcesDeclaration
import konnekt.parsingError
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun KtNamedFunction.parameters(ctx: CompilerContext): List<Parameter>? {
    val params = valueParameters.map { ParameterScope(it) }
    return params.map {
        val type = it.type?.text ?: return ctx.parsingError("${it.parameter.name}'s type is absent")
        val source = ctx.refine(it) ?: return null
        Parameter(source, it.name, type)
    }
}

class ParameterScope(
    val parameter: KtParameter,
    val sourceAnnotations: List<SourceAnnotationScope> = parameter.annotationEntries.mapNotNull { sourceAnnotation(it) },
    val name: String = parameter.nameAsSafeName.identifier,
    val type: KtTypeReference? = parameter.typeReference
)

class SourceAnnotationScope(val annotationEntry: KtAnnotationEntry, val source: SourcesDeclaration)

fun sourceAnnotation(annotationEntry: KtAnnotationEntry): SourceAnnotationScope? {
  val source = when (annotationEntry.typeReference?.typeElement?.safeAs<KtUserType>()?.referencedName) {
    null -> null
    "Path" -> SourcesDeclaration.PATH
    "Body" -> SourcesDeclaration.BODY
    "Query" -> SourcesDeclaration.QUERY
    "Part" -> SourcesDeclaration.PART
    "Header" -> SourcesDeclaration.HEADER
    "Field" -> SourcesDeclaration.FIELD
    else -> null
  }
  return source?.let { SourceAnnotationScope(annotationEntry, source) }
}

private fun CompilerContext.refine(scope: ParameterScope): SourceAnnotation? {
    val annotation = when (scope.sourceAnnotations.size) {
      1 -> scope.sourceAnnotations[0]
      else -> return parsingError("Value parameter ${scope.name} should be annotated with 1 source annotation, but was ${scope.parameter}")
    }

    fun body(annotationEntry: KtAnnotationEntry): Body? {
        return if (annotationEntry.valueArguments.size == 0) Body
        else parsingError("$annotationEntry should not have value arguments")
    }

    fun query(annotationEntry: KtAnnotationEntry): Query? {
        return when (annotationEntry.valueArguments.size) {
            1 -> withArgumentResolvingContext(annotationEntry, setOf("value")) {
                val value = get("value", 0, converter = CompilerContext::constantStringOrNull)
                value?.let { Query(value) }
            }
            2 -> withArgumentResolvingContext(annotationEntry, setOf("value", "encoded")) {
                val value = get("value", 0, converter = CompilerContext::constantStringOrNull)
                val encoded = get("encoded", 1, defaultValue = true, converter = CompilerContext::constantBooleanOrNull)
                if (encoded != null) {
                    value?.let { Query(it, encoded) }
                } else {
                    value?.let { Query(it) }
                }
            }
            else -> parsingError("${annotationEntry.text} should have 2 arguments: value and optional encoded")
        }
    }

    fun path(annotationEntry: KtAnnotationEntry): Path? {
        return when (annotationEntry.valueArguments.size) {
            1 -> withArgumentResolvingContext(annotationEntry, setOf("value")) {
                val value = get("value", 0, converter = CompilerContext::constantStringOrNull)
                value?.let { Path(value) }
            }
            2 -> withArgumentResolvingContext(annotationEntry, setOf("value", "encoded")) {
              val value = get("value", 0, converter = CompilerContext::constantStringOrNull)
              val encoded = get("encoded", 1, defaultValue = true, converter = CompilerContext::constantBooleanOrNull)
              if (encoded != null) {
                value?.let { Path(it, encoded) }
              } else {
                value?.let { Path(it) }
              }
            }
            else -> parsingError("${annotationEntry.text} should have 2 arguments: value and optional encoded")
        }
    }

    fun part(annotationEntry: KtAnnotationEntry): Part? {
      return when (annotationEntry.valueArguments.size) {
        1 -> withArgumentResolvingContext(annotationEntry, setOf("value")) {
          val value = get("value", 0, converter = CompilerContext::constantStringOrNull)
          value?.let { Part(value) }
        }
        else -> parsingError("${annotationEntry.text} should have 1 argument: value")
      }
    }

    fun field(annotationEntry: KtAnnotationEntry): Field? {
        return when (annotationEntry.valueArguments.size) {
            1 -> withArgumentResolvingContext(annotationEntry, setOf("value")) {
                val value = get("value", 0, converter = CompilerContext::constantStringOrNull)
                value?.let { Field(value) }
            }
            2 -> withArgumentResolvingContext(annotationEntry, setOf("value", "encoded")) {
                val value = get("value", 0, converter = CompilerContext::constantStringOrNull)
                val encoded = get("encoded", 1, defaultValue = true, converter = CompilerContext::constantBooleanOrNull)
                if (encoded != null) {
                  value?.let { Field(it, encoded) }
                } else {
                  value?.let { Field(it) }
                }
            }
            else -> parsingError("${annotationEntry.text} should have 2 arguments: value and optional encoded")
        }
    }

    fun header(annotationEntry: KtAnnotationEntry): Header? {
        return when (annotationEntry.valueArguments.size) {
          1 -> withArgumentResolvingContext(annotationEntry, setOf("value")) {
            val value = get("value", 0, converter = CompilerContext::constantStringOrNull)
            value?.let { Header(value) }
          }
          else -> parsingError("${annotationEntry.text} should have 1 argument: value")
        }
    }

    return when (annotation.source) {
        SourcesDeclaration.BODY -> body(annotation.annotationEntry)
        SourcesDeclaration.QUERY -> query(annotation.annotationEntry)
        SourcesDeclaration.PART -> part(annotation.annotationEntry)
        SourcesDeclaration.FIELD -> field(annotation.annotationEntry)
        SourcesDeclaration.PATH -> path(annotation.annotationEntry)
        SourcesDeclaration.HEADER -> header(annotation.annotationEntry)
    }
}
