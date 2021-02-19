package konnekt

import konnekt.prelude.Body
import konnekt.prelude.Client
import konnekt.prelude.DELETE
import konnekt.prelude.Field
import konnekt.prelude.FormUrlEncoded
import konnekt.prelude.GET
import konnekt.prelude.HEAD
import konnekt.prelude.HTTP
import konnekt.prelude.Headers
import konnekt.prelude.Multipart
import konnekt.prelude.OPTIONS
import konnekt.prelude.PATCH
import konnekt.prelude.POST
import konnekt.prelude.PUT
import konnekt.prelude.Part
import konnekt.prelude.Path
import konnekt.prelude.Query
import konnekt.prelude.Header
import konnekt.prelude.HeaderMap

val CLIENT_ANNOTATION_NAMES = setOf(Client::class.java.simpleName, Client::class.java.name)

interface AnnotationDeclaration {
  val declaration: Class<*>
}

val AnnotationDeclaration.names: List<String> get() = listOf(declaration.simpleName, declaration.name)

val SOURCE_ANNOTATIONS = setOf(
    Path::class.java,
    Body::class.java,
    Query::class.java,
    Field::class.java,
    Part::class.java,
    Header::class.java
)

enum class SourcesDeclaration(override val declaration: Class<*>): AnnotationDeclaration {
  BODY(Body::class.java),
  QUERY(Query::class.java),
  PART(Part::class.java),
  FIELD(Field::class.java),
  PATH(Path::class.java),
  HEADER(Header::class.java);
}

val HEADERS_ANNOTATIONS = setOf(
    Headers::class.java
)

object HeadersDeclaration : AnnotationDeclaration {
  override val declaration = Headers::class.java
}

val ENCODING_ANNOTATIONS = setOf(
    Multipart::class.java,
    FormUrlEncoded::class.java
)

enum class MimeEncodingsDeclaration(override val declaration: Class<*>) : AnnotationDeclaration {
  MULTIPART(Multipart::class.java), FORM_URL_ENCODED(FormUrlEncoded::class.java)
}

val VERB_ANNOTATIONS = setOf(
    HTTP::class.java,
    GET::class.java,
    POST::class.java,
    PUT::class.java,
    PATCH::class.java,
    DELETE::class.java,
    HEAD::class.java,
    OPTIONS::class.java
)

enum class VerbsDeclaration(override val declaration: Class<*>): AnnotationDeclaration {
  Http(HTTP::class.java),
  Get(GET::class.java),
  Post(POST::class.java),
  Put(PUT::class.java),
  Patch(PATCH::class.java),
  Delete(DELETE::class.java),
  Head(HEAD::class.java),
  Options(OPTIONS::class.java)
}