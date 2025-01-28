/*
 * Copyright (C) 2024 The Dagger Authors.
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

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

private typealias JavaSourceSet = org.gradle.api.tasks.SourceSet

@DslMarker annotation class DaggerGradleDsl

@DaggerGradleDsl
class DaggerSourceSet(
  private val project: Project,
  private val kotlinSourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
  private val javaSourceSets: NamedDomainObjectContainer<JavaSourceSet>,
) {
  private val resourceCopyTask: TaskProvider<ResourceCopyTask> =
    project.tasks.register("copyResources", ResourceCopyTask::class.java) {
      outputDirectory.set(project.layout.buildDirectory.dir("generated/resources"))
    }

  init {
    listOf(resourceCopyTask.map { it.outputDirectory }).let {
      kotlinSourceSets.named("main").configure { resources.setSrcDirs(it) }
      javaSourceSets.named("main").configure { resources.setSrcDirs(it) }
    }
  }

  /** The main source set */
  val main: SourceSet =
    object : SourceSet {
      override fun setPackages(packages: List<String>) {
        val packagePaths = packages.map { Path(it) }
        kotlinSourceSets.named("main").configure {
          kotlin.includePackages(project.rootProject.layout.projectDirectory, packagePaths)
        }
        javaSourceSets.named("main").configure {
          java.includePackages(project.rootProject.layout.projectDirectory, packagePaths)
        }
      }

      override fun setResources(resources: Map<String, String>) {
        resourceCopyTask.configure {
          val baseDir = project.rootProject.layout.projectDirectory
          resources.forEach { (resourceFilePath, jarDirectoryPath) ->
            val resource = baseDir.file(resourceFilePath)
            resourceSpecs.put(resource.asFile.path, jarDirectoryPath)
            inputFiles.add(resource)
          }
        }
      }
    }

  /** The test source set */
  val test: SourceSet =
    object : SourceSet {
      override fun setPackages(packages: List<String>) {
        val packagePaths = packages.map { Path(it) }
        kotlinSourceSets.named("test").configure {
          kotlin.includePackages(project.rootProject.layout.projectDirectory, packagePaths)
        }
        javaSourceSets.named("test").configure {
          java.includePackages(project.rootProject.layout.projectDirectory, packagePaths)
        }
      }

      override fun setResources(resources: Map<String, String>) {
        throw UnsupportedOperationException(
          "Resources are only configurable for the 'main' source set."
        )
      }
    }

  interface SourceSet {
    /**
     * Sets the list of source packages that are part of the project's source set.
     *
     * Only sources directly in those packages are included and not in its subpackages.
     *
     * Example usage:
     * ```
     * daggerSources {
     *     main.setPackages(
     *         listOf(
     *             "java/dagger",
     *             "java/dagger/assisted",
     *             "java/dagger/internal",
     *             "java/dagger/multibindings",
     *         )
     *     )
     * }
     * ```
     *
     * @see daggerSources
     */
    fun setPackages(packages: List<String>)

    /**
     * Sets the resource file paths and their corresponding artifact location.
     *
     * Example usage:
     * ```
     * daggerSources {
     *     main.setResources(
     *         mapOf("java/dagger/r8.pro" to "META-INF/com.android.tools/r8/")
     *     )
     * }
     * ```
     *
     * @see daggerSources
     */
    fun setResources(resources: Map<String, String>)
  }
}

/**
 * Configure project's source set based on Dagger's project structure.
 *
 * Specifically it will include sources in the packages specified by
 * [DaggerSourceSet.SourceSet.setPackages] and resources as specified by
 * [DaggerSourceSet.SourceSet.setResources].
 */
fun Project.daggerSources(block: DaggerSourceSet.() -> Unit) {
  val kotlinExtension =
    extensions.findByType(KotlinProjectExtension::class.java)
      ?: error("The daggerSources() configuration must be applied to a Kotlin (JVM) project.")
  val javaExtension =
    extensions.findByType(JavaPluginExtension::class.java)
      ?: error("The daggerSources() configuration must be applied to a Kotlin (JVM) project.")
  val daggerSources = DaggerSourceSet(this, kotlinExtension.sourceSets, javaExtension.sourceSets)
  block.invoke(daggerSources)
}

/**
 * Includes sources from the given [packages] into this source set.
 *
 * Only sources within the package directory are included and not its sub-packages.
 */
private fun SourceDirectorySet.includePackages(baseDir: Directory, packages: Iterable<Path>) {
  setSrcDirs(listOf(baseDir))
  include(packages.map { it.pathString + "/*" })
}
