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
 * Provides a way to retrieve the given ComponentHost type from an appropriate component.
 *
 * This class should never be used on its own. It should always be used with [rememberComponentHost]
 * to ensure that the lifecycle of the given host is correct.
 *
 * The annotation processor will generate appropriate classes to ensure that it's possible to inject
 * `ComponentHostCreator<SpecificHost>`.
 *
 * @param HostT the type annotated with [ComposeComponentHost], which is used here to differentiate one
 *   ComponentHostCreator from another.
 */
interface ComponentHostCreator<out HostT>
