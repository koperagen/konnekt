package konnekt.gradle

import io.github.classgraph.ClassGraph
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.util.*

class KonnektGradlePlugin : Plugin<Project> {

  companion object {
    fun isEnabled(project: Project): Boolean = project.plugins.findPlugin(KonnektGradlePlugin::class.java) != null
  }

  override fun apply(project: Project): Unit {
    val properties = Properties()
    properties.load(this.javaClass.getResourceAsStream("plugin.properties"))
    val compilerPluginVersion = properties.getProperty("COMPILER_PLUGIN_VERSION")
    val kotlinVersion = properties.getProperty("KOTLIN_VERSION")
    if (kotlinVersion != project.getKotlinPluginVersion())
       throw InvalidUserDataException("Use Kotlin $kotlinVersion for Konnekt Gradle Plugin")
    project.afterEvaluate { p ->
      // Dependencies that aren't provided by compiler-plugin
      p.dependencies.add("kotlinCompilerClasspath", "org.jetbrains.kotlin:kotlin-script-util:$kotlinVersion")
      p.dependencies.add("kotlinCompilerClasspath", "org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
      p.dependencies.add("kotlinCompilerClasspath", "org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:$kotlinVersion")

      p.tasks.withType(KotlinCompile::class.java).configureEach {
        it.kotlinOptions.freeCompilerArgs += listOf(
          "-Xplugin=${classpathOf("konnekt-plugin:$compilerPluginVersion")}"
//          , "-P"
//          , "plugin:arrow.meta.plugin.compiler:generatedSrcOutputDir=${p.buildDir.absolutePath}"
        )
      }
    }
  }

  private fun classpathOf(dependency: String): File {
    val regex = Regex(".*${dependency.replace(':', '-')}.*")
    return ClassGraph().classpathFiles.first { classpath -> classpath.name.matches(regex) }
  }
}

fun Project.getKotlinPluginVersion(): String? =
    plugins.asSequence().mapNotNull { (it as? KotlinBasePluginWrapper)?.kotlinPluginVersion }.firstOrNull()
