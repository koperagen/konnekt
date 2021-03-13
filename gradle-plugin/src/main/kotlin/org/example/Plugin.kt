package org.example

import org.gradle.api.Plugin
import org.gradle.api.Project


class MyPlugin : Plugin<Project> {

  override fun apply(p: Project) {
    p.afterEvaluate {
      println("Hello, world!")
    }
  }

}