package konnekt

import arrow.meta.phases.CompilerContext
import konnekt.prelude.Client
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.psi.KtAnnotated

fun Any?.TODO(cause: String): Nothing = throw NotImplementedError(cause)

val String.noCompanion
  get() = "${Client::class.java.simpleName} annotated interface $this needs to declare companion object."

val String.notSuspended
  get() = "Methods in @${Client::class.java.simpleName} interface $this should have suspend modifier"

val String.superTypesNotAllowed
  get() = "${Client::class.java.simpleName} annotated interface $this must not have super types."

val String.noVerb
  get() = "Methods in @${Client::class.java.simpleName} interface $this should have verb annotation"

val String.noClientAnnotation
  get() = "interface $this should be annotated with ${ClientDeclaration.fqEntry}"

internal fun CompilerContext.knownError(message: String, element: KtAnnotated? = null): Unit =
    ctx.messageCollector?.report(CompilerMessageSeverity.ERROR, message, null) ?: Unit

fun <T> CompilerContext.parsingError(message: String, element: KtAnnotated? = null): T? {
  ctx.messageCollector?.report(CompilerMessageSeverity.ERROR, message, null)
  return null
}