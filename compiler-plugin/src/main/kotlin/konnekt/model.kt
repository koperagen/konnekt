package konnekt

import arrow.meta.phases.analysis.ElementScope
import arrow.meta.quotes.nameddeclaration.stub.typeparameterlistowner.NamedFunction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.addRemoveModifier.addModifier
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

data class Method(
  val name: String,
  val httpVerb: VerbAnnotation,
  val headers: List<HeaderAnnotation>,
  val encoding: MimeEncoding? = null,
  val params: List<Parameter>,
  val returnType: Type
)

class MethodScope(
  val function: KtNamedFunction,
  val name: String = function.nameAsSafeName.identifier,
  val httpVerbs: List<VerbAnnotationScope> = function.annotationEntries.mapNotNull { verbAnnotation(it) },
  val encoding: List<MimeEncodingScope> = function.annotationEntries.mapNotNull { mimeEncoding(it) },
  val params: List<ParameterScope> = function.valueParameters.map { ParameterScope(it) },
  val returnType: KtTypeReference? = function.typeReference
)

class VerbAnnotationScope(
  val annotation: KtAnnotationEntry,
  val verb: Verb,
  val arguments: List<KtValueArgument> = annotation.valueArgumentList?.arguments ?: emptyList()
)

fun verbAnnotation(annotationEntry: KtAnnotationEntry): VerbAnnotationScope? {
  val verb =  when (annotationEntry.typeReference?.typeElement?.safeAs<KtUserType>()?.referencedName) {
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

enum class Verb {
  GET, DELETE, HEAD, OPTIONS, PATCH, POST, PUT, HTTP;
}

class MimeEncodingScope(annotationEntry: KtAnnotationEntry, encoding: MimeEncoding)

fun mimeEncoding(annotationEntry: KtAnnotationEntry): MimeEncodingScope? {
  val name = annotationEntry.typeReference?.typeElement?.safeAs<KtUserType>()?.referencedName
  return when (name) {
    multipartAnnotation -> MimeEncoding.MULTIPART
    formUrlEncodedAnnotation -> MimeEncoding.FORM_URL_ENCODED
    else -> null
  }?.let { encoding ->
    MimeEncodingScope(annotationEntry, encoding)
  }
}

class ParameterScope(
  val parameter: KtParameter,
  val sourceAnnotations: List<SourceAnnotationScope> = parameter.annotationEntries.mapNotNull { sourceAnnotation(it) },
  val name: String = parameter.nameAsSafeName.identifier,
  val type: KtTypeReference? = parameter.typeReference
)

class SourceAnnotationScope(val annotationEntry: KtAnnotationEntry, source: Source)

enum class Source {
  BODY, QUERY, PART, FIELD, PATH, HEADER
}

fun sourceAnnotation(annotationEntry: KtAnnotationEntry): SourceAnnotationScope? {
  val source = when (annotationEntry.typeReference?.typeElement?.safeAs<KtUserType>()?.referencedName) {
    null -> null
    "Path" -> Source.PATH
    "Body" -> Source.BODY
    "Query" -> Source.QUERY
    "Part" -> Source.PART
    "Header" -> Source.HEADER
    else -> null
  }
  return source?.let { SourceAnnotationScope(annotationEntry, source) }
}

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

data class TypedParameter<A : SourceAnnotation>(val annotation: A, val name: String, val type: Type)

typealias BodyParameter = TypedParameter<Body>

typealias QueryParameter = TypedParameter<Query>

typealias PartParameter = TypedParameter<Part>

typealias FieldParameter = TypedParameter<Field>

typealias PathParameter = TypedParameter<Path>

typealias HeaderParameter = TypedParameter<Header>

typealias Type = String

typealias HttpPath = String

sealed class SourceAnnotation(val annotationName: String)

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

  return """
    override suspend fun $name(${params.render()}): $returnType {
        return client.${httpVerb.verb.toLowerCase()}(path = "${substituteParams(httpVerb.path, params.filterPaths())}") {
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

fun List<Parameter>.filterQueries(): List<QueryParameter> = mapNotNull { parameter ->
  (parameter.annotation as? Query)?.let { annotation -> TypedParameter(annotation, parameter.name, parameter.type) }
}

fun List<Parameter>.filterPaths(): List<PathParameter> = mapNotNull { parameter ->
  parameter.annotation.safeAs<Path>()?.let { annotation -> TypedParameter(annotation, parameter.name, parameter.type) }
}

fun TypedParameter<Query>.render() = """parameter("${annotation.key}", $name)"""