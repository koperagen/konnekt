import arrow.meta.plugin.testing.Assert
import arrow.meta.plugin.testing.AssertSyntax
import arrow.meta.plugin.testing.CompilerTest
import arrow.meta.plugin.testing.assertThis
import io.kotest.matchers.shouldBe

fun AssertSyntax.failsWithError(error: String) = failsWith { it.contains(error) }

infix fun String.expect(assert: CompilerTest.Companion.() -> Assert) = assertThis(CompilerTest({ konnektConfig }, { this@expect.source }, assert))

infix fun String.shouldBeIgnoringWhitespaces(other: String) = this.ignoringWhitespaces() shouldBe other.ignoringWhitespaces()

fun String.ignoringWhitespaces() = filterNot { it.isWhitespace() }