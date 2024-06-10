/*
 * Copyright (C) 2023 The Dagger Authors.
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

package dagger.hilt.android.compose

/**
 * Annotation used to indicate that the annotated binding can be injected from the
 * [dagger.hilt.android.compose.components.ComposeComponent].
 *
 * Classes can be annotated `@ComposeRetainedProvided`:
 * ```kotlin
 * @ComposeRetainedScoped
 * @ComposeRetainedProvided
 * class MyRetainedClass @Inject constructor(...) {...}
 * ```
 *
 * `@Provides` and `@Binds` methods can also be annotated `@ComposeRetainedProvided`:
 * ```kotlin
 * @Module
 * @InstallIn(ComposeRetainedComponent.class)
 * object MyRetainedClassModule {
 *
 *   @Provides
 *   @ComposeRetainedScoped
 *   @ComposeRetainedProvided
 *   fun provideMyRetainedClass(...) {
 *     return MyRetainedClass(...)
 *   }
 * }
 * ```
 *
 * Annotating a binding with `@ComposeRetainedProvided` will cause Hilt to generate code to provide
 * it in the [dagger.hilt.android.compose.components.ComposeComponent]. Any other binding that is in
 * the ComposeComponent can then inject and use the annotated type directly.
 *
 * For example:
 * ```kotlin
 * class MyClassInComposeComponent @Inject constructor(private val retainedDep: MyRetainedClass) {
 *
 *   @Composable
 *   fun MyComposable() {
 *     retainedDep.doSomethingWithRetainedDep()
 *   }
 * }
 * ```
 *
 * Similarly, since ComponentHosts are bindings in the ComposeComponent, `MyRetainedClass` can be
 * directly injected into the class annotated as `@ComposeComponentHost`:
 * ```kotlin
 * @ComposeComponentHost
 * class MyComposeComponentHost @Inject constructor(private val retainedDep: MyRetainedDep) {...}
 * ```
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ComposeRetainedProvided
