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

package dagger.hilt.android.compose.internal

import dagger.hilt.android.compose.ComponentHostCreator
import dagger.hilt.android.compose.ComposeComponentHostExtras
import dagger.hilt.android.compose.components.ComposeRetainedComponent

/**
 * Parallel interface to [dagger.hilt.android.compose.ComponentHostCreator] that exists to prevent
 * Hilt clients from improperly creating Component Hosts.
 *
 * ComponentHostCreator is a marker interface that is only used as a parameter to
 * [dagger.hilt.android.compose.rememberComponentHost]. Within rememberComponentHost, we cast
 * ComponentHostCreator to InternalComponentHostCreator to call createComponentHost. This is a safe
 * cast because the generated ComponentHostCreator always implements both interfaces.
 *
 * Creating a Host without going through rememberComponentHost is wrong and will result in the host
 * having an incorrect lifecycle.
 *
 * @param HostT the underlying type annotated with
 *   [dagger.hilt.android.compose.ComposeComponentHost].
 */
interface InternalComponentHostCreator<out HostT> : ComponentHostCreator<HostT> {
  fun createComponentHost(
    extras: ComposeComponentHostExtras?,
    retainedComponent: ComposeRetainedComponent,
  ): HostT

  fun createRetainedComponent(extras: ComposeComponentHostExtras?): ComposeRetainedComponent
}
