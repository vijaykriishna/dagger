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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.jvm.toolchain.JavaLanguageVersion

class KotlinJvmConventionPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.apply(project.getPluginIdByName("kotlinJvm"))

        project.plugins.withId(project.getPluginIdByName("kotlinJvm")) {
            val kotlinProject = project.extensions.getByName("kotlin") as KotlinJvmProjectExtension
            kotlinProject.explicitApi()
            kotlinProject.jvmToolchain {
                languageVersion.set(JavaLanguageVersion.of(project.getVersionByName("jdk")))
            }
            kotlinProject.compilerOptions.apply {
                languageVersion.set(KotlinVersion.fromVersion(project.getVersionByName("kotlinTarget")))
                apiVersion.set(KotlinVersion.fromVersion(project.getVersionByName("kotlinTarget")))
                jvmTarget.set(JvmTarget.fromTarget(project.getVersionByName("jvmTarget")))
            }
        }
    }
}