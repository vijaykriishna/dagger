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

package dagger.hilt.android.processor.internal.compose.composeretainedprovided

import androidx.room.compiler.processing.ExperimentalProcessingApi
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XRoundEnv
import androidx.room.compiler.processing.isMethod
import androidx.room.compiler.processing.isTypeElement
import com.google.common.collect.ImmutableSet
import com.squareup.javapoet.ClassName
import dagger.hilt.processor.internal.BadInputException
import dagger.hilt.processor.internal.BaseProcessingStep
import dagger.internal.codegen.xprocessing.XElements

/** Annotation processing step for [dagger.hilt.android.compose.ComposeRetainedProvided]. */
@OptIn(ExperimentalProcessingApi::class)
class ComposeRetainedProvidedProcessingStep(env: XProcessingEnv) : BaseProcessingStep(env) {

  private val annotatedMethodElements = mutableListOf<XMethodElement>()

  override fun annotationClassNames() =
    ImmutableSet.of(ComposeRetainedProvidedMetadata.COMPOSE_RETAINED_PROVIDED_CLASS_NAME)

  override fun processEach(annotation: ClassName, element: XElement) {
    when {
      element.isMethod() -> {
        // Methods annotated as `@ComposeRetainedProvided` need to be collected and processed later
        // since only one EntryPoint and one Module are generated for all the methods in a given
        // enclosing class, i.e. module. The EntryPoint and Module have methods corresponding to
        // each non-generated method annotated as `@ComposeRetainedProvided`.
        annotatedMethodElements.add(XElements.asMethod(element))
      }
      element.isTypeElement() && element.isClass() -> {
        // Classes annotated as `@ComposeRetainedProvided` can be processed immediately as they're
        // both the enclosing class and the single provided type.
        val metadata = ComposeRetainedProvidedMetadata.forClass(element)
        ComposeRetainedProvidedGenerator(processingEnv(), metadata).generate()
      }
      else -> {
        throw BadInputException(
          "@${ComposeRetainedProvidedMetadata.COMPOSE_RETAINED_PROVIDED_CLASS_NAME.simpleName()} " +
            "can only be used with method and class types: \n\t${XElements.toStableString(element)}"
        )
      }
    }
  }

  override fun postProcess(env: XProcessingEnv, round: XRoundEnv) {
    try {
      annotatedMethodElements
        .groupBy { method -> XElements.asTypeElement(method.enclosingElement) }
        .map { entry -> ComposeRetainedProvidedMetadata.forMethods(entry.key, entry.value) }
        .forEach { metadata -> ComposeRetainedProvidedGenerator(env, metadata).generate() }
    } finally {
      annotatedMethodElements.clear()
    }
  }
}
