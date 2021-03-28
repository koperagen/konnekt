import arrow.meta.plugin.testing.Assert
import arrow.meta.plugin.testing.CompilerTest
import arrow.meta.plugin.testing.assertThis
import io.kotest.core.factory.TestFactory
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import konnekt.*
import java.lang.reflect.InvocationTargetException

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

    "Conflicting annotations" - {

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

infix fun String.shouldBeIgnoringWhitespaces(other: String) = this.ignoringWhitespaces() shouldBe other.ignoringWhitespaces()

fun String.ignoringWhitespaces() = filterNot { it.isWhitespace() }