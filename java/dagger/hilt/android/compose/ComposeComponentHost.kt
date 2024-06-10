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

import dagger.hilt.GeneratesRootInput

/**
 * Annotation used to indicate that the annotated type hosts the Compose and ComposeRetained Dagger
 * components.
 *
 * See the following example on how to use `@ComposeComponentHost`:
 * ```
 * /** Hosts the MyScreen Composable. Hilt_MyHost is generated and must be the super type. */
 * @ComposeComponentHost
 * class MyHost @Inject internal constructor(dependency: MyInjectedDependency) : Hilt_MyHost {
 *
 *   @Composable
 *   fun MyScreen(modifier: Modifier = Modifier) {
 *     // Define MyScreen, using data from the injected dependency
 *   }
 * }
 * ```
 *
 * `MyHost` is annotated with `@ComposeComponentHost`, so Hilt generates a
 * `ComponentHostCreator<MyHost>` and provides a binding for it. `MyHost` is injected and used as
 * follows:
 * ```
 * @AndroidEntryPoint(ComponentActivity::class)
 * class HomeActivity: Hilt_HomeActivity() {
 *   @Inject lateinit var myHostCreator: ComponentHostCreator<MyHost>
 *
 *   override fun onCreate(bundle: Bundle?) {
 *     super.onCreate(bundle)
 *     setContent {
 *       // myHostCreator must be used with rememberComponentHost to have the correct lifecycle
 *       val myHost = rememberComponentHost(myHostCreator)
 *       myHost.MyScreen()
 *     }
 *   }
 * }
 * ```
 *
 * As with other Hilt components, bindings can be scoped to either the ComposeComponent or
 * ComposeRetainedComponent by using the appropriate scope annotation:
 * [dagger.hilt.android.compose.scopes.ComposeScoped] for the ComposeComponent and
 * [dagger.hilt.android.compose.scopes.ComposeRetainedScoped] for the ComposeRetainedComponent.
 * Annotating a binding as such results in Hilt providing the same instance of that binding every
 * time it's requested in the corresponding component. For example:
 * ```
 * /**
 *  * Because ComposeScopedDependency is @ComposeScoped,
 *  * firstDependency.scopedDependency === secondDependency.scopedDependency.
 *  */
 * @ComposeComponentHost
 * class MyHost @Inject internal constructor(
 *   private val firstDependency: FirstDependency,
 *   private val secondDependency: SecondDependency
 * ) {...}
 *
 * class FirstDependency @Inject constructor(private val scopedDependency: ComposeScopedDependency)
 * class SecondDependency @Inject constructor(private val scopedDependency: ComposeScopedDependency)
 *
 * @ComposeScoped
 * class ComposeScopedDependency @Inject constructor()
 * ```
 *
 * The equivalent applies to `@ComposeRetainedScoped` annotated bindings, however additional
 * machinery is needed. See the documentation on
 * [dagger.hilt.android.compose.ComposeRetainedProvided] for details.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@GeneratesRootInput
annotation class ComposeComponentHost
