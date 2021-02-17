import konnekt.HeadersDeclaration
import konnekt.SourcesDeclaration
import konnekt.names

val stringLiterals = listOf(""""p"""", "\"\"")

val booleanLiterals = listOf("true", "false")

infix fun <T, E> List<T>.product(other: List<E>): List<Pair<T, E>> {
  return flatMap { l -> other.map { r -> l to r  } }
}

@JvmName("productTriple")
infix fun <T, E, L> List<Pair<T, E>>.product(other: List<L>): List<Triple<T, E, L>> {
  return flatMap { (l1, l2) -> other.map { r -> Triple(l1, l2, r)  } }
}

data class Argument(val value: String, val name: String? = null) {
  override fun toString(): String =
      if (name != null) {
        "$name = $value"
      } else {
        value
      }
}

fun List<String>.named(name: String): List<Argument> = flatMap {
  listOf(Argument(it, null), Argument(it, name))
}

fun annotationVariants(it: SourcesDeclaration): List<String> {
  return when (it) {
    SourcesDeclaration.BODY -> it.names.map { "@$it" }
    SourcesDeclaration.QUERY, SourcesDeclaration.FIELD, SourcesDeclaration.PATH -> (it.names product stringLiterals.named("value"))
        .let { oneArg ->
          val stringOnly = oneArg.map { (name, str) -> "@$name($str)" }
          val complete = (oneArg product booleanLiterals.named("encoded")).map { (name, str, bool) -> "@$name($str, $bool)" }
          stringOnly + complete
        }
    SourcesDeclaration.PART, SourcesDeclaration.HEADER -> (it.names product stringLiterals.named("value"))
        .map { (name, str) -> "@$name($str)" }
  }
}

fun functions(source: SourcesDeclaration): List<String> {
  val annotations = annotationVariants(source)
  return when (source) {
    SourcesDeclaration.PATH -> annotations.mapIndexed { i, annotation ->
      """|@GET("/test/{p}")
         |suspend fun test${source.name}$i($annotation r: Int): String""".trimMargin()
    }
    SourcesDeclaration.BODY, SourcesDeclaration.QUERY, SourcesDeclaration.PART, SourcesDeclaration.FIELD, SourcesDeclaration.HEADER -> annotations.mapIndexed { i, annotation ->
      """|@GET("/test")
         |suspend fun test${source.name}$i($annotation r: Int): String""".trimMargin()
    }
  }
}

val varargsHeaders = listOf(listOf("ff"))

fun headerAnnotationVariants(): List<String> {
  return (HeadersDeclaration.names product varargsHeaders).map { (name, args) -> "@$name(${args.joinToString(", "){ "\"$it\"" } })" }
}

fun headerFunctions(): List<String> {
  val annotations = headerAnnotationVariants()
  return annotations.mapIndexed { i, annotation ->
    """|@GET("/test")
       |$annotation
       |suspend fun testHEADERS_$i(): String""".trimMargin()
  }
}