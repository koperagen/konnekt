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

fun CompilerContext.verify(method: Method): Request? = withRefinedParameters(method) {
  verifyPath(method.httpVerb.path, it.pathParams) ?: return@withRefinedParameters null

  val noFieldParameters: () -> Boolean = {
    if (it.fieldParams.isNotEmpty()) {
      parsingError<Unit>(method.name.requiredEncoding(MimeEncodingsDeclaration.FORM_URL_ENCODED))
      false
    } else {
      true
    }
  }

  val noPartParameters: () -> Boolean = {
    if (it.partParams.isNotEmpty()) {
      parsingError<Unit>(method.name.requiredEncoding(MimeEncodingsDeclaration.MULTIPART))
      false
    } else {
      true
    }
  }

  val noBodyParameters: (String) -> Boolean = { annotation ->
    if (it.bodyParams.isNotEmpty()) {
      parsingError<Unit>("Method ${method.name} cannot have both @${annotation} encoding and @Body parameter")
      false
    } else {
      true
    }
  }

  when (method.encoding) {
    is FormUrlEncoded -> computeIf(noPartParameters, { noBodyParameters("FormUrlEncoded") }) {
      FormUrlEncodedRequest(
          method.name,
          method.httpVerb,
          method.headers,
          it.fieldParams,
          it.queryParams,
          it.pathParams,
          it.headerParams,
          method.returnType,
          method.params
      )
    }
    is Multipart -> computeIf(noFieldParameters, { noBodyParameters("Multipart") }) {
      MultipartRequest(
          method.name,
          method.httpVerb,
          method.headers,
          it.partParams,
          it.queryParams,
          it.pathParams,
          it.headerParams,
          method.returnType,
          method.params
      )
    }
    null -> computeIf(noFieldParameters, noPartParameters) {
      val body = when (it.bodyParams.size) {
        0 -> null
        1 -> it.bodyParams[0]
        else -> return@computeIf parsingError(method.name.severalBodyParameters)
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

private fun CompilerContext.verifyPath(path: HttpPath, params: List<PathParameter>): Unit? {
  val variables = PATH_VARIABLE_PATTERN.findAll(path)
  val variableNames = variables.map { it.groups[1]!!.value }.toSet()
  val paramsNames = params.map { it.annotation.placeholder }.toSet()
  val unusedVariables = variableNames - paramsNames
  val unusedArguments = paramsNames - variableNames
  return when {
    unusedVariables.isNotEmpty() -> {
      parsingError("URL Template variables [${unusedVariables.joinToString()}] has no matching function arguments")
    }
    unusedArguments.isNotEmpty() -> {
      parsingError("@Path arguments [${unusedArguments.joinToString()}] has no matching URL Template variable")
    }
    else -> Unit
  }
}

private val PATH_VARIABLE_PATTERN = """\{(.+?)}""".toRegex()

sealed class Request {
  abstract val name: String
  abstract val httpVerb: VerbAnnotationModel
  abstract val headers: HeadersAnnotationModel?
  abstract val queryParameters: List<QueryParameter>
  abstract val pathParameters: List<PathParameter>
  abstract val headerParameters: List<HeaderParameter>
  abstract val returnType: Type
  abstract val params: List<Parameter>
}

data class SimpleRequest(
  override val name: String,
  override val httpVerb: VerbAnnotationModel,
  override val headers: HeadersAnnotationModel?,
  val body: BodyParameter?,
  override val queryParameters: List<QueryParameter>,
  override val pathParameters: List<PathParameter>,
  override val headerParameters: List<HeaderParameter>,
  override val returnType: Type,
  override val params: List<Parameter>
) : Request()

data class MultipartRequest(
  override val name: String,
  override val httpVerb: VerbAnnotationModel,
  override val headers: HeadersAnnotationModel?,
  val parts: List<PartParameter>,
  override val queryParameters: List<QueryParameter>,
  override val pathParameters: List<PathParameter>,
  override val headerParameters: List<HeaderParameter>,
  override val returnType: Type,
  override val params: List<Parameter>
) : Request()

data class FormUrlEncodedRequest(
  override val name: String,
  override val httpVerb: VerbAnnotationModel,
  override val headers: HeadersAnnotationModel?,
  val fields: List<FieldParameter>,
  override val queryParameters: List<QueryParameter>,
  override val pathParameters: List<PathParameter>,
  override val headerParameters: List<HeaderParameter>,
  override val returnType: Type,
  override val params: List<Parameter>
) : Request()