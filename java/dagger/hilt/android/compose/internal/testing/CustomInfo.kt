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

package dagger.hilt.android.compose.internal.testing

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import com.google.errorprone.annotations.CanIgnoreReturnValue

/**
 * Adds custom info to the Modifier.
 *
 * Use [assertHasCustomInfo] to check if a given node has some customInfo.
 */
fun Modifier.customInfo(info: Any?): Modifier = semantics(properties = { customInfo = info })

/**
 * Asserts that the current semantics node has the given [customInfo][Modifier.customInfo].
 *
 * Use [Modifier.customInfo] to add custom info to a node.
 */
@CanIgnoreReturnValue
fun SemanticsNodeInteraction.assertHasCustomInfo(info: Any?): SemanticsNodeInteraction =
  this.assert(hasCustomInfo(info))

/**
 * Returns a [SemanticsMatcher] which checks if the associated node has some
 * [customInfo][Modifier.customInfo].
 */
private fun hasCustomInfo(info: Any?): SemanticsMatcher =
  SemanticsMatcher.expectValue(customInfoKey, info)

/** Key used to identify the customInfo in SemanticsProperties. */
private val customInfoKey: SemanticsPropertyKey<Any?> =
  SemanticsPropertyKey(name = "CustomInfo", mergePolicy = { parentValue, _ -> parentValue })

/** Property which stores the customInfo itself. */
private var SemanticsPropertyReceiver.customInfo by customInfoKey
