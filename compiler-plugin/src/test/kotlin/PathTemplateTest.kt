import io.kotest.core.spec.style.FreeSpec

class PathTemplateTest : FreeSpec({
  "Path argument of verb annotations" - {

    "accepts variable that has matching argument" {
      interfaceTemplate {
        """
        |@GET("foo/{a}")
        |suspend fun foo(@Path("a") a: String)
        """.trimMargin()
      } expect {
        compiles
      }
    }

    "rejects variable that has no matching argument" {
      interfaceTemplate {
        """
          |@GET("foo/{b}")
          |suspend fun foo(@Path("a") a: String)
        """.trimMargin()
      } expect {
        fails
      }
    }

    "accepts template without variables" {
      interfaceTemplate {
        """
          |@GET("foo")
          |suspend fun foo()
        """.trimMargin()
      } expect {
        compiles
      }
    }

    "rejects template if not all arguments expanded" {
      interfaceTemplate {
        """
          |@GET("foo/{a}}")
          |suspend fun foo(@Path("a") a: String, @Path("b") b: String)
        """.trimMargin()
      } expect {
        fails
      }
    }

  }
})