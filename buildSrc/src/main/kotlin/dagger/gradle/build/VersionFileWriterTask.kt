/*
 * Copyright (C) 2025 The Dagger Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dagger.gradle.build

import com.android.build.api.variant.LibraryAndroidComponentsExtension
import dagger.gradle.build.VersionFileWriterTask.Companion.TASK_NAME
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/** Task that write a version to a given output file. */
@DisableCachingByDefault(because = "Doesn't benefit from caching")
abstract class VersionFileWriterTask : DefaultTask() {
  @get:Input abstract val version: Property<String>
  @get:Input abstract val relativePath: Property<String>
  @get:OutputDirectory abstract val outputDir: DirectoryProperty

  @TaskAction
  fun run() {
    val outputFile = File(outputDir.get().asFile, relativePath.get())
    outputFile.parentFile.mkdirs()
    println(version.get())
    outputFile.printWriter().use { it.println(version.get()) }
  }

  internal companion object {
    const val TASK_NAME = "writeVersionFile"
  }
}

fun Project.configureVersionFileWriter(
  libraryAndroidComponentsExtension: LibraryAndroidComponentsExtension,
  daggerExtension: DaggerBuildExtension,
) {
  if (tasks.findByName(TASK_NAME) != null) {
    return
  }
  val writeVersionFile = tasks.register<VersionFileWriterTask>(TASK_NAME)

  afterEvaluate { configureVersionFile(writeVersionFile, daggerExtension) }
  libraryAndroidComponentsExtension.onVariants {
    it.sources.resources!!.addGeneratedSourceDirectory(
      writeVersionFile,
      VersionFileWriterTask::outputDir,
    )
  }
}

fun Project.configureVersionFileWriter(
  kotlinProjectExtension: KotlinProjectExtension,
  daggerExtension: DaggerBuildExtension,
) {
  if (tasks.findByName(TASK_NAME) != null) {
    return
  }
  val writeVersionFile = tasks.register<VersionFileWriterTask>(TASK_NAME)
  writeVersionFile.configure {
    this.outputDir.set(layout.buildDirectory.dir("generatedVersionFile"))
  }
  val sourceSet = kotlinProjectExtension.sourceSets.getByName("main")
  val resources = sourceSet.resources
  val includes = resources.includes
  resources.srcDir(writeVersionFile.map { it.outputDir })
  if (includes.isNotEmpty()) {
    includes.add("META-INF/*.version")
    resources.setIncludes(includes)
  }
  afterEvaluate { configureVersionFile(writeVersionFile, daggerExtension) }
}

private fun configureVersionFile(
  writeVersionFile: TaskProvider<VersionFileWriterTask>,
  daggerExtension: DaggerBuildExtension,
) {
  writeVersionFile.configure {
    val group = project.group as String
    val artifactId = project.name
    val version =
      if (daggerExtension.isPublished) {
        project.version.toString()
      } else {
        "0.0.0"
      }

    this.version.set(version)
    this.relativePath.set(String.format("META-INF/%s_%s.version", group, artifactId))
    this.enabled = daggerExtension.isPublished
  }
}
