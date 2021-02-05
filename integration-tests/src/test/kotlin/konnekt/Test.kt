package konnekt

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import konnekt.prelude.Client
import konnekt.prelude.GET
import konnekt.prelude.Path
import konnekt.prelude.Query
import kotlinx.coroutines.runBlocking

@Client
interface IceAndFireClient {
  @GET("/books")
  suspend fun listBooks(
      @Query("name") name: String? = null,
  ): String

  @GET("/books/{id}")
  suspend fun bookById(@Path("id") id: Int): String

  @GET("/characters")
  suspend fun listCharacters(): String

  @GET("/characters/{id}")
  suspend fun characterById(@Path("id") id: Int): String

  companion object
}

class Test : AnnotationSpec() {

  companion object {
    val client = HttpClient(MockEngine) {
      engine {
        addHandler {
          when (it.url.fullPath) {
            "/books" -> {
              it.method shouldBe HttpMethod.Get
              respondOk()
            }
            "/books/1" -> {
              respondOk()
            }
            "/characters" -> respondOk()
            "/characters/1" -> respondOk()
            else -> error("")
          }
        }
      }
    }
  }

  @Test
  fun test() = runBlocking {
    IceAndFireClient(client).listBooks() shouldBe ""
  }

}

private val Url.hostWithPortIfRequired: String get() = if (port == protocol.defaultPort) host else hostWithPort
private val Url.fullUrl: String get() = "${protocol.name}://$hostWithPortIfRequired$fullPath"