package konnekt

fun <T> computeIf(vararg preconditions: () -> Boolean, factory: () -> T) = if (preconditions.all { it() }) factory() else null

fun Any?.TODO(cause: String): Nothing = throw NotImplementedError(cause)