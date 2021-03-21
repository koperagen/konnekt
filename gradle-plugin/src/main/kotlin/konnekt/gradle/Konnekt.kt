package konnekt.gradle

import org.gradle.api.artifacts.dsl.DependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import java.util.*

object Konnekt {
  val ktorVersion by lazy {
    val properties = Properties()
    properties.load(this.javaClass.getResourceAsStream("plugin.properties"))
    properties.getProperty("KTOR_VERSION")
  }
}

fun KotlinDependencyHandler.konnekt(groupWithArtifact: String): String {
  return konnektDependency(groupWithArtifact)
}

fun DependencyHandler.konnekt(groupWithArtifact: String): String {
  return konnektDependency(groupWithArtifact)
}

private fun konnektDependency(groupWithArtifact: String): String {
  return "$groupWithArtifact:${Konnekt.ktorVersion}"
}