import arrow.meta.plugin.testing.CompilerTest
import arrow.meta.plugin.testing.ConfigSyntax
import arrow.meta.plugin.testing.Dependency
import arrow.meta.plugin.testing.assertThis
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import konnekt.*
import java.lang.reflect.InvocationTargetException

@Suppress("unused")
class CodegenTest : FreeSpec({

  "!simple path" - {
    val path = "/test/{placeholder}"

    "!Parse raw path string to components " {
    }

    "Generate valid path from components" {
      val pathParams = listOf(Parameter(Path("placeholder", false), "placeholder", "String"))
      substituteParams(path, pathParams.filterPaths()) shouldBe "/test/\${placeholder}/"
    }
  }

  "!full path" - {
    val path = "/test/{placeholder}/"

    "Parse raw path string to components " {
    }

  }

  "parts render" - {
    "source annotations" - {
      "path" {
        val annotation = Path(placeholder = "id", encoded = false)
        annotation.toString() shouldBe """@Path(placeholder = "id", encoded = false)"""
      }
    }
  }

  "!method render" - {
    "ff" {
      val path = "/pets/{id}"
      println(path)
      val method = Method(
          name = "getPet",
          httpVerb = VerbAnnotation.get(path),
          headers = emptyList(),
          encoding = null,
          params = listOf(konnekt.Parameter(
              annotation = Path(placeholder = "id", encoded = false),
              name = "param",
              type = "String"
          )),
          returnType = "String"
      )

      method.render() shouldBeIgnoringWhitespaces """
        override suspend fun getPet(@Path(placeholder = "id", encoded = false) param: String): String {
          return client.get("/pets/${'$'}{param}") {
          }
        }
      """.trimIndent()
    }
  }

  "interface render" - {
    val prelude = """
      @Target(AnnotationTarget.CLASS)
      annotation class client

      @Target(AnnotationTarget.FUNCTION)
      annotation class GET(val value: String)

      @Target(AnnotationTarget.VALUE_PARAMETER)
      annotation class Path(val value: String, val encoded: Boolean = false)

      @Target(AnnotationTarget.VALUE_PARAMETER)
      annotation class Query(val value: String, val encoded: Boolean = false)

      @Target(AnnotationTarget.VALUE_PARAMETER)
      annotation class Body

      @Target(AnnotationTarget.VALUE_PARAMETER)
      annotation class QueryMap

      @Target(AnnotationTarget.FUNCTION)
      annotation class DELETE(val value: String)

      @Target(AnnotationTarget.VALUE_PARAMETER)
      annotation class Field(val key: String)

      @Target(AnnotationTarget.VALUE_PARAMETER)
      annotation class FieldMap

      @Target(AnnotationTarget.FUNCTION)
      annotation class FormUrlEncoded

      @Target(AnnotationTarget.FUNCTION)
      annotation class HEAD

      @Target(AnnotationTarget.FUNCTION)
      annotation class Header

      @Target(AnnotationTarget.FUNCTION)
      annotation class HeaderMap

      @Target(AnnotationTarget.FUNCTION)
      annotation class Headers(vararg val values: String)

      @Target(AnnotationTarget.FUNCTION)
      annotation class HTTP

      @Target(AnnotationTarget.FUNCTION)
      annotation class Multipart

      @Target(AnnotationTarget.FUNCTION)
      annotation class OPTIONS

      @Target(AnnotationTarget.VALUE_PARAMETER)
      annotation class Part

      @Target(AnnotationTarget.VALUE_PARAMETER)
      annotation class PartMap

      @Target(AnnotationTarget.FUNCTION)
      annotation class PATCH

      @Target(AnnotationTarget.FUNCTION)
      annotation class POST(val value: String)

      @Target(AnnotationTarget.FUNCTION)
      annotation class PUT

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
        import kotlinx.coroutines.runBlocking
        import java.io.File
        import java.time.LocalDateTime
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
      @client
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

    "!tt" {
      assertThis(CompilerTest(
          config = { listOf(addMetaPlugins(KonnektPlugin()), ktorDependencies) },
          code = { declaration.source },
          assert = { quoteOutputMatches(implementation.source) }
      ))
    }

    "integration test" {
      val objectMapper = jacksonObjectMapper()
      val books = objectMapper.readValue<List<Book>>(resourceContent("books.json"))
      val book = objectMapper.readValue<Book>(resourceContent("book_1.json"))
      val character = objectMapper.readValue<Character>(resourceContent("character_1.json"))
      assertThis(CompilerTest(
          config = { listOf(addMetaPlugins(KonnektPlugin()), ktorDependencies) },
          code = { declaration.source },
          assert = {
            allOf(
                "listBooks()".source.evalsTo(books),
                "bookById()".source.evalsTo(book),
                "characterById()".source.evalsTo(character)
            )
          }
      ))
    }

    "simple test" - {
      val declaration = """
        |//metadebug
        |$imports
        |$prelude
        |
        |@client
        |interface SimpleClient {
        |   @POST("/url")
        |   suspend fun test(): String
        |   
        |   companion object
        |}
        |
        |val klient = HttpClient(MockEngine) {
        |   engine {
        |       addHandler { request ->
        |           assert(request.method == HttpMethod.Post)
        |           respondOk("OK")
        |       }
        |   }
        |}
        |
        |val api = SimpleClient(klient)
        |
        |fun eval(): String = runBlocking { api.test() }
      """.trimMargin()

      "functional POST test" {
        try {
          assertThis(CompilerTest(
              config = { listOf(addMetaPlugins(KonnektPlugin()), ktorDependencies) },
              code = { declaration.source },
              assert = { "eval()".source.evalsTo("OK") }
          ))
        } catch (e: InvocationTargetException) {
          throw e.cause!!
        }
      }
    }

    "companion object required" - {
      val declaration = """
        |//metadebug
        |$imports
        |$prelude
        |
        |@client
        |interface SimpleClient {
        |   @POST("/url")
        |   suspend fun test(): String
        |}
      """.trimMargin()

      "compiler error present" {
        try {
          assertThis(CompilerTest(
              config = { listOf(addMetaPlugins(KonnektPlugin()), ktorDependencies) },
              code = { declaration.source },
              assert = { failsWith { it.contains("SimpleClient".noCompanion)  } }
          ))
        } catch (e: InvocationTargetException) {
          throw e.cause!!
        }
      }
    }
  }
})


val ConfigSyntax.ktorDependencies get() = addDependencies(
    Dependency("ktor-client-core:1.3.0"),
    Dependency("ktor-http:1.3.0"),
    Dependency("ktor-http-jvm:1.3.0"),
    Dependency("ktor-client-core-jvm:1.3.0"),
    Dependency("ktor-client-cio:1.3.0"),
    Dependency("ktor-client-json-jvm:1.3.0"),
    Dependency("ktor-client-logging-jvm:1.3.0"),
    Dependency("ktor-client-jackson:1.3.0"),
    Dependency("ktor-client-mock:1.3.0"),
    Dependency("ktor-client-mock-jvm:1.3.0"),
    Dependency("kotlinx-coroutines-core-jvm:1.3.9"),
    Dependency("jackson-databind:2.9.9.3"),
    Dependency("jackson-module-kotlin:2.9.9")
)



infix fun String.shouldBeIgnoringWhitespaces(other: String) = this.ignoringWhitespaces() shouldBe other.ignoringWhitespaces()

fun String.ignoringWhitespaces() = filterNot { it.isWhitespace() }