import arrow.meta.ide.IdeMetaPlugin
import arrow.meta.ide.IdePlugin
import arrow.meta.ide.phases.IdeContext
import arrow.meta.ide.plugins.initial.initialIdeSetUp
import arrow.meta.ide.plugins.quotes.quotes
import kotlin.contracts.ExperimentalContracts


class SampleIdeaPlugin : IdeMetaPlugin() {
    @ExperimentalContracts
    override fun intercept(ctx: IdeContext): List<IdePlugin> =
            listOf(initialIdeSetUp, quotes)

//    @ExperimentalContracts
//    override fun intercept(ctx: CompilerContext): List<CliPlugin> =
//            listOf(transformReplaceClass)
}