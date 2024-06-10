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

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider

/**
 * A unique label for a node based on the provided "path" to it in the Compose tree.
 *
 * For example, for a Compose node called "Count", which is nested in a "CounterWidget" that's
 * placed in "MyTestActivity", call `NodeLabel("MyTestActivity", "CounterWidget", "Count")`.
 */
class NodeLabel(vararg path: String) {
  /**
   * The string version of the provided path, created by placing " -> " between each part of the
   * path and wrapping the whole string in brackets.
   */
  private val label: String = path.joinToString(prefix = "[", separator = " -> ", postfix = "]")

  override fun toString(): String {
    return "NodeLabel(label='$label')"
  }
}

/**
 * Creates a Compose node with the given [nodeLabel].
 *
 * Use [onNodeWithLabel] or [onAllNodesWithLabel] to find nodes with a given label in the
 * composition.
 */
@Composable
fun LabeledNode(nodeLabel: NodeLabel, modifier: Modifier = Modifier) {
  // The `text` parameter needs to be non-empty for clicks to work well.
  Text(text = "LabeledNode", modifier.nodeLabel(nodeLabel))
}

/**
 * Adds a [NodeLabel] to the Modifier.
 *
 * Use [onNodeWithLabel] or [onAllNodesWithLabel] to find nodes in the composition with a given
 * label.
 */
fun Modifier.nodeLabel(label: NodeLabel): Modifier = semantics(properties = { nodeLabel = label })

/** Finds a semantics node with the given label. */
fun SemanticsNodeInteractionsProvider.onNodeWithLabel(label: NodeLabel): SemanticsNodeInteraction =
  this.onNode(hasNodeLabel(label))

/** Finds all semantics nodes with the given label. */
fun SemanticsNodeInteractionsProvider.onAllNodesWithLabel(
  label: NodeLabel
): SemanticsNodeInteractionCollection = this.onAllNodes(hasNodeLabel(label))

/**
 * Returns a [SemanticsMatcher] which checks if the associated node has some
 * [nodeLabel][Modifier.nodeLabel].
 */
private fun hasNodeLabel(label: NodeLabel): SemanticsMatcher =
  SemanticsMatcher.expectValue(nodeLabelKey, label)

/** Key used to identify the NodeLabel in SemanticsProperties. */
private val nodeLabelKey: SemanticsPropertyKey<NodeLabel> =
  SemanticsPropertyKey(name = "NodeLabel", mergePolicy = { parentValue, _ -> parentValue })

/** Property which stores the NodeLabel itself. */
private var SemanticsPropertyReceiver.nodeLabel by nodeLabelKey
