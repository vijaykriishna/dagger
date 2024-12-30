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

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isDirectory

private typealias JavaSourceSet = org.gradle.api.tasks.SourceSet

@DslMarker
annotation class DaggerGradleDsl

@DaggerGradleDsl
class DaggerSourceSet(
    private val project: Project,
    private val kotlinSourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
    private val javaSourceSets: NamedDomainObjectContainer<JavaSourceSet>,
) {
    val main: SourceSet = object : SourceSet {
        override fun setPackages(packages: List<String>) {
            val packagePaths = packages.map { Path(it) }
            kotlinSourceSets.getByName("main").kotlin
                .includePackages("${project.rootDir}/java", packagePaths)
            javaSourceSets.getByName("main").java
                .includePackages("${project.rootDir}/java", packagePaths)
        }
    }
    val test: SourceSet = object : SourceSet {
        override fun setPackages(packages: List<String>) {
            val packagePaths = packages.map { Path(it) }
            kotlinSourceSets.getByName("test").kotlin
                .includePackages("${project.rootDir}/javatests", packagePaths)
            javaSourceSets.getByName("test").java
                .includePackages("${project.rootDir}/javatests", packagePaths)
        }
    }

    interface SourceSet {
        fun setPackages(packages: List<String>)
    }
}

/**
 * Configure project's source set based on Dagger's project structure.
 *
 * Specifically it will include sources in the packages specified by [DaggerSourceSet.SourceSet.setPackages].
 */
fun Project.daggerSources(block: DaggerSourceSet.() -> Unit) {
    val kotlinExtension = extensions.findByType(KotlinProjectExtension::class.java)
        ?: error("The daggerSources() configuration must be applied to a Kotlin (JVM) project.")
    val javaExtension = extensions.findByType(JavaPluginExtension::class.java)
        ?: error("The daggerSources() configuration must be applied to a Kotlin (JVM) project.")
    val daggerSources = DaggerSourceSet(this, kotlinExtension.sourceSets, javaExtension.sourceSets)
    block.invoke(daggerSources)
}

/**
 * Includes sources from the given [packages] into this source set.
 *
 * Only sources within the package directory are included and not its sub-packages.
 */
private fun SourceDirectorySet.includePackages(
    basePath: String,
    packages: Iterable<Path>,
) {
    val packagesDirectories = packages.flatMap { it.expandParts() }.toSet()
    setSrcDirs(listOf(basePath)).include {
        val path = Path(it.path)
        if (Path(basePath).resolve(path).isDirectory()) {
            path in packagesDirectories
        } else {
            path.parent in packages
        }
    }
}

/**
 * Expands a [Path] to includes it parents.
 *
 * i.e. for `"foo/bar"` it will expand to `setOf("foo", foo/bar")`
 */
private fun Path.expandParts(): Set<Path> {
    return buildSet {
        var path: Path? = this@expandParts
        while (path != null) {
            add(path)
            path = path.parent
        }
    }
}