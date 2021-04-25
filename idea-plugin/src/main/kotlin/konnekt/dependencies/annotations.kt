package konnekt.dependencies

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

val CLIENT_ANNOTATION_NAMES = setOf(Client::class.java.simpleName, Client::class.java.name)

interface AnnotationDeclaration {
  val declaration: Class<*>
}

val AnnotationDeclaration.names: List<String> get() = listOf(declaration.simpleName, declaration.name)

val AnnotationDeclaration.fqEntry: String get() = "@${declaration.name}"
val AnnotationDeclaration.simpleEntry: String get() = "@${declaration.simpleName}"

object ClientDeclaration : AnnotationDeclaration {
  override val declaration: Class<*> = Client::class.java
}

enum class SourcesDeclaration(override val declaration: Class<*>): AnnotationDeclaration {
  BODY(Body::class.java),
  QUERY(Query::class.java),
  PART(Part::class.java),
  FIELD(Field::class.java),
  PATH(Path::class.java),
  HEADER(Header::class.java);
}

object HeadersDeclaration : AnnotationDeclaration {
  override val declaration = Headers::class.java
}

enum class MimeEncodingsDeclaration(
  override val declaration: Class<*>,
  val components: List<AnnotationDeclaration>
) : AnnotationDeclaration {

  MULTIPART(Multipart::class.java, listOf(SourcesDeclaration.PART)),
  FORM_URL_ENCODED(FormUrlEncoded::class.java, listOf(SourcesDeclaration.FIELD))

}

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