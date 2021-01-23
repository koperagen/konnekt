package konnekt

import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtValueArgument

fun toSourceAnnotationOrNull(annotationEntry: KtAnnotationEntry, annotationName: String): SourceAnnotation? {

  return when (annotationName) {
    "Path" -> path(annotationEntry)
    "Body" -> body(annotationEntry)
    "Query" -> query(annotationEntry)
    "Part" -> part(annotationEntry)
    "Field" -> field(annotationEntry)
    else -> null
  }
}

val sourceAnnotations = setOf("Path", "Body", "Query", "Part", "Field")

private fun path(annotationEntry: KtAnnotationEntry): Path {
  val args = annotationEntry.valueArgumentList?.arguments
      ?: error("Argument list for path should contain 'id' and optoinally 'encoded'")
  val (value, encoded) = args.values()
  return if (encoded != null) {
    Path(value, encoded)
  } else {
    Path(value)
  }
}

private fun body(annotationEntry: KtAnnotationEntry): Body {
  return Body
}

private fun query(annotationEntry: KtAnnotationEntry): Query {
  val args = requireNotNull(annotationEntry.valueArgumentList?.arguments)
  val (key, encoded) = args.values()
  return if (encoded != null) {
    Query(key, encoded)
  } else {
    Query(key)
  }
}

private fun part(annotationEntry: KtAnnotationEntry): Part {
  val args = requireNotNull(annotationEntry.valueArgumentList?.arguments)
  val (key, encoded) = args.values()
  return if (encoded != null) {
    Part(key, encoded)
  } else {
    Part(key)
  }
}

private fun field(annotationEntry: KtAnnotationEntry): Field {
  val args = requireNotNull(annotationEntry.valueArgumentList?.arguments)
  val key = requireNotNull(args.getStringOrNull(0))
  return Field(key)
}

private fun List<KtValueArgument>.values(): Pair<String, Boolean?> =
    (getStringOrNull(0) ?: error("Expected first string param in argument list")) to
        getBooleanOrNull(1)