package konnekt

import arrow.meta.phases.analysis.ElementScope
import arrow.meta.quotes.nameddeclaration.stub.typeparameterlistowner.NamedFunction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.addRemoveModifier.addModifier
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

data class Method(
  val name: String,
  val httpVerb: VerbAnnotationModel,
  val headers: HeadersAnnotation?,
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

data class VerbAnnotationModel(val verb: String, val path: HttpPath) {
  companion object {
    private const val GET = "get"
    private const val DELETE = "delete"
    private const val HEAD = "head"
    private const val OPTIONS = "options"
    private const val PATCH = "patch"
    private const val POST = "post"
    private const val PUT = "put"

    fun get(path: HttpPath) = VerbAnnotationModel(GET, path)
    fun delete(path: HttpPath) = VerbAnnotationModel(DELETE, path)
    fun head(path: HttpPath) = VerbAnnotationModel(HEAD, path)
    fun options(path: HttpPath) = VerbAnnotationModel(OPTIONS, path)
    fun patch(path: HttpPath) = VerbAnnotationModel(PATCH, path)
    fun post(path: HttpPath) = VerbAnnotationModel(POST, path)
    fun put(path: HttpPath) = VerbAnnotationModel(PUT, path)
  }
}

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

data class Query(val value: String, val encoded: Boolean = false) : SourceAnnotation("Query")

data class Part(val value: String, val encoding: String) : SourceAnnotation("Part")

data class Field(val value: String, val encoded: Boolean = false) : SourceAnnotation("Field")

// TODO HeaderMap
data class Header(val value: String) : SourceAnnotation("Header")

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

fun TypedParameter<Query>.render() = """parameter("${annotation.value}", $name)"""