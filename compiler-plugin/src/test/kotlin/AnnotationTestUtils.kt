import konnekt.Source

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

fun annotationVariants(it: Source): List<String> {
  return when (it) {
    Source.BODY -> it.names.map { "@$it" }
    Source.QUERY, Source.FIELD, Source.PATH -> (it.names product stringLiterals.named("value"))
        .let { oneArg ->
          val stringOnly = oneArg.map { (name, str) -> "@$name($str)" }
          val complete = (oneArg product booleanLiterals.named("encoded")).map { (name, str, bool) -> "@$name($str, $bool)" }
          stringOnly + complete
        }
    Source.PART, Source.HEADER -> (it.names product stringLiterals.named("value"))
        .map { (name, str) -> "@$name($str)" }
  }
}

fun functions(source: Source): List<String> {
  val annotations = annotationVariants(source)
  return when (source) {
    Source.PATH -> annotations.mapIndexed { i, annotation ->
      """|@GET("/test/{p}")
         |suspend fun test${source.name}$i($annotation r: Int): String""".trimMargin()
    }
    Source.BODY, Source.QUERY, Source.PART, Source.FIELD, Source.HEADER -> annotations.mapIndexed { i, annotation ->
      """|@GET("/test")
         |suspend fun test${source.name}$i($annotation r: Int): String""".trimMargin()
    }
  }
}