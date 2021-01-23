package konnekt

import org.jetbrains.kotlin.psi.KtValueArgument

internal fun List<KtValueArgument>.getStringOrNull(position: Int) =
    getOrNull(position)?.text?.removeSurrounding("\"")

internal fun List<KtValueArgument>.getBooleanOrNull(position: Int) =
    getOrNull(position)?.text?.toBooleanOrNull()

internal val booleanValues = mapOf("true" to true, "false" to false)

internal fun String.toBooleanOrNull(): Boolean? {
  return booleanValues[this]
}