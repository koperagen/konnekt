import arrow.meta.plugin.testing.CompilerTest
import arrow.meta.plugin.testing.Config
import arrow.meta.plugin.testing.ConfigSyntax
import arrow.meta.plugin.testing.Dependency
import arrow.meta.plugin.testing.assertThis
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.factory.TestFactory
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import konnekt.*
import java.lang.reflect.InvocationTargetException

private val CompilerTest.Companion.konnektConfig: List<Config>
  get() = listOf(addMetaPlugins(KonnektPlugin()), ktorDependencies)

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

  fun annotationTest(source: SourcesDeclaration): TestFactory {
    val functions = functions(source)
    return "Plugin parses @${source.declaration.simpleName}".annotationTest(functions)
  }

  SourcesDeclaration.values().forEach {
    include(annotationTest(it))
  }

  include("Plugin parses @Headers".annotationTest(functions = headerFunctions()))

  fun annotationTest(encoding: MimeEncodingsDeclaration): TestFactory {
    val functions = mimeEncodingFunctions(encoding)
    return "Plugin parses @${encoding.declaration.simpleName}".annotationTest(functions)
  }

  MimeEncodingsDeclaration.values().forEach {
    include(annotationTest(it))
  }

  "!method render" - {
    "ff" {
      val path = "/pets/{id}"
      println(path)
      val method = Method(
          name = "getPet",
          httpVerb = VerbAnnotationModel.get(path),
          headers = HeadersAnnotation(emptyList()),
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

  "Reference expression in string argument cause error for @Headers" {
    val declaration = """
        |//metadebug
        |$imports
        |$prelude
        |
        |const val constString = "foo"
        |
        |@Client
        |interface Test {
        |   @Headers(constString)
        |   @GET("/test")
        |   suspend fun test(): String 
        |   
        |   companion object
        |}
      """.trimMargin()

    assertThis(CompilerTest(
        config = { konnektConfig },
        code = { declaration.source },
        assert = { failsWith { it.contains("Reference expression is not yet supported for konnekt annotations") } }
    ))
  }

  "functional tests" - {

    "companion object required" - {
      val declaration = """
        |//metadebug
        |$imports
        |$prelude
        |
        |@Client
        |interface SimpleClient {
        |   @POST("/url")
        |   suspend fun test(): String
        |}
      """.trimMargin()

      "compiler error present" {
        try {
          assertThis(CompilerTest(
              config = { konnektConfig },
              code = { declaration.source },
              assert = { failsWith { it.contains("SimpleClient".noCompanion)  } }
          ))
        } catch (e: InvocationTargetException) {
          throw e.cause!!
        }
      }
    }

    "Conflicting annotations" - {
      "two mime encodings cause error" {
        val declaration = """
          |//metadebug
          |$imports
          |$prelude
          |
          |@Client
          |interface Test {
          |   @FormUrlEncoded
          |   @Multipart
          |   @GET("/test")
          |   suspend fun test(): String
          |   
          |   companion object
          |}
        """.trimMargin()

        assertThis(CompilerTest(
            config = { konnektConfig },
            code = { declaration.source },
            assert = {
              failsWith {
                it.contains("should be annotated with one") &&
                    it.contains(MimeEncodingsDeclaration.MULTIPART.declaration.simpleName) &&
                    it.contains(MimeEncodingsDeclaration.FORM_URL_ENCODED.declaration.simpleName)
              }
            }
        ))
      }

      "multiple @Headers cause error" {
        val declaration = """
          |//metadebug
          |$imports
          |$prelude
          |
          |@Client
          |interface Test {
          |   @GET("/test")
          |   @Headers("")
          |   @Headers("")
          |   suspend fun test(): String
          |   
          |   companion object
          |}
        """.trimMargin()

        assertThis(CompilerTest(
            config = { konnektConfig },
            code = { declaration.source },
            assert = { failsWith { it.contains("Repeating @Headers annotation is not yet supported") } }
        ))
      }
    }
  }

  "integration tests" - {

    "sources test" {
      assertThis(CompilerTest(
          config = { listOf(addMetaPlugins(KonnektPlugin()), ktorDependencies) },
          code = { code.source },
          assert = {
            allOf(
                "query_test()".source.evalsTo(Unit),
                "body_test()".source.evalsTo(Unit),
                "path_test()".source.evalsTo(Unit),
                "header_test()".source.evalsTo(Unit),
                "field_test()".source.evalsTo(Unit)
            )
          }
      ))
    }

    "!tt" {
      assertThis(CompilerTest(
          config = { listOf(addMetaPlugins(KonnektPlugin()), ktorDependencies) },
          code = { declaration.source },
          assert = { quoteOutputMatches(implementation.source) }
      ))
    }

    "complex return type propagates" {
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
        |@Client
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
  }
})

fun String.annotationTest(functions: Iterable<String>) = stringSpec {
  this@annotationTest {
    val code = """
      |//metadebug
      |$imports
      |@Client
      |interface Test {
      | ${functions.joinToString("\n")}
      | companion object
      |}
    """.trimMargin()

    // Syntactically wrong code doesn't reach transformation phase
    // thus won't be printed
    println(code)

    assertThis(CompilerTest(
        config = { listOf(addMetaPlugins(KonnektPlugin()), ktorDependencies) },
        code = { code.source },
        assert = { compiles }
    ))
  }
}

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
    Dependency("jackson-module-kotlin:2.9.9"),
    Dependency("prelude")
)



infix fun String.shouldBeIgnoringWhitespaces(other: String) = this.ignoringWhitespaces() shouldBe other.ignoringWhitespaces()

fun String.ignoringWhitespaces() = filterNot { it.isWhitespace() }