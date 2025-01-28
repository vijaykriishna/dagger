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

import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * A task for copying JAR resources files located in the repository structure into a generated
 * resource source set that matches the JAR's resources structure. This is necessary due to the
 * repository's structure not being the standard Gradle source set structure.
 */
@DisableCachingByDefault(because = "Not worth caching")
abstract class ResourceCopyTask : DefaultTask() {

  /** Specifications of resource files to copy and their destination directory within the JAR. */
  @get:Input abstract val resourceSpecs: MapProperty<String, String>

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val inputFiles: ListProperty<RegularFile>

  @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

  @TaskAction
  fun execute() {
    val specMap = resourceSpecs.get()
    inputFiles.get().forEach { resourceFile ->
      val inputFile = resourceFile.asFile
      check(inputFile.exists()) { "Resource file does not exist: $inputFile" }
      check(inputFile.isFile) { "Resource file must be a file not a directory: $inputFile" }
      val jarOutputDir = specMap.getValue(inputFile.path)
      val outputFile = outputDirectory.get().dir(jarOutputDir).file(inputFile.name).asFile
      inputFile.copyTo(outputFile, overwrite = true)
    }
  }

  companion object {
    fun register(
      project: Project,
      name: String,
      buildDir: Provider<Directory>,
      kotlinSourceSet: NamedDomainObjectProvider<KotlinSourceSet>,
      javaSourceSet: NamedDomainObjectProvider<JavaSourceSet>,
    ): TaskProvider<ResourceCopyTask> {
      val task =
        project.tasks.register(name, ResourceCopyTask::class.java) { outputDirectory.set(buildDir) }
      listOf(task.map { it.outputDirectory }).let {
        kotlinSourceSet.configure { resources.setSrcDirs(it) }
        javaSourceSet.configure { resources.setSrcDirs(it) }
      }
      return task
    }
  }
}
