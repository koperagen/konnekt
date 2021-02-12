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

val SOURCE_ANNOTATIONS = setOf(
    Path::class.java,
    Body::class.java,
    Query::class.java,
    Field::class.java,
    Part::class.java,
    Header::class.java
)

enum class Source(val declaration: Class<*>) {
  BODY(Body::class.java),
  QUERY(Query::class.java),
  PART(Part::class.java),
  FIELD(Field::class.java),
  PATH(Path::class.java),
  HEADER(Header::class.java);

  val names: List<String> = listOf(declaration.simpleName, declaration.name)
}

val HEADERS_ANNOTATIONS = setOf(
    Headers::class.java
)

val ENCODING_ANNOTATIONS = setOf(
    Multipart::class.java,
    FormUrlEncoded::class.java
)

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