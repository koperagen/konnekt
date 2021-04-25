package konnekt.dependencies

import konnekt.prelude.Client

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

val String.severalBodyParameters
  get() = "Method $this should have only 0 or 1 @Body parameter"

val String.requiredEncoding: (MimeEncodingsDeclaration) -> String
  get() = { encoding ->
    "Method $this should be annotated with ${encoding.simpleEntry} " +
      "to declare ${encoding.components.joinToString { it.simpleEntry }} parameters"
  }