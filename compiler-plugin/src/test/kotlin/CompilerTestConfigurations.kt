import arrow.meta.plugin.testing.CompilerTest
import arrow.meta.plugin.testing.Config
import arrow.meta.plugin.testing.ConfigSyntax
import arrow.meta.plugin.testing.Dependency
import konnekt.KonnektPlugin

val CompilerTest.Companion.konnektConfig: List<Config>
  get() = listOf(addMetaPlugins(KonnektPlugin()), ktorDependencies)

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