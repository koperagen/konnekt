import arrow.meta.plugin.testing.Assert
import arrow.meta.plugin.testing.CompilerTest
import arrow.meta.plugin.testing.assertThis
import io.kotest.core.spec.style.FunSpec


class ImportConflictResolutionTest : FunSpec() {

  private object Code {
    val sourceUsesExternalName = """
      |package test 
      |
      |import konnekt.prelude.*
      |import java.util.Date
      |
      |@Client
      |interface Foo {
      |   
      |   @GET("/foo")
      |   suspend fun bar(): Date
      |   
      |   companion object
      |}
    """.trimMargin()

    val sourceDefinesClassName = """
      |package test
      |
      |import konnekt.prelude.*
      |
      |data class UserDefined(val a: String)
      |
      |@Client
      |interface Foo {
      |
      |   @GET("/foo")
      |   suspend fun bar(): UserDefined
      |   
      |   companion object
      |}
    """.trimMargin()

    val sourceDefinesNestedClass = """
      |package test
      |
      |import konnekt.prelude.*
      |
      |@Client
      |interface Foo {
      |
      |   @GET("/foo")
      |   suspend fun bar(): Nested
      |
      |   data class Nested(val value: String)
      |   
      |   companion object 
      |}
      |
    """.trimMargin()

    val sourceImportsConflictingName = """
      |package test 
      |
      |import konnekt.prelude.*
      |import java.net.http.HttpClient
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

  private fun FunSpec.importTest(name: String, code: String, assert: CompilerTest.Companion.() -> Assert = { compiles }) {
    test(name) {
      assertThis(CompilerTest(
        config = { konnektConfig },
        code = { code.source },
        assert = assert
      ))
    }
  }

  init {
    importTest("Code.sourceDefinesClassName", Code.sourceDefinesClassName)
    importTest("Code.sourceDefinesNestedClass", Code.sourceDefinesNestedClass)
    importTest("Code.sourceUsesExternalName", Code.sourceUsesExternalName)
    importTest("Code.sourceImportsConflictingName", Code.sourceImportsConflictingName) { fails }
  }
}