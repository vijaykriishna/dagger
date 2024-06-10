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

import androidx.room.compiler.codegen.toJavaPoet
import androidx.room.compiler.processing.ExperimentalProcessingApi
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.compat.XConverters.getProcessingEnv
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import dagger.hilt.processor.internal.ClassNames
import dagger.hilt.processor.internal.ProcessorErrors
import dagger.hilt.processor.internal.Processors
import javax.lang.model.element.Modifier

/**
 * Validates information about a class annotated with @ComposeComponentHost and provides metadata
 * about it for code generation.
 */
internal class ComposeComponentHostMetadata private constructor(val element: XTypeElement) {

  /** JavaPoet ClassName for ${CLASS}. */
  val elementClassName: ClassName = element.asClassName().toJavaPoet()

  /**
   * The class name of the generated class which holds onto the ComposeComponent and
   * ComposeRetainedComponent. Has the form: Hilt_${CLASS}.
   */
  val componentHolderClassName: ClassName = componentHolderClassName(element)

  /**
   * The class name of the generated EntryPoint providing ${CLASS}. Has the form:
   * Hilt_${CLASS}EntryPoint.
   */
  val hostEntryPointClassName: ClassName = Processors.append(componentHolderClassName, "EntryPoint")

  /**
   * The class name of the generated class which implements ComponentHostCreator<${CLASS}>. Has the
   * form: Hilt_${CLASS}ComponentHostCreator.
   */
  val creatorClassName: ClassName =
    Processors.append(componentHolderClassName, "ComponentHostCreator")

  /** [TypeName] for ComponentHostCreator<${CLASS}>. */
  val componentHostCreatorInterfaceTypeName: TypeName =
    ParameterizedTypeName.get(COMPONENT_HOST_CREATOR_INTERFACE_CLASS, element.type.typeName)

  /** [TypeName] for InternalComponentHostCreator<${CLASS}>. */
  val internalComponentHostCreatorInterfaceTypeName: TypeName =
    ParameterizedTypeName.get(
      INTERNAL_COMPONENT_HOST_CREATOR_INTERFACE_CLASS,
      element.type.typeName
    )

  /**
   * The class name of the generated Dagger module which provides appropriate bindings for the
   * generated classes. Has the form: Hilt_${CLASS}ComponentHostCreatorModule.
   */
  val moduleClassName: ClassName = Processors.append(creatorClassName, "Module")

  /**
   * Modifiers that should be applied to the generated component holder class.
   *
   * <p>Note that the generated class must have public visibility if used by a
   * public @ComposeComponentHost-annotated kotlin class. See:
   * https://discuss.kotlinlang.org/t/why-does-kotlin-prohibit-exposing-restricted-visibility-types/7047
   */
  fun componentHolderModifiers(): Array<Modifier> {
    // Note XElement#isPublic() refers to the jvm visibility. Since "internal" visibility is
    // represented as public in the jvm, we have to check XElement#isInternal() explicitly.
    return if (
      element.isFromKotlin() && element.isPublic() && !element.isInternal()
    ) {
      arrayOf(Modifier.PUBLIC)
    } else {
      arrayOf()
    }
  }

  companion object {
    @ExperimentalProcessingApi
    internal fun create(typeElement: XTypeElement): ComposeComponentHostMetadata {
      ProcessorErrors.checkState(
        typeElement.getConstructors().any { it.hasAnnotation(ClassNames.INJECT) },
        "@ComposeComponentHost annotated classes should have an @Inject constructor."
      )

      // TODO(b/279185507): Revisit if we should have this requirement.
      ProcessorErrors.checkState(
        typeElement.getAllMethods().any { it.hasAnnotation(COMPOSABLE_ANNOTATION_CLASS) },
        "@ComposeComponentHost annotated classes should have at least one @Composable function. " +
          "Use a regular class with an @Inject constructor for classes without composables."
      )

      val superTypeName =
        typeElement.superClass?.typeElement?.asClassName()?.toJavaPoet()?.simpleName()
      val expectedSuperTypeName = componentHolderClassName(typeElement).simpleName()
      // TODO(b/288210593): Add this check back to KSP once this bug is fixed.
      if (typeElement.getProcessingEnv().backend == XProcessingEnv.Backend.JAVAC) {
        ProcessorErrors.checkState(
          expectedSuperTypeName.contentEquals(superTypeName),
          typeElement,
          "@ComposeComponentHost annotated class expected to extend %s. Found: %s",
          expectedSuperTypeName,
          superTypeName
        )
      }

      return ComposeComponentHostMetadata(typeElement)
    }

    private fun componentHolderClassName(typeElement: XTypeElement) =
      Processors.prepend(Processors.getEnclosedClassName(typeElement), "Hilt_")

    /**
     * [ClassName] for ComponentHostCreator<T>.
     *
     * TODO(b/281594970): Move to appropriate constants file.
     */
    private val COMPONENT_HOST_CREATOR_INTERFACE_CLASS: ClassName =
      ClassName.get("dagger.hilt.android.compose", "ComponentHostCreator")

    /**
     * [ClassName] for InternalComponentHostCreator<T>.
     *
     * TODO(b/281594970): Move to appropriate constants file.
     */
    private val INTERNAL_COMPONENT_HOST_CREATOR_INTERFACE_CLASS: ClassName =
      ClassName.get("dagger.hilt.android.compose.internal", "InternalComponentHostCreator")

    /**
     * [ClassName] for @Composable.
     *
     * TODO(b/281594970): Move to appropriate constants file.
     */
    private val COMPOSABLE_ANNOTATION_CLASS: ClassName =
      ClassName.get("androidx.compose.runtime", "Composable")
  }
}
