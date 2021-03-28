import arrow.meta.plugin.testing.AssertSyntax
import io.kotest.core.spec.style.DescribeSpec
import konnekt.MimeEncodingsDeclaration
import konnekt.noCompanion
import konnekt.noVerb
import konnekt.notSuspended
import konnekt.requiredEncoding
import konnekt.severalBodyParameters
import konnekt.superTypesNotAllowed

class ClientInterfaceRequirementsTests : DescribeSpec({

  describe("@Client interface") {
    it("must have companion object", expectOn = { failsWith { it.contains("SimpleClient".noCompanion)  } }) {
      """
        |//metadebug
        |$imports
        |$prelude
        |
        |@Client
        |interface SimpleClient {
        |   @POST("/url")
        |   suspend fun test(): String
        |}
        |""".trimMargin()
    }

    it("must have no supertypes", expectOn = { failsWith { it.contains("Test".superTypesNotAllowed) } }) {
      """
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
    }

    context("method") {
      it("must have suspend modifier", expectOn = { failsWith { it.contains("Test".notSuspended) } }) {
        """
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
        |""".trimMargin()
      }

      it("must have verb annotation", expectOn = { failsWith { it.contains("Test".noVerb) } }) {
        """
          |//metadebug
          |import konnekt.prelude.*
          |
          |@Client
          |interface Test {
          |   suspend fun foo()
          |   
          |   companion object
          |}
        """.trimMargin()
      }

      it("must have only 0 or 1 mime encoding annotation", expectOn = {
        failsWith {
          it.contains("should be annotated with one") &&
              it.contains(MimeEncodingsDeclaration.MULTIPART.declaration.simpleName) &&
              it.contains(MimeEncodingsDeclaration.FORM_URL_ENCODED.declaration.simpleName)
        }
      }) {
        """
          |//metadebug
          |import konnekt.prelude.*
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
      }

      it("must not have both @Body and @Multipart", expectOn = {
        failsWith { it.contains("Method test cannot have both @Multipart encoding and @Body parameter") }
      }) {
        """
          |//metadebug
          |import konnekt.prelude.*
          |
          |@Client
          |interface Test {
          |   @Multipart
          |   @GET("/test")
          |   suspend fun test(@Body foo: String): String
          |   
          |   companion object
          |}
        """.trimMargin()
      }

      it("must not have both @Body and @FormUrlEncoded", expectOn = {
        failsWith { it.contains("Method test cannot have both @FormUrlEncoded encoding and @Body parameter") }
      }) {
        """
          |//metadebug
          |import konnekt.prelude.*
          |
          |@Client
          |interface Test {
          |   @FormUrlEncoded
          |   @GET("/test")
          |   suspend fun test(@Body foo: String): String
          |   
          |   companion object
          |}
        """.trimMargin()
      }

      it("must have only 0 or 1 @Body parameter", expectOn = { failsWithError("test".severalBodyParameters) }) {
        """
          //metadebug
          import konnekt.prelude.*
          @Client
          interface Test {
            @GET("/test")
            suspend fun test(@Body foo: String, @Body bar: String): String
            
            companion object
          }
        """.trimIndent()
      }

      it("must have @Multipart annotation to declare @Part parameters", expectOn = {
        failsWithError("test".requiredEncoding(MimeEncodingsDeclaration.MULTIPART))
      }) {
        """
          //metadebug
          import konnekt.prelude.*
          @Client
          interface Test {
            @GET("/test")
            suspend fun test(@Part("foo") foo: String): String
          
            companion object
          }
          
        """.trimIndent()
      }

      it("must have @FormUrlEncoded annotation to declare @Field parameters", expectOn = {
        failsWithError("test".requiredEncoding(MimeEncodingsDeclaration.FORM_URL_ENCODED))
      }) {
        """
          //metadebug
          import konnekt.prelude.*
          @Client
          interface Test {
            @GET("/test")
            suspend fun test(@Field("foo") foo: String): String
          
            companion object
          }
        """.trimIndent()
      }
    }

  }
})

fun AssertSyntax.failsWithError(error: String) = failsWith { it.contains(error) }