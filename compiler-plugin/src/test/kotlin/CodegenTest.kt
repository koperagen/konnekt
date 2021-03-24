import arrow.meta.plugin.testing.Assert
import arrow.meta.plugin.testing.CompilerTest
import arrow.meta.plugin.testing.Config
import arrow.meta.plugin.testing.ConfigSyntax
import arrow.meta.plugin.testing.Dependency
import arrow.meta.plugin.testing.assertThis
import io.kotest.core.factory.TestFactory
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import konnekt.*
import java.lang.reflect.InvocationTargetException

val CompilerTest.Companion.konnektConfig: List<Config>
  get() = listOf(addMetaPlugins(KonnektPlugin()), ktorDependencies)

@Suppress("unused")
class CodegenTest : FreeSpec({

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

    "supertypes not allowed" - {
      val declaration = """
        |//metadebug
        |$imports
        |$prelude
        |
        |interface SomeInterface
        |
        |@Client
        |interface Test : SomeInterface {
        |   companion object
        |}
        |""".trimMargin()

      "compiler error present" {
        try {
          assertThis(CompilerTest(
              config = { konnektConfig },
              code = { declaration.source },
              assert = { failsWith { it.contains("Test".superTypesNotAllowed) } }
          ))
        } catch (e: InvocationTargetException) {
          throw e.cause!!
        }
      }
    }

    "methods must have suspend modifier" - {
      val declaration = """
        |//metadebug
        |$imports
        |$prelude
        |
        |@Client
        |interface Test {
        |   fun foo()
        |   
        |   companion object
        |}
      """.trimMargin()

      "compiler error present" {
        try {
          assertThis(CompilerTest(
              config = { konnektConfig },
              code = { declaration.source },
              assert = { failsWith { it.contains("Test".notSuspended) } }
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

infix fun String.expect(assert: CompilerTest.Companion.() -> Assert) = assertThis(CompilerTest({ konnektConfig }, { this@expect.source }, assert))

fun interfaceTemplate(fn: () -> String): String {
  return """
    |//metadebug
    |$imports
    |$prelude
    |
    |@Client
    |interface Test {
    |
    |   ${fn()}
    |   
    |   companion object
    |}
  """.trimMargin()
}

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

val ConfigSyntax.ktorDependencies get() = addDependencies(
    Dependency("ktor-client-core"),
    Dependency("ktor-http"),
    Dependency("ktor-http-jvm"),
    Dependency("ktor-client-core-jvm"),
    Dependency("ktor-client-cio"),
    Dependency("ktor-client-json-jvm"),
    Dependency("ktor-client-logging-jvm"),
    Dependency("ktor-client-jackson"),
    Dependency("ktor-client-mock"),
    Dependency("ktor-client-mock-jvm"),
    Dependency("kotlinx-coroutines-core-jvm"),
    Dependency("jackson-databind"),
    Dependency("jackson-module-kotlin"),
    Dependency("prelude")
)

infix fun String.shouldBeIgnoringWhitespaces(other: String) = this.ignoringWhitespaces() shouldBe other.ignoringWhitespaces()

fun String.ignoringWhitespaces() = filterNot { it.isWhitespace() }