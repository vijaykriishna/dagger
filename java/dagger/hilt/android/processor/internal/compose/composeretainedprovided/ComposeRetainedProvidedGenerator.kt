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
import androidx.room.compiler.processing.XAnnotation
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.addOriginatingElement
import com.google.common.collect.ImmutableSet
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeSpec
import dagger.hilt.processor.internal.ClassNames
import dagger.hilt.processor.internal.Components
import dagger.hilt.processor.internal.Processors
import dagger.internal.codegen.xprocessing.XAnnotations
import javax.lang.model.element.Modifier

/**
 * Source generator for classes annotated as [dagger.hilt.android.compose.ComposeRetainedProvided].
 */
@OptIn(ExperimentalProcessingApi::class)
internal class ComposeRetainedProvidedGenerator
constructor(
  private val env: XProcessingEnv,
  private val metadata: ComposeRetainedProvidedMetadata
) {

  /** Generates needed files for a given type marked as `@ComposeRetainedProvided`. */
  fun generate() {
    env.filer.write(entryPointJavaFile())
    metadata.providedTypes
      .map { providedType -> accessorJavaFile(providedType) }
      .forEach { javaFile -> env.filer.write(javaFile) }
    env.filer.write(moduleJavaFile())
  }

  /**
   * Creates a JavaFile representing the generated Hilt_${ProvidedType}ComposeRetainedEntryPoint.
   *
   * For a binding of "Foo<String>", the JavaFile contains the following:
   * ```java
   * @Generated("ComposeRetainedProvidedGenerator")
   * @OriginatingElement(topLevelClass = Foo.class)
   * @GeneratedEntryPoint
   * @InstallIn(ComposeRetainedComponent.class)
   * interface Hilt_FooComposeRetainedEntryPoint {
   *   @OptionalQualifier
   *   Foo<String> get_Foo();
   * }
   * ```
   *
   * If a module has multiple methods annotated as `@ComposeRetainedProvided`, one EntryPoint is
   * generated with getter methods for each method.
   */
  private fun entryPointJavaFile(): JavaFile {
    val typeSpec =
      TypeSpec.interfaceBuilder(metadata.entryPointClassName)
        .addGeneratedCodeAnnotations(metadata.enclosingElement, env)
        .addAnnotation(Processors.getOriginatingElementAnnotation(metadata.enclosingElement))
        .addAnnotation(ClassNames.GENERATED_ENTRY_POINT)
        .addAnnotation(
          Components.getInstallInAnnotationSpec(ImmutableSet.of(COMPOSE_RETAINED_COMPONENT_CLASS))
        )

    for (providedType in metadata.providedTypes) {
      typeSpec.addMethod(getEntryPointMethodFor(providedType))
    }

    return JavaFile.builder(metadata.entryPointClassName.packageName(), typeSpec.build()).build()
  }

  /**
   * ```java
   * /* optional */ @Nullable
   * @OptionalQualifier
   * Foo<String> get_Foo();
   * ```
   */
  private fun getEntryPointMethodFor(providedType: ProvidedType): MethodSpec {
    return MethodSpec.methodBuilder(metadata.entryPointGetterMethodNameFor(providedType))
      .addOptionalQualifier(providedType.qualifier)
      .addAnnotations(providedType.nullableAnnotations.map { XAnnotations.getAnnotationSpec(it) })
      .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
      .returns(providedType.typeName)
      .build()
  }

  /**
   * Creates a JavaFile representing the generated Hilt_${CLASS}Accessor.
   *
   * For a binding of "Foo<String>", the JavaFile contains the following:
   * ```java
   * @Generated("ComposeRetainedProvidedGenerator")
   * final class Hilt_FooAccessor {
   *   private final ComposeRetainedComponent currentComposeRetainedComponent;
   *
   *   @Inject
   *   Hilt_FooAccessor(
   *       @HiltComposeInternal ComposeRetainedComponent currentComposeRetainedComponent) {
   *     this.currentComposeRetainedComponent = currentComposeRetainedComponent;
   *   }
   *
   *   Foo<String> get() {
   *     return ((Hilt_FooComposeRetainedEntryPoint) currentComposeRetainedComponent).get_Foo();
   *   }
   * }
   * ```
   *
   * If a module has multiple methods annotated as `@ComposeRetainedProvided`, one Accessor class is
   * generated for each method.
   */
  private fun accessorJavaFile(providedType: ProvidedType): JavaFile {
    val typeSpec =
      TypeSpec.classBuilder(metadata.accessorClassNameOf(providedType))
        .addGeneratedCodeAnnotations(metadata.enclosingElement, env)
        .addModifiers(Modifier.FINAL)
        .addField(
          CURRENT_COMPOSE_RETAINED_COMPONENT_PARAM.type,
          CURRENT_COMPOSE_RETAINED_COMPONENT_PARAM.name,
          Modifier.PRIVATE,
          Modifier.FINAL
        )
        .addMethod(accessorConstructor())
        .addMethod(accessorGetMethod(providedType))
        .build()

    return JavaFile.builder(metadata.accessorClassNameOf(providedType).packageName(), typeSpec)
      .build()
  }

  /**
   * ```java
   * @Inject
   * Hilt_FooAccessor(
   *     @HiltComposeInternal ComposeRetainedComponent currentComposeRetainedComponent) {
   *   this.currentComposeRetainedComponent = currentComposeRetainedComponent;
   * }
   * ```
   */
  private fun accessorConstructor(): MethodSpec {
    return MethodSpec.constructorBuilder()
      .addAnnotation(ClassNames.INJECT)
      .addParameter(CURRENT_COMPOSE_RETAINED_COMPONENT_PARAM)
      .addStatement("this.\$1L = \$1L", CURRENT_COMPOSE_RETAINED_COMPONENT_PARAM.name)
      .build()
  }

  /**
   * ```java
   * Foo<String> get() {
   *   return ((Hilt_FooComposeRetainedEntryPoint) currentComposeRetainedComponent).get_Foo();
   * }
   * ```
   */
  private fun accessorGetMethod(providedType: ProvidedType): MethodSpec {
    return MethodSpec.methodBuilder("get")
      .returns(providedType.typeName)
      .addStatement(
        "return ((\$T) \$L).\$L()",
        metadata.entryPointClassName,
        CURRENT_COMPOSE_RETAINED_COMPONENT_PARAM.name,
        metadata.entryPointGetterMethodNameFor(providedType),
      )
      .build()
  }

  /**
   * Creates a JavaFile representing the generated Hilt_${CLASS}Module.
   *
   * For a binding of "Foo<String>", the JavaFile contains the following:
   * ```java
   * @Generated("ComposeRetainedProvidedGenerator")
   * @OriginatingElement(topLevelClass = Foo.class)
   * @Module
   * @InstallIn(ComposeComponent.class)
   * abstract class Hilt_FooModule {
   *  @OptionalQualifier
   *  @Provides
   *  static Foo<String> provideFoo(Hilt_FooAccessor accessor) {
   *    return accessor.get()
   *  }
   * }
   * ```
   *
   * If the source module has multiple methods annotated as `@ComposeRetainedProvided`, one Module
   * is generated with @Binds methods for each method.
   */
  private fun moduleJavaFile(): JavaFile {
    val typeSpec =
      TypeSpec.classBuilder(metadata.moduleClassName)
        .addGeneratedCodeAnnotations(metadata.enclosingElement, env)
        .addAnnotation(Processors.getOriginatingElementAnnotation(metadata.enclosingElement))
        .addAnnotation(ClassNames.MODULE)
        .addAnnotation(
          Components.getInstallInAnnotationSpec(ImmutableSet.of(COMPOSE_COMPONENT_CLASS))
        )
        .addModifiers(Modifier.ABSTRACT)

    for (providedType in metadata.providedTypes) {
      typeSpec.addMethod(providesProvidedTypeMethod(providedType))
    }

    return JavaFile.builder(metadata.moduleClassName.packageName(), typeSpec.build()).build()
  }

  /**
   * ```java
   * /* optional */ @Nullable
   * @OptionalQualifier
   * @Provides
   * static Foo<String> provides_Foo(Hilt_FooAccessor accessor) {
   *   return accessor.get()
   * }
   * ```
   */
  private fun providesProvidedTypeMethod(providedType: ProvidedType): MethodSpec {
    return MethodSpec.methodBuilder(metadata.moduleProvidesMethodNameFor(providedType))
      .addAnnotation(ClassNames.PROVIDES)
      .addOptionalQualifier(providedType.qualifier)
      .addAnnotations(providedType.nullableAnnotations.map { XAnnotations.getAnnotationSpec(it) })
      .addModifiers(Modifier.STATIC)
      .returns(providedType.typeName)
      .addParameter(
        ParameterSpec.builder(metadata.accessorClassNameOf(providedType), "accessor").build()
      )
      .addStatement("return accessor.get()")
      .build()
  }

  companion object {
    /**
     * ClassName for [dagger.hilt.android.compose.components.ComposeComponent].
     *
     * TODO(b/281594970): Move to appropriate constants file.
     */
    private val COMPOSE_COMPONENT_CLASS: ClassName =
      ClassName.get("dagger.hilt.android.compose.components", "ComposeComponent")

    /** ClassName for [dagger.hilt.android.compose.components.ComposeRetainedComponent]. */
    private val COMPOSE_RETAINED_COMPONENT_CLASS: ClassName =
      ClassName.get("dagger.hilt.android.compose.components", "ComposeRetainedComponent")

    /**
     * Classname for @HiltComposeInternal.
     *
     * TODO(b/281594970): Move to appropriate constants file.
     */
    private val HILT_COMPOSE_INTERNAL =
      ClassName.get("dagger.hilt.android.compose.internal.qualifiers", "HiltComposeInternal")

    /**
     * ParameterSpec for [dagger.hilt.android.compose.components.ComposeRetainedComponent], named
     * "currentComposeRetainedComponent" and annotated with @HiltComposeInternal.
     */
    private val CURRENT_COMPOSE_RETAINED_COMPONENT_PARAM =
      ParameterSpec.builder(COMPOSE_RETAINED_COMPONENT_CLASS, "currentComposeRetainedComponent")
        .addAnnotation(HILT_COMPOSE_INTERNAL)
        .build()
  }
}

/**
 * Adds default required code generation annotations to the TypeSpec.Builder.
 *
 * For a class "SpecificHost", the Type will be annotated as follows:
 * ```java
 * @Generated("ComposeRetainedProvidedGenerator")
 * ```
 */
@OptIn(ExperimentalProcessingApi::class)
private fun TypeSpec.Builder.addGeneratedCodeAnnotations(
  originatingElement: XTypeElement,
  env: XProcessingEnv
): TypeSpec.Builder {
  this.addOriginatingElement(originatingElement)
  Processors.addGeneratedAnnotation(this, env, ComposeRetainedProvidedGenerator::class.java)
  return this
}

/** Adds the provided annotation to the MethodSpec.Builder, if the annotation is present. */
private fun MethodSpec.Builder.addOptionalQualifier(
  optionalQualifier: XAnnotation?
): MethodSpec.Builder {
  return if (optionalQualifier == null) this
  else this.addAnnotation(XAnnotations.getAnnotationSpec(optionalQualifier))
}
