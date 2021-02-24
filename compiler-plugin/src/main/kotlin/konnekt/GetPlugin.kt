package konnekt

import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.invoke
import arrow.meta.phases.CompilerContext
import arrow.meta.phases.analysis.ElementScope
import arrow.meta.quotes.ScopedList
import arrow.meta.quotes.Transform
import arrow.meta.quotes.classDeclaration
import arrow.meta.quotes.nameddeclaration.stub.typeparameterlistowner.NamedFunction
import org.jetbrains.kotlin.cli.common.toLogger
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.addRemoveModifier.addModifier
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

        if (c.body?.functions?.any { !it.hasModifier(KtTokens.SUSPEND_KEYWORD) } == true) {
          knownError(c.nameAsSafeName.asString().notSuspended)
          return@classDeclaration Transform.empty
        }

        if (c.body?.functions?.any { !it.hasVerbAnnotation() } == true) {
          knownError(c.nameAsSafeName.asString().noVerb)
          return@classDeclaration Transform.empty
        }

        if (c.superTypeListEntries.isNotEmpty()) {
          knownError(c.nameAsSafeName.asString().superTypesNotAllowed)
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

fun isKonnektClient(ktClass: KtClass): Boolean = ktClass.isInterface() && ktClass.hasAnnotation(*CLIENT_ANNOTATION_NAMES.toTypedArray())

fun KtAnnotated.hasVerbAnnotation() = hasAnnotation(*VerbsDeclaration.values().flatMap { it.names }.toTypedArray())

fun KtAnnotated.hasAnnotation(
    vararg annotationNames: String
): Boolean {
  val names = annotationNames.toHashSet()
  val predicate: (KtAnnotationEntry) -> Boolean = {
    it.referencedName in names
  }
  return annotationEntries.any(predicate)
}

private val ktorImports: String = """
  |import io.ktor.client.*
  |import io.ktor.client.request.*
  |""".trimMargin()

fun KtNamedFunction.generateDefinition(ctx: CompilerContext, func: NamedFunction): String? {
  val method = extractData(ctx)
  return if (method != null) {
    ctx.verify(method)?.render()
  } else {
    return null
  }
}

fun KtNamedFunction.extractData(ctx: CompilerContext): Method? {
  val name = nameAsSafeName.identifier
  val verb = verbs(ctx) ?: return null

  val headers = headers(ctx)

  val encoding = mimeEncoding(ctx)

  val parameters = parameters(ctx) ?: return null
  val returnType = typeReference?.text ?: TODO("Handle return type absence")
  return Method(name, verb, headers, encoding, parameters, returnType)
}

fun SimpleRequest.render(): String {
  require(headers == null) { "Runtime mapping for headers not yet implemented" }
  val path = substituteParams(httpVerb.path, pathParameters) // move outside
  val `httpVerb` = httpVerb.verb
  val `params` = params.joinToString(",") { "${it.name}: ${it.type}" }
  val `headerParameters` = headerParameters.joinToString("\n") {
    """header("${it.annotation.value}", ${it.name}"""
  }
  val `queryParameters` = queryParameters.joinToString("\n") {
    """parameter("${it.annotation.value}", ${it.name})"""
  }
  val `body` = if (body == null) "" else "body = ${body.name}"

  return """
    |override suspend fun $name($`params`): $returnType {
    |   return client.$`httpVerb`(path = "$path") {
    |       $`headerParameters`
    |       $`queryParameters`
    |       $`body`
    |   }
    |}
  """.trimMargin()
}

operator fun Any?.unaryPlus(): String = this?.toString() ?: ""

fun substituteParams(path: String, pathParams: List<PathParameter>): String {
  return pathParams.fold(path) { path, param ->
    path.replace("{${param.annotation.placeholder}}", "\${${param.name}}")
  }
}
