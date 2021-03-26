import arrow.meta.plugin.testing.Assert
import arrow.meta.plugin.testing.CompilerTest
import arrow.meta.plugin.testing.assertThis
import io.kotest.core.spec.style.scopes.DescribeScope
import java.lang.reflect.InvocationTargetException

suspend fun DescribeScope.it(name: String, expectOn: CompilerTest.Companion.() -> Assert, code: () -> String) {
  it(name) {
    try {
      assertThis(CompilerTest({ konnektConfig }, { code().source }, expectOn))
    } catch (e: InvocationTargetException) {
      // Message and stacktrace of exception could be too obscure
      throw e.cause ?: throw e
    }
  }
}

suspend fun DescribeScope.xit(name: String, expectOn: CompilerTest.Companion.() -> Assert, code: () -> String) {
  xit(name) {}
}