import arrow.meta.plugin.testing.CompilerTest
import arrow.meta.plugin.testing.CompilerTest.Companion.source
import arrow.meta.plugin.testing.Dependency
import arrow.meta.plugin.testing.assertThis
import org.junit.Test

class BinaryExpressionTest {

    companion object {
        val binaryExpression = """
                         | //metadebug
                         | 
                         | class Wrapper {
                         |   init {
                         |     println(2 == 3)
                         |   }
                         | }
                         | """.source
    }

    @Test
    fun `check if extra function is generated`() {
        assertThis(CompilerTest(
            config = { addDependencies(Dependency("arrow-meta-prelude:1.3.61-SNAPSHOT")) + addMetaPlugins(TransformMetaPlugin()) },
            code = {
                """
                | //metadebug
                |
                | class Foo() {}
                """.source
            },
            assert = {
                quoteOutputMatches(
                    """
                    | @arrow.synthetic class FooModified {
                    |   fun generatedFun() = println("Generated function")
                    | }
                    """.source
                )
            }
        ))
    }
}