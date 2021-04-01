package konnekt

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