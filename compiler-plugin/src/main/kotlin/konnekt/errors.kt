package konnekt

import arrow.meta.phases.CompilerContext
import konnekt.prelude.Client
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.psi.KtAnnotated

fun Any?.TODO(cause: String): Nothing = throw NotImplementedError(cause)

val String.noCompanion
  get() = "${Client::class.java.simpleName} annotated interface $this needs to declare companion object."

val String.notSuspended
  get() = "Function in ${Client::class.java.simpleName} interface should have suspend modifier"

internal fun CompilerContext.knownError(message: String, element: KtAnnotated? = null): Unit =
    ctx.messageCollector?.report(CompilerMessageSeverity.ERROR, message, null) ?: Unit

fun <T> CompilerContext.parsingError(message: String, element: KtAnnotated? = null): T? {
  ctx.messageCollector?.report(CompilerMessageSeverity.ERROR, message, null)
  return null
}