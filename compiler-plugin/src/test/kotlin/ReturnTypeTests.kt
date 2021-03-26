import arrow.meta.plugin.testing.Assert
import arrow.meta.plugin.testing.CompilerTest
import arrow.meta.plugin.testing.assertThis
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.DescribeSpec

class ReturnTypeTests : DescribeSpec({

  describe("Return type logic") {
    context("user defined types parsed from json") {
      val objectMapper = jacksonObjectMapper()
      val books = objectMapper.readValue<List<Book>>(resourceContent("books.json"))
      val book = objectMapper.readValue<Book>(resourceContent("book_1.json"))
      val character = objectMapper.readValue<Character>(resourceContent("character_1.json"))

      it("propagates return type to runtime library", expectOn = {
        allOf(
            "listBooks()".source.evalsTo(books),
            "bookById()".source.evalsTo(book),
            "characterById()".source.evalsTo(character)
        )
      }) {
        """
          //metadebug
          $imports
          $prelude
          ${classesStub()}
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
          ${mockedClient()}
          val api = IceAndFireClient(klient)
          
          fun listBooks() = runBlocking { api.listBooks() }
          fun bookById() = runBlocking { api.bookById(1) }
          fun characterById() = runBlocking { api.characterById(1) }
        """.trimIndent()
      }
    }

    it("accepts absence of return type", expectOn = { compiles })  {
      """
      |package test 
      |
      |import konnekt.prelude.*
      |
      |@Client
      |interface Foo {
      |   
      |   @GET("/foo")
      |   suspend fun bar()
      |   
      |   companion object
      |}
      """.trimMargin()
    }
  }
})

private fun mockedClient() = """
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
""".trimIndent()

private fun classesStub() = """
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

fun Any.resourceContent(file: String): String {
  return javaClass.classLoader.getResourceAsStream(file)?.reader()?.readText() ?: error("Resourse $file not found")
}

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