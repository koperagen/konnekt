import arrow.meta.plugin.testing.Assert
import arrow.meta.plugin.testing.CompilerTest
import arrow.meta.plugin.testing.Config
import arrow.meta.plugin.testing.assertThis
import io.kotest.core.spec.style.scopes.DescribeScope
import java.lang.reflect.InvocationTargetException

interface WithCompilerTestConfig {
  val config: CompilerTest.Companion.() -> List<Config>

  suspend fun DescribeScope.it(name: String, expectOn: CompilerTest.Companion.() -> Assert, code: () -> String) =
      it(name, expectOn, config, code)
}

suspend fun DescribeScope.it(name: String, expectOn: CompilerTest.Companion.() -> Assert, config: CompilerTest.Companion.() -> List<Config>, code: () -> String) {
  it(name) {
    try {
      assertThis(CompilerTest(config, { code().source }, expectOn))
    } catch (e: InvocationTargetException) {
      // Message and stacktrace of exception could be too obscure
      throw e.cause ?: throw e
    }
  }
}

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