package konnekt

import arrow.meta.phases.analysis.ElementScope
import arrow.meta.quotes.nameddeclaration.stub.typeparameterlistowner.NamedFunction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.addRemoveModifier.addModifier
import substituteParams

data class Method(
    val name: String,
    val httpVerb: VerbAnnotation,
    val headers: List<HeaderAnnotation>,
    val encoding: MimeEncoding? = null,
    val params: List<Parameter>,
    val returnType: Type
)

data class VerbAnnotation(val verb: String, val path: HttpPath) {
  companion object {
    private const val GET = "get"
    private const val DELETE = "delete"
    private const val HEAD = "head"
    private const val OPTIONS = "options"
    private const val PATCH = "patch"
    private const val POST = "post"
    private const val PUT = "put"

    fun get(path: HttpPath) = VerbAnnotation(GET, path)
    fun delete(path: HttpPath) = VerbAnnotation(DELETE, path)
    fun head(path: HttpPath) = VerbAnnotation(HEAD, path)
    fun options(path: HttpPath) = VerbAnnotation(OPTIONS, path)
    fun patch(path: HttpPath) = VerbAnnotation(PATCH, path)
    fun post(path: HttpPath) = VerbAnnotation(POST, path)
    fun put(path: HttpPath) = VerbAnnotation(PUT, path)
  }
}

data class HeaderAnnotation(val headers: List<String>)

enum class MimeEncoding {
    MULTIPART, FORM_URL_ENCODED
}

data class Parameter(
    val annotation: SourceAnnotation,
    val name: String,
    val type: Type
)

class TypedParameter<A: SourceAnnotation>(val annotation: A, val name: String, val type: Type)

sealed class UrlPart

object Constant: UrlPart()

data class Placeholder(var value: String = "") : UrlPart()

typealias Type = String

typealias HttpPath = String

sealed class SourceAnnotation(val name: String)

data class Path(val placeholder: String, val encoded: Boolean = false) : SourceAnnotation("Path") {
  override fun toString(): String {
    return """@Path(placeholder = "$placeholder", encoded = $encoded)"""
  }
}

object Body : SourceAnnotation("Body")

// TODO Check default param for encoded
data class Query(val key: String, val encoded: Boolean = false) : SourceAnnotation("Query")

data class Part(val key: String, val encoding: Boolean = false) : SourceAnnotation("Part")

data class Field(val key: String) : SourceAnnotation("Field")

// TODO HeaderMap + Header annotation
object Header : SourceAnnotation("Header")

fun Method.render(): String {
    fun List<Parameter>.render() = joinToString { "${it.name}: ${it.type}" }

    fun renderPath(verbAnnotation: VerbAnnotation, params: List<Parameter>) = substituteParams(verbAnnotation.path, params)

    return """
    override suspend fun $name(${params.render()}): $returnType {
        return client.${httpVerb.verb.toLowerCase()}(path = "${renderPath(httpVerb, params)}") {
            ${params.filterQueries().joinToString("\n") {
                    it.render()
                }
            }
        }
    }
    """.trimIndent()
}

fun ElementScope.render(method: Method): NamedFunction = method.render().function.apply {
  addModifier(owner = value, modifier = KtTokens.OVERRIDE_KEYWORD)
}

fun List<Parameter>.filterQueries() = mapNotNull { parameter ->
  (parameter.annotation as? Query)?.let { annotation -> TypedParameter(annotation, parameter.name, parameter.type) }
}

fun TypedParameter<Query>.render() = """parameter("${annotation.key}", $name)"""

//fun Map<String, UrlPart>.substituteParams(pathParams: List<Parameter>): String {
//    pathParams.forEach { parameter ->
//        val placeholder = parameter.annotation.safeAs<Path>()?.placeholder ?: return@forEach
//        get(placeholder)?.safeAs<Placeholder>()?.value = parameter.name
//        println(parameter.name)
//    }
//    println(entries.joinToString { (string, part) -> "string: $string part $part" })
//    return entries.joinToString("/", "\"/", postfix = "\"") { (key, part) ->
//        when(part) {
//            Constant -> key
//            is Placeholder -> "\${${part.value}}"
//        }
//    }
//}

//fun NamedFunction.implement(): NamedFunction {
//    `(params)`.map { it.typeReference }
//}