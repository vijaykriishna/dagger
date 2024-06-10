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

import androidx.room.compiler.codegen.toJavaPoet
import androidx.room.compiler.processing.XAnnotation
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XTypeElement
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import dagger.hilt.processor.internal.ClassNames
import dagger.hilt.processor.internal.Components
import dagger.hilt.processor.internal.ProcessorErrors
import dagger.hilt.processor.internal.Processors
import dagger.internal.codegen.xprocessing.XElements

/**
 * Validates information about a class or module method annotated with
 * [dagger.hilt.android.compose.ComposeRetainedProvided] and provides metadata about it for code
 * generation.
 *
 * @param enclosingElement The element enclosing the provided type as an XTypeElement. For classes
 *   annotated as `@ComposeRetainedScoped`, this is the class itself. For methods annotated as
 *   `@ComposeRetainedScoped`, this is the module which defines the method.
 * @param providedTypes A list of all of the types provided by the [enclosingElement] as
 *   [ProvidedTypes][ProvidedType]. For classes annotated as `@ComposeRetainedScoped`, this is the
 *   class itself. For methods annotated as `@ComposeRetainedScoped`, this is the return type of the
 *   method.
 */
internal class ComposeRetainedProvidedMetadata
private constructor(val enclosingElement: XTypeElement, val providedTypes: List<ProvidedType>) {

  /** JavaPoet ClassName for ${ENCLOSING_ELEMENT}. */
  private val enclosingElementClassName: ClassName = enclosingElement.asClassName().toJavaPoet()

  /**
   * Returns the base class name for the [enclosingElement]. Has the form:
   * Hilt_${ENCLOSING_ELEMENT}.
   *
   * This class name is not directly used in code generation, rather it's the base class name that
   * other generated classes build upon.
   *
   * For classes annotated as `@ComposeRetainedScoped`, this is Hilt_${PROVIDED_TYPE}. For module
   * methods annotated as `@ComposeRetainedScoped`, this is is Hilt_${MODULE_NAME}.
   */
  private val baseEnclosingElementClassName: ClassName =
    Processors.prepend(Processors.getEnclosedClassName(enclosingElementClassName), "Hilt_")

  /**
   * The class name of the generated EntryPoint providing access to ${CLASS} outside of the
   * ComposeRetainedComponent. Has the form Hilt_${ENCLOSING_ELEMENT}ComposeRetainedEntryPoint.
   */
  val entryPointClassName: ClassName =
    Processors.append(baseEnclosingElementClassName, "ComposeRetainedEntryPoint")

  /**
   * The class name of the generated Dagger module which provides appropriate bindings for the
   * generated classes. Has the form: Hilt_${ENCLOSING_ELEMENT}Module.
   */
  val moduleClassName: ClassName = Processors.append(baseEnclosingElementClassName, "Module")

  /**
   * The EntryPoint method name which provides access to [providedType]. Has the form
   * "get_${fullyQualifiedEnclosingElement}${providedType.uniqueIdentifier}".
   */
  fun entryPointGetterMethodNameFor(providedType: ProvidedType): String {
    return "get_${Processors.getFullEnclosedName(enclosingElement)}${providedType.uniqueIdentifier}"
  }

  /**
   * The class name of the generated accessor class which has a getter for [providedType]. Has the
   * form: Hilt_${ENCLOSING_ELEMENT}_${providedType.uniqueIdentifier}Accessor.
   */
  fun accessorClassNameOf(providedType: ProvidedType): ClassName =
    Processors.append(baseEnclosingElementClassName, "_${providedType.uniqueIdentifier}Accessor")

  /**
   * The Module method name which provides the ProvidedType. Has the form
   * "provides_${fullyQualifiedEnclosingElement}${providedType.uniqueIdentifier}".
   */
  fun moduleProvidesMethodNameFor(providedType: ProvidedType): String {
    return "provides_${Processors.getFullEnclosedName(enclosingElement)}${providedType.uniqueIdentifier}"
  }

  companion object {

    /** Creates metadata for a class which is annotated as `@ComposeRetainedProvided`. */
    internal fun forClass(injectableClass: XTypeElement): ComposeRetainedProvidedMetadata {
      ProcessorErrors.checkState(
        injectableClass.getConstructors().any { it.hasAnnotation(ClassNames.INJECT) },
        "@ComposeRetainedProvided annotated classes should have an @Inject constructor:\n  %s",
        XElements.toStableString(injectableClass),
      )

      checkAllScopeAnnotationsAreComposeRetainedScoped(injectableClass)

      return ComposeRetainedProvidedMetadata(
        injectableClass,
        listOf(
          ProvidedType(
            uniqueIdentifier = injectableClass.asClassName().toJavaPoet().simpleName(),
            typeName = injectableClass.type.typeName
          )
        ),
      )
    }

    /**
     * Creates metadata for a group of methods annotated as `@ComposeRetainedProvided`. The methods
     * should all be declared within the same [enclosingElement].
     */
    internal fun forMethods(
      enclosingElement: XTypeElement,
      composeRetainedProvidedMethods: List<XMethodElement>
    ): ComposeRetainedProvidedMetadata {
      val installInComponents = Components.getComponents(enclosingElement)
      ProcessorErrors.checkState(
        installInComponents.size == 1 &&
          installInComponents.contains(COMPOSE_RETAINED_COMPONENT_CLASS),
        enclosingElement,
        "@%s annotated methods must be inside @InstallIn(ComposeRetainedComponent.class) " +
          "annotated classes:\n  %s",
        COMPOSE_RETAINED_PROVIDED_CLASS_NAME.simpleName(),
        XElements.toStableString(enclosingElement)
      )

      val methodReturnTypes = mutableListOf<ProvidedType>()

      for (composeRetainedProvidedMethod in composeRetainedProvidedMethods) {
        ProcessorErrors.checkState(
          composeRetainedProvidedMethod.hasAnyAnnotation(ClassNames.PROVIDES, ClassNames.BINDS),
          composeRetainedProvidedMethod,
          "@%s annotated methods must also be annotated @Provides or @Binds:\n  %s",
          COMPOSE_RETAINED_PROVIDED_CLASS_NAME.simpleName(),
          XElements.toStableString(composeRetainedProvidedMethod)
        )

        checkAllScopeAnnotationsAreComposeRetainedScoped(composeRetainedProvidedMethod)

        ProcessorErrors.checkState(
          !composeRetainedProvidedMethod.hasAnyAnnotation(
            ClassNames.INTO_SET,
            ClassNames.INTO_MAP,
            ClassNames.ELEMENTS_INTO_SET
          ),
          composeRetainedProvidedMethod,
          "Multibindings are not supported for @%s annotated methods:\n  %s",
          COMPOSE_RETAINED_PROVIDED_CLASS_NAME.simpleName(),
          XElements.toStableString(composeRetainedProvidedMethod)
        )

        val nullableAnnotations =
          composeRetainedProvidedMethod.getAllAnnotations().filter {
            XElements.getSimpleName(it.typeElement) == "Nullable"
          }

        // At this point, we know the method is a @Provides or @Binds method. Other annotation
        // processors guarantee that such methods have a return type and at most one @Qualifier
        // annotation, so it's safe to get that information without validating it.
        methodReturnTypes.add(
          ProvidedType(
            uniqueIdentifier = XElements.getSimpleName(composeRetainedProvidedMethod),
            typeName = composeRetainedProvidedMethod.returnType.typeName,
            qualifier =
              Processors.getQualifierAnnotations(composeRetainedProvidedMethod).firstOrNull(),
            nullableAnnotations = nullableAnnotations,
          )
        )
      }

      return ComposeRetainedProvidedMetadata(enclosingElement, methodReturnTypes)
    }

    private fun checkAllScopeAnnotationsAreComposeRetainedScoped(element: XElement) {
      val scopeAnnotations = Processors.getScopeAnnotations(element)
      ProcessorErrors.checkState(
        scopeAnnotations.isNotEmpty(),
        "@%s annotated classes and methods must be annotated @%s:\n  %s",
        COMPOSE_RETAINED_PROVIDED_CLASS_NAME.simpleName(),
        COMPOSE_RETAINED_SCOPED_CLASS_NAME.simpleName(),
        XElements.toStableString(element),
      )

      for (scopeAnnotation in Processors.getScopeAnnotations(element)) {
        ProcessorErrors.checkState(
          scopeAnnotation.className == COMPOSE_RETAINED_SCOPED_CLASS_NAME,
          "@%s annotated classes and methods can only be annotated @%s:\n  @%s %s",
          COMPOSE_RETAINED_PROVIDED_CLASS_NAME.simpleName(),
          COMPOSE_RETAINED_SCOPED_CLASS_NAME.simpleName(),
          scopeAnnotation.name,
          XElements.toStableString(element),
        )
      }
    }

    /**
     * ClassName for [dagger.hilt.android.compose.ComposeRetainedProvided]
     *
     * TODO(b/281594970): Move to appropriate constants file.
     */
    internal val COMPOSE_RETAINED_PROVIDED_CLASS_NAME: ClassName =
      ClassName.get("dagger.hilt.android.compose", "ComposeRetainedProvided")

    /**
     * Class name for @ComposeRetainedScoped.
     *
     * TODO(b/281594970): Move to appropriate constants file.
     */
    private val COMPOSE_RETAINED_SCOPED_CLASS_NAME: ClassName =
      ClassName.get("dagger.hilt.android.compose.scopes", "ComposeRetainedScoped")

    /** ClassName for [dagger.hilt.android.compose.components.ComposeRetainedComponent]. */
    private val COMPOSE_RETAINED_COMPONENT_CLASS: ClassName =
      ClassName.get("dagger.hilt.android.compose.components", "ComposeRetainedComponent")
  }
}

/**
 * Represents a type which is provided by either a class or a method annotated with
 * `@ComposeRetainedProvided`.
 *
 * @param uniqueIdentifier a unique identifier used when generating classes and methods for the
 *   provided type. For example, the generated EntryPoint needs a getter method to return the
 *   typeName.
 * @param typeName the TypeName of the provided type.
 * @param qualifier an optional qualifier for the provided type.
 */
internal data class ProvidedType(
  val uniqueIdentifier: String,
  val typeName: TypeName,
  val qualifier: XAnnotation? = null,
  val nullableAnnotations: List<XAnnotation> = emptyList(),
)
