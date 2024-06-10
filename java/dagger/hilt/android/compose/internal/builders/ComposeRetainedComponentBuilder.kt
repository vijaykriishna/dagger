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

package dagger.hilt.android.compose.internal.builders

import dagger.BindsInstance
import dagger.hilt.DefineComponent
import dagger.hilt.android.compose.ComposeComponentHostExtras
import dagger.hilt.android.compose.components.ComposeRetainedComponent

/** Builder used to create a [ComposeRetainedComponent]. */
@DefineComponent.Builder
interface ComposeRetainedComponentBuilder {
  fun bindComposeComponentHostExtras(
    @BindsInstance extras: ComposeComponentHostExtras?
  ): ComposeRetainedComponentBuilder

  fun build(): ComposeRetainedComponent
}
