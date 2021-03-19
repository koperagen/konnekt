package konnekt.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project


class MyPlugin : Plugin<Project> {

  override fun apply(p: Project) {
    p.tasks.create("Hello") {
      it.doLast {
        println("Hello, world!")
      }
    }
  }

}