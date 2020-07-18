import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.MetaPlugin
import arrow.meta.invoke
import arrow.meta.phases.CompilerContext
import arrow.meta.quotes.Transform
import arrow.meta.quotes.binaryExpression
import arrow.meta.quotes.classDeclaration
import kotlin.contracts.ExperimentalContracts

val Meta.transformReplaceClass: CliPlugin
    get() = "Transform Replace Class" {
        meta(
            classDeclaration(this, { name == "Foo" }) { c ->
                Transform.replace(
                    c,
                    """
                    | class FooModified {
                    |   fun generatedFun() = println("Generated function")
                    | }
                    """.`class`.syntheticScope
                )
            }
        )
    }

open class TransformMetaPlugin : MetaPlugin() {
    @ExperimentalContracts
    override fun intercept(ctx: CompilerContext): List<CliPlugin> =
            listOf(transformReplaceClass)
}