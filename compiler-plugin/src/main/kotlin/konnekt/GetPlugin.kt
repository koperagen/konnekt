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
  return extractData(ctx)?.render()
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

fun Method.render(): String {
  fun List<Parameter>.render() = joinToString { "${it.name}: ${it.type}" }

  return """
    override suspend fun $name(${params.render()}): $returnType {
        return client.${httpVerb.verb.toLowerCase()}(path = "${substituteParams(httpVerb.path, params.filterPaths())}") {
            ${params.filterQueries().joinToString("\n") {
    it.render()
  }
  }
        }
    }
    """.trimIndent()
}

fun ElementScope.render(method: Method): NamedFunction = method.render().function.apply {
  addModifier(owner = value, modifier = KtTokens.OVERRIDE_KEYWORD)
}

fun List<Parameter>.filterQueries(): List<QueryParameter> = mapNotNull { parameter ->
  (parameter.annotation as? Query)?.let { annotation -> TypedParameter(annotation, parameter.name, parameter.type) }
}

fun List<Parameter>.filterPaths(): List<PathParameter> = mapNotNull { parameter ->
  parameter.annotation.safeAs<Path>()?.let { annotation -> TypedParameter(annotation, parameter.name, parameter.type) }
}

fun TypedParameter<Query>.render() = """parameter("${annotation.value}", $name)"""

fun substituteParams(path: String, pathParams: List<PathParameter>): String {
  return pathParams.fold(path) { path, param ->
    path.replace("{${param.annotation.placeholder}}", "\${${param.name}}")
  }
}
