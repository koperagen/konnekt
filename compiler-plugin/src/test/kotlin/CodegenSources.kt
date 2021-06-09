val prelude = """
  fun Any.resourceContent(name: String) = javaClass.classLoader.getResource(name)?.readText() ?: error("Not fount resource file ${'$'}name")
  fun MockRequestHandleScope.respondJson(file: File) = respondJson(file.readText())
  fun MockRequestHandleScope.respondJson(content: String): HttpResponseData =
      respond(content, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
  private val Url.hostWithPortIfRequired: String get() = if (port == protocol.defaultPort) host else hostWithPort
  private val Url.fullUrl: String get() = "${'$'}{protocol.name}://${'$'}hostWithPortIfRequired${'$'}fullPath"
""".trimIndent()

val imports = """
  import io.ktor.client.HttpClient
  import io.ktor.client.engine.mock.*
  import io.ktor.client.features.defaultRequest
  import io.ktor.client.features.json.*
  import io.ktor.client.features.logging.DEFAULT
  import io.ktor.client.features.logging.LogLevel
  import io.ktor.client.features.logging.Logging
  import io.ktor.client.request.*
  import io.ktor.http.*
  import io.ktor.http.Url
  import kotlinx.coroutines.runBlocking
  import java.io.File
  import java.time.LocalDateTime
  import konnekt.prelude.*
  import konnekt.prelude.Headers
""".trimIndent()

val code = """
  package konnekt

  import io.ktor.client.*
  import io.ktor.client.engine.mock.*
  import io.ktor.http.*
  import io.ktor.http.content.*
  import konnekt.prelude.Body
  import konnekt.prelude.Client
  import konnekt.prelude.Field
  import konnekt.prelude.GET
  import konnekt.prelude.Header
  import konnekt.prelude.Path
  import konnekt.prelude.Query
  import konnekt.prelude.FormUrlEncoded
  import kotlinx.coroutines.runBlocking

  //metadebug

  @Client
  interface Sources {
    @GET("/query")
    suspend fun query(@Query("p") q: Int): String

    @GET("/body")
    suspend fun body(@Body json: String): String

    @GET("/path/{page}")
    suspend fun path(@Path("page") page: Int): String

    @GET("/header")
    suspend fun header(@Header("p") h: Int): String

    @FormUrlEncoded()
    @GET("/field")
    suspend fun field(@Field("p") f: Int): String

    companion object
  }


  val client = HttpClient(MockEngine) {
    engine {
      addHandler {
        when (val path = it.url.fullPath) {
          "/query?p=1" -> respondOk()
          "/body" -> {
            assert(it.body.toByteArray().decodeToString() == ""${'"'}{"a": "b"}""${'"'})
            respondOk()
          }
          "/path/1" -> respondOk()
          "/header" -> {
            //println(it.headers)
            //assert(it.headers.contains("p", "1"))
            respondOk() 
          }
          "/field" -> {
            assert(it.body.toByteArray().decodeToString() == "p=1")
            respondOk()
          }
          else -> error("${'$'}path not handled")
        }
      }
    }
  }
  

  val api: Sources = Sources.invoke(client)

  fun query_test() = runBlocking { api.query(1) }

  fun body_test() = runBlocking { api.body(""${'"'}{"a": "b"}""${'"'}) }

  fun path_test() = runBlocking { api.path(1) }

  fun header_test() = runBlocking { api.header(1) }

  fun field_test() = runBlocking { api.field(1) }
""".trimIndent()
