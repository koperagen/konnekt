package konnekt

import io.kotest.core.TestConfiguration
import io.kotest.core.test.TestStatus
import java.io.File
import java.nio.file.Files

fun TestConfiguration.tempdir(prefix: String? = null, suffix: String? = ".tmp"): File {
  val file = Files.createTempDirectory(prefix).toFile()
  afterTest { (_, result) ->
    if (result.status == TestStatus.Success) {
      file.walkBottomUp().filter { it != file }.fold(true) { res, it -> (it.delete() || !it.exists()) && res }
    }
  }
  afterSpec {
    file.deleteRecursively()
  }
  return file
}

val TestConfiguration.newFile: File.(String) -> File
  get() = {
  val file = File(this, it)
  beforeTest {
    file.createNewFile()
  }
  file
}