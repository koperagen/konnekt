package konnekt

import arrow.meta.phases.CompilerContext
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun <T> CompilerContext.withArgumentResolvingContext(
    annotation: KtAnnotationEntry,
    expectedNames: Set<String>,
    op: ArgumentResolvingContext.() -> T
): T? {
    val args: List<ValueArgument> = annotation.valueArguments
    val context = ArgumentResolvingContext(args, this)
    if (context.namedArgs.keys.any { it !in expectedNames }) {
      return parsingError("Argument names of ${annotation.referencedName} should be [${expectedNames.joinToString()}], but were [${context.namedArgs}].")
    }

    return context.run(op)
}

class ArgumentResolvingContext internal constructor(val args: List<ValueArgument>, val context: CompilerContext)  {
    val namedArgs: Map<String, ValueArgument> = args
        .mapNotNull { arg -> arg.getArgumentName()?.asName?.asString()?.let { name -> name to arg } }
        .toMap()
}

fun <T> ArgumentResolvingContext.get(name: String, position: Int, defaultValue: Boolean = false, converter: CompilerContext.(ValueArgument) -> T?): T? {
    val arg = namedArgs[name] ?: args.getOrNull(position)
    return if (arg == null) {
        return if (!defaultValue) context.parsingError("Named argument $name should be present. Or positional at $position")
        else null
    } else {
        context.converter(arg)
    }
}

internal fun CompilerContext.constantStringOrNull(arg: ValueArgument): String? {
    return arg.getArgumentExpression()?.safeAs<KtStringTemplateExpression>()?.let {
        when (it.entries.size) {
            0 -> ""
            1 -> it.entries.single().safeAs<KtLiteralStringTemplateEntry>()?.text
            else -> parsingError("String argument should be simple literal string without interpolation")
        }
    }
}

internal fun CompilerContext.constantBooleanOrNull(arg: ValueArgument): Boolean? {
  return arg.getArgumentExpression()?.safeAs<KtConstantExpression>()?.let {
   booleanValues[it.text] ?: parsingError("Boolean argument should be simple literal 'true' or 'false'")
  }
}

internal val booleanValues = mapOf("true" to true, "false" to false)