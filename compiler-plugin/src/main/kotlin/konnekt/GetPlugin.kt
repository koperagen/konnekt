package konnekt

import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.invoke
import arrow.meta.phases.CompilerContext
import arrow.meta.quotes.ScopedList
import arrow.meta.quotes.Transform
import arrow.meta.quotes.classDeclaration
import arrow.meta.quotes.nameddeclaration.stub.typeparameterlistowner.NamedFunction
import konnekt.prelude.Client
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.toLogger
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtUserType
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

fun KtNamedFunction.extractData(ctx: CompilerContext): Method? {
  val name = nameAsSafeName.identifier
  val verb = verbs(ctx) ?: return null

  val headers = headers(ctx)

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

  val parameters = parameters(ctx) ?: return null
  val returnType = typeReference?.text ?: TODO("Handle return type absence")
  return Method(name, verb, headers, encoding, parameters, returnType)
}

fun <T> CompilerContext.parsingError(message: String, element: KtAnnotated? = null): T? {
  ctx.messageCollector?.report(CompilerMessageSeverity.ERROR, message, null)
  return null
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

fun substituteParams(path: String, pathParams: List<PathParameter>): String {
  return pathParams.fold(path) { path, param ->
    path.replace("{${param.annotation.placeholder}}", "\${${param.name}}")
  }
}

fun Any?.TODO(cause: String): Nothing = throw NotImplementedError(cause)

val String.noCompanion
  get() = "${Client::class.java.simpleName} annotated interface $this needs to declare companion object."

val String.notSuspended
  get() = "Function in ${Client::class.java.simpleName} interface should have suspend modifier"

internal fun CompilerContext.knownError(message: String, element: KtAnnotated? = null): Unit =
    ctx.messageCollector?.report(CompilerMessageSeverity.ERROR, message, null) ?: Unit