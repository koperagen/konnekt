package konnekt.dependencies

import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtClass

fun isKonnektClient(ktClass: KtClass): Boolean = ktClass.isInterface() && ktClass.hasAnnotation(*CLIENT_ANNOTATION_NAMES.toTypedArray())

fun KtAnnotated.hasVerbAnnotation() = hasAnnotation(*VerbsDeclaration.values().flatMap { it.names }.toTypedArray())