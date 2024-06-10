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

package dagger.hilt.android.processor.internal.compose.composecomponenthost

import androidx.room.compiler.processing.ExperimentalProcessingApi
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XProcessingEnv
import com.google.common.collect.ImmutableSet
import com.squareup.javapoet.ClassName
import dagger.hilt.processor.internal.BaseProcessingStep
import dagger.internal.codegen.xprocessing.XElements

/** Annotation processing step for [dagger.hilt.android.compose.ComposeComponentHost]. */
@OptIn(ExperimentalProcessingApi::class)
class ComposeComponentHostProcessingStep(env: XProcessingEnv) : BaseProcessingStep(env) {
  override fun annotationClassNames() = ImmutableSet.of(composeComponentHostClass)

  override fun processEach(annotation: ClassName, element: XElement) {
    val metadata = ComposeComponentHostMetadata.create(XElements.asTypeElement(element))
    ComposeComponentHostGenerator(processingEnv(), metadata).generate()
  }

  companion object {
    /**
     * ClassName for [dagger.hilt.android.compose.ComposeComponentHost].
     *
     * TODO(b/281594970): Move to appropriate constants file.
     */
    private val composeComponentHostClass: ClassName =
      ClassName.get("dagger.hilt.android.compose", "ComposeComponentHost")
  }
}
