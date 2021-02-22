package konnekt

import arrow.meta.phases.CompilerContext

fun <A : SourceAnnotation> Parameter.toTypedParameter(annotation: A): TypedParameter<A> = TypedParameter(annotation, name, type)

interface RefinedParametersContext {
  val pathParams: List<PathParameter>
  val bodyParams: List<BodyParameter>
  val queryParams: List<QueryParameter>
  val partParams: List<PartParameter>
  val fieldParams: List<FieldParameter>
  val headerParams: List<HeaderParameter>
}

fun <T> CompilerContext.withRefinedParameters(method: Method, f: CompilerContext.(RefinedParametersContext) -> T): T {
  val pathParams = mutableListOf<PathParameter>()
  val bodyParams = mutableListOf<BodyParameter>()
  val queryParams = mutableListOf<QueryParameter>()
  val partParams = mutableListOf<PartParameter>()
  val fieldParams = mutableListOf<FieldParameter>()
  val headerParams = mutableListOf<HeaderParameter>()

  method.params.forEach {
    when (it.annotation) {
      is Path -> pathParams.add(it.toTypedParameter(it.annotation))
      is Body -> bodyParams.add(TypedParameter(it.annotation, it.name, it.type))
      is Query -> queryParams.add(TypedParameter(it.annotation, it.name, it.type))
      is Part -> partParams.add(TypedParameter(it.annotation, it.name, it.type))
      is Field -> fieldParams.add(TypedParameter(it.annotation, it.name, it.type))
      is Header -> headerParams.add(TypedParameter(it.annotation, it.name, it.type))
    }
  }

  val context = object : RefinedParametersContext {
    override val pathParams: List<PathParameter> = pathParams
    override val bodyParams: List<BodyParameter> = bodyParams
    override val queryParams: List<QueryParameter> = queryParams
    override val partParams: List<PartParameter> = partParams
    override val fieldParams: List<FieldParameter> = fieldParams
    override val headerParams: List<HeaderParameter> = headerParams
  }

  return f(context)
}

fun CompilerContext.verify(method: Method): SimpleRequest? = withRefinedParameters(method) {
  when (method.encoding) {
    is FormUrlEncoded -> null
    is Multipart -> null
    null -> {
      require(it.fieldParams.isEmpty())
      require(it.partParams.isEmpty())
      val body = when (it.bodyParams.size) {
        0 -> null
        1 -> it.bodyParams[0]
        else -> TODO()
      }

      SimpleRequest(
          method.name,
          method.httpVerb,
          method.headers,
          body,
          it.queryParams,
          it.pathParams,
          it.headerParams,
          method.returnType,
          method.params
      )
    }
  }
}

data class SimpleRequest(
  val name: String,
  val httpVerb: VerbAnnotationModel,
  val headers: HeadersAnnotationModel?,
  val body: BodyParameter?,
  val queryParameters: List<QueryParameter>,
  val pathParameters: List<PathParameter>,
  val headerParameters: List<HeaderParameter>,
  val returnType: Type,
  val params: List<Parameter>
)