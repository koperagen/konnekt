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
""".trimIndent()

val classesStub = """
data class Book(
  val url: String,
  val name: String,
  val isbn: String,
  val authors: List<String>,
  val numberOfPages: String,
  val publisher: String,
  val country: String,
  val mediaType: String,
  val released: String,
  val characters: List<String>,
  val povCharacters: List<String>
)

data class Character(
  val url: String,
  val name: String,
  val gender: String,
  val culture: String,
  val born: String,
  val died: String,
  val titles: List<String>,
  val aliases: List<String>,
  val father: String,
  val mother: String,
  val spouse: String,
  val allegiances: List<String>,
  val books: List<String>,
  val povBooks: List<String>,
  val tvSeries: List<String>,
  val playedBy: List<String>
)
""".trimIndent()

val declaration = """
//metadebug
$imports
$prelude
$classesStub
@Client
interface IceAndFireClient {
  @GET("/books")
  suspend fun listBooks(
      @Query("name") name: String? = null,
      @Query("fromReleaseDate") fromReleaseDate: LocalDateTime? = null,
      @Query("toReleaseDate") toReleaseDate: LocalDateTime? = null
  ): List<Book>

  @GET("/books/{id}")
  suspend fun bookById(@Path("id", false) id: Int): Book

  @GET("/characters")
  suspend fun listCharacters(): List<Character>

  @GET("/characters/{id}")
  suspend fun characterById(@Path("id") id: Int): Character
  
  companion object
}

val klient = HttpClient(MockEngine) {
  engine {
    addHandler { request ->
      when (val fullUrl = request.url.fullUrl) {
        "https://anapioficeandfire.com/api/books" -> respondJson(resourceContent("books.json"))
        "https://anapioficeandfire.com/api/books/1" -> respondJson(resourceContent("book_1.json"))
        "https://anapioficeandfire.com/api/characters/1" -> respondJson(resourceContent("character_1.json"))
        else -> error("fullUrl=${'$'}fullUrl request=${'$'}request")
      }
    }
  }

  install(JsonFeature) {
    serializer = JacksonSerializer()
  }

  Logging {
    logger = io.ktor.client.features.logging.Logger.Companion.DEFAULT
    level = LogLevel.ALL
  }

  defaultRequest {
    url {
      protocol = URLProtocol.HTTPS
      host = "anapioficeandfire.com"
      encodedPath = "/api${'$'}encodedPath"
    }
  }
}

val api = IceAndFireClient(klient)

fun listBooks() = runBlocking { api.listBooks() }
fun bookById() = runBlocking { api.bookById(1) }
fun characterById() = runBlocking { api.characterById(1) }
""".trimIndent()

val implementation = """
//metadebug
$prelude
$classesStub
interface IceAndFireClient {
  @GET("/books")
  suspend fun listBooks(
      @Query("name") name: String? = null,
      @Query("fromReleaseDate") fromReleaseDate: LocalDateTime? = null,
      @Query("toReleaseDate") toReleaseDate: LocalDateTime? = null
  ): List<Book>

  @GET("/books/{id}")
  suspend fun bookById(@Path("id") id: Int): Book

  @GET("/characters")
  suspend fun listCharacters(): List<Character>

  @GET("/characters/{id}")
  suspend fun characterById(@Path("id") id: Int): Character

  companion object {
    operator fun invoke(client: HttpClient) : IceAndFireClient {
      return object : IceAndFireClient {
        override suspend fun listBooks(name: String?, fromReleaseDate: LocalDateTime?, toReleaseDate: LocalDateTime?): List<Book> {
          return client.get(path = "/books") {
            parameter("name", name)
            parameter("fromReleaseDate", fromReleaseDate)
            parameter("toReleaseDate", toReleaseDate)
          }
        }

        override suspend fun bookById(id: Int): Book {
          return client.get(path = "/books/${'$'}{id}")
        }

        override suspend fun listCharacters(): List<Character> {
          return client.get(path = "/characters")
        }

        override suspend fun characterById(id: Int): Character {
          return client.get(path = "/characters/${'$'}{id}")
        }
      }
    }
  }
}
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
