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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.withType

class ShadowConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(project.getPluginIdByName("shadow"))

        val shadeExtension = project.extensions.create<ShadeExtension>("shading")

        // Configuration for shaded dependencies
        val shadedConfiguration = project.configurations.create("shaded") {
            isCanBeConsumed = false
            isCanBeResolved = true
            isTransitive = false // Do not include transitive dependencies of shaded deps
        }

        // Shaded dependencies are compile only dependencies
        project.configurations.named("compileOnly").configure { extendsFrom(shadedConfiguration) }

        val shadowTask = project.tasks.withType<ShadowJar>().named("shadowJar") {
            // Use no classifier, the shaded jar is the one to be published.
            archiveClassifier.set("")
            // Set the 'shaded' configuration as the dependencies configuration to shade
            configurations = listOf(shadedConfiguration)
            // Enable service files merging
            mergeServiceFiles()
            // Enable package relocation (necessary for project that only relocate but have no
            // shaded deps)
            isEnableRelocation = true

            shadeExtension.rules.forEach { (from, to) ->
                relocate(from, to)
            }
        }

        // Change the default jar task classifier to avoid conflicting with the shaded one.
        project.tasks.withType<Jar>().named("jar").configure {
            archiveClassifier.set("before-shade")
        }

        configureOutgoingArtifacts(project, shadowTask)
    }

    /**
     * Configure Gradle consumers (that use Gradle publishing metadata) of the project to use the
     * shaded jar.
     *
     * This is necessary so that the publishing Gradle module metadata references the shaded jar.
     * See https://github.com/GradleUp/shadow/issues/847
     */
    private fun configureOutgoingArtifacts(project: Project, task: TaskProvider<ShadowJar>) {
        project.configurations.configureEach {
            if (name == "apiElements" || name == "runtimeElements") {
                outgoing.artifacts.clear()
                outgoing.artifact(task)
            }
        }
    }
}

abstract class ShadeExtension {
    internal val rules = mutableMapOf<String, String>()

    fun relocate(fromPackage: String, toPackage: String) {
        check(!rules.containsKey(fromPackage)) {
            "Duplicate shading rule declared for $fromPackage"
        }
        rules[fromPackage] = toPackage
    }
}