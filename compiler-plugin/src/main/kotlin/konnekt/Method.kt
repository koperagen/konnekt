package konnekt

data class Method(
  val name: String,
  val httpVerb: VerbAnnotationModel,
  val headers: HeadersAnnotationModel?,
  val encoding: MimeEncoding? = null,
  val params: List<Parameter>,
  val returnType: Type
)

enum class Verb {
  GET, DELETE, HEAD, OPTIONS, PATCH, POST, PUT, HTTP;
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

data class HeadersAnnotationModel(val headers: List<String>)

sealed class MimeEncoding

object FormUrlEncoded : MimeEncoding()

object Multipart : MimeEncoding()


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

data class Part(val value: String) : SourceAnnotation("Part")

data class Field(val value: String, val encoded: Boolean = false) : SourceAnnotation("Field")

data class Header(val value: String) : SourceAnnotation("Header")