package konnekt

import arrow.meta.phases.CompilerContext
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

@Deprecated("Use constantStringOrNull() instead", level = DeprecationLevel.WARNING)
internal fun List<KtValueArgument>.getStringOrNull(position: Int) =
    getOrNull(position)?.text?.removeSurrounding("\"")

@Deprecated("Use constantBooleanOrNull() instead", level = DeprecationLevel.WARNING)
internal fun List<KtValueArgument>.getBooleanOrNull(position: Int) =
    getOrNull(position)?.text?.toBooleanOrNull()

internal val booleanValues = mapOf("true" to true, "false" to false)

internal fun String.toBooleanOrNull(): Boolean? {
  return booleanValues[this]
}

internal fun CompilerContext.constantStringOrNull(arg: KtValueArgument): String? {
    return arg.getArgumentExpression()?.safeAs<KtStringTemplateExpression>()?.let {
        when (it.entries.size) {
            0 -> ""
            1 -> it.entries.single().safeAs<KtLiteralStringTemplateEntry>()?.text
            else -> parsingError("URL pattern argument should be simple literal string without interpolation")
        }
    }
}

internal fun CompilerContext.constantBooleanOrNull(arg: KtValueArgument): Boolean? = TODO()