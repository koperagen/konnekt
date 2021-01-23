package konnekt

import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.invoke
import arrow.meta.phases.CompilerContext
import arrow.meta.quotes.ScopedList
import arrow.meta.quotes.Transform
import arrow.meta.quotes.classDeclaration
import arrow.meta.quotes.nameddeclaration.stub.typeparameterlistowner.NamedFunction
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.toLogger
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class KonnektPlugin : Meta {
  override fun intercept(ctx: CompilerContext): List<CliPlugin> =
      listOf(konnektPlugin)
}

val Meta.konnektPlugin: CliPlugin
  get() = "GET Plugin" {
    meta(
      classDeclaration(ctx, ::isKonnektClient) { c ->
        if (c.companionObjects.isEmpty()) {
          knownError(c.nameAsSafeName.asString().noCompanion, c)
          return@classDeclaration Transform.empty
        }

        val implementation = body.functions.value
            .mapNotNull { it.generateDefinition(ctx, NamedFunction(it)) }
            .takeIf { it.size == body.functions.value.size }
            ?.joinToString("\n")
            ?: return@classDeclaration Transform.empty

        val imports = ScopedList(c.containingKtFile.importDirectives, separator = "\n")

        // How to debug transformations?
        ctx.messageCollector?.toLogger()?.log(implementation)
        Transform.newSources(
            """|package ${c.containingKtFile.packageFqName}
               |
               |$ktorImports
               |$imports
               |
               |operator fun $name.Companion.invoke(client: HttpClient): $name {
               |  return object : $name {
               |    $implementation
               |  }
               |}
               |""".trimMargin().file("$name\$Implementation")
        )
      }
    )
  }

private val ktorImports: String = """
  |import io.ktor.client.*
  |import io.ktor.client.request.*
  |""".trimMargin()

fun KtNamedFunction.generateDefinition(ctx: CompilerContext, func: NamedFunction): String? {
  return extractData(ctx)?.render()
}

val headersAnnotations = setOf("Headers")

val multipartAnnotation = "Multipart"

val formUrlEncodedAnnotation = "FormUrlEncoded"

val sourceAnnotations = setOf("Path", "Body", "Query", "Part", "Field")

fun KtNamedFunction.extractData(ctx: CompilerContext): Method? {
  val name = nameAsSafeName.identifier
  val verb = refactor1(ctx) ?: return null

  val headers = annotationEntries
      .mapNotNull {
        val name = it.typeReference?.typeElement?.safeAs<KtUserType>()?.referencedName
        if (name != null && name in headersAnnotations) {
          toHeaderAnnotation(it)
        } else {
          null
        }
      }

  val encoding = annotationEntries
      .mapNotNull {
        val name = it.typeReference?.typeElement?.safeAs<KtUserType>()?.referencedName
        when (name) {
          multipartAnnotation -> MimeEncoding.MULTIPART
          formUrlEncodedAnnotation -> MimeEncoding.FORM_URL_ENCODED
          else -> null
        }
      }.let {
        when (it.size) {
          0 -> null
          1 -> it[0]
          else -> error("Function $nameAsSafeName annotation entries $annotationEntries contains multiple mime encoding items. Specify 0 or 1 encoding")
        }
      }

  val parameters = valueParameters
      .map { parameter ->
        val sourceAnnotation = parameter.annotationEntries
            .mapNotNull { annotationEntry ->
              val annotationName = annotationEntry.typeReference?.typeElement?.safeAs<KtUserType>()?.referencedName
              if (annotationName != null && annotationName in sourceAnnotations) {
                toSourceAnnotationOrNull(annotationEntry, annotationName)
              } else {
                null
              }
            }
            .first()
        val name = parameter.nameAsSafeName.identifier
        val type = parameter.typeReference?.text ?: TODO("Handle value parameter type absence")
        Parameter(sourceAnnotation, name, type)
      }
  val returnType = typeReference?.text ?: TODO("Handle return type absence")
  return Method(name, verb, headers, encoding, parameters, returnType)
}

fun <T> CompilerContext.parsingError(message: String, element: KtAnnotated? = null): T? {
  ctx.messageCollector?.report(CompilerMessageSeverity.ERROR, message, null)
  return null
}

private fun KtNamedFunction.refactor1(ctx: CompilerContext): VerbAnnotation? {
  val verbAnnotations = annotationEntries.mapNotNull { verbAnnotation(it) }
  return when (verbAnnotations.size) {
    0 -> ctx.parsingError("Client method should be annotated with some Verb Annotation", this)
    1 -> {
      val scope = verbAnnotations.first()
      ctx.refine(scope)
    }
    else -> ctx.parsingError("Client method should be annotated with exactly 1 Verb Annotation", this)
  }
}

private fun CompilerContext.refine(scope: VerbAnnotationScope): VerbAnnotation? {
  return when (scope.verb) {
    Verb.HTTP -> TODO()
    else -> {
      val argument = singleString(scope)
      TODO()
    }
  }
}

private fun CompilerContext.singleString(scope: VerbAnnotationScope): String? {
  return when (scope.arguments.size) {
    1 -> {
      val arg = scope.arguments.single()
      TODO()
    }
    else -> TODO()/*parsingError("", scope.annotation)*/
  }
}

inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String): T? =
    enumValues<T>().firstOrNull { it.name.equals(name, ignoreCase = true) }

fun toVerbAnnotation(annotationEntry: KtAnnotationEntry, verb: String): VerbAnnotation {
  val path = annotationEntry.valueArgumentList
      ?.arguments
      ?.map { it.text.removeSurrounding("\"") }
      ?.singleOrNull()
      ?: error("Expected exactly one argument")
  println("konnekt.toVerbAnnotation: $path")
  return VerbAnnotation(verb, path)
}

fun toHeaderAnnotation(annotationEntry: KtAnnotationEntry): HeaderAnnotation {
  val headers = annotationEntry.valueArgumentList
      ?.arguments
      ?.map { it.text.removeSurrounding("\"") }
      ?: emptyList()
  return HeaderAnnotation(headers)
}

fun toSourceAnnotationOrNull(annotationEntry: KtAnnotationEntry, annotationName: String): SourceAnnotation? {

  return when (annotationName) {
    "Path" -> path(annotationEntry)
    "Body" -> body(annotationEntry)
    "Query" -> query(annotationEntry)
    "Part" -> part(annotationEntry)
    "Field" -> field(annotationEntry)
    else -> null
  }
}

private fun List<KtValueArgument>.values(): Pair<String, Boolean?> =
    (getStringOrNull(0) ?: error("Expected first string param in argument list")) to
        getBooleanOrNull(1)

private fun path(annotationEntry: KtAnnotationEntry): Path {
    val args = annotationEntry.valueArgumentList?.arguments
        ?: error("Argument list for path should contain 'id' and optoinally 'encoded'")
    val (value, encoded) = args.values()
    return if (encoded != null) {
        Path(value, encoded)
    } else {
        Path(value)
    }
}

private fun body(annotationEntry: KtAnnotationEntry): Body {
    return Body
}

private fun query(annotationEntry: KtAnnotationEntry): Query {
    val args = requireNotNull(annotationEntry.valueArgumentList?.arguments)
    val (key, encoded) = args.values()
    return if (encoded != null) {
        Query(key, encoded)
    } else {
        Query(key)
    }
}

private fun part(annotationEntry: KtAnnotationEntry): Part {
    val args = requireNotNull(annotationEntry.valueArgumentList?.arguments)
    val (key, encoded) = args.values()
    return if (encoded != null) {
        Part(key, encoded)
    } else {
        Part(key)
    }
}

private fun field(annotationEntry: KtAnnotationEntry): Field {
    val args = requireNotNull(annotationEntry.valueArgumentList?.arguments)
    val key = requireNotNull(args.getStringOrNull(0))
    return Field(key)
}

private fun List<KtValueArgument>.getStringOrNull(position: Int) =
    getOrNull(position)?.text?.removeSurrounding("\"")

private fun List<KtValueArgument>.getBooleanOrNull(position: Int) =
    getOrNull(position)?.text?.toBooleanOrNull()

private val booleanValues = mapOf("true" to true, "false" to false)

fun String.toBooleanOrNull(): Boolean? {
  return booleanValues[this]
}

fun isKonnektClient(ktClass: KtClass): Boolean = ktClass.isInterface() && ktClass.hasAnnotation("client", "prelude.client", "konnekt.prelude.client")

fun KtAnnotated.hasAnnotation(
    vararg annotationNames: String
): Boolean {
  val names = annotationNames.toHashSet()
  val predicate: (KtAnnotationEntry) -> Boolean = {
    it.typeReference
        ?.typeElement
        ?.safeAs<KtUserType>()
        ?.referencedName in names
  }
  return annotationEntries.any(predicate)
}

fun substituteParams(path: String, pathParams: List<PathParameter>): String {
  return pathParams.fold(path) { path, param ->
    path.replace("{${param.annotation.placeholder}}", "\${${param.name}}")
  }
}

val String.noCompanion
  get() = "@Client annotated interface $this needs to declare companion object."

internal fun CompilerContext.knownError(message: String, element: KtAnnotated? = null): Unit =
    ctx.messageCollector?.report(CompilerMessageSeverity.ERROR, message, null) ?: Unit