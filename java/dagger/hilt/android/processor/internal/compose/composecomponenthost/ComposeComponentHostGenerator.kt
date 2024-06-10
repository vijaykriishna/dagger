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
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.addOriginatingElement
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeSpec
import dagger.hilt.android.processor.internal.AndroidClassNames
import dagger.hilt.processor.internal.ClassNames
import dagger.hilt.processor.internal.Processors
import javax.lang.model.element.Modifier

/** Source generator for classes annotated as @ComposeComponentHost. */
@OptIn(ExperimentalProcessingApi::class)
internal class ComposeComponentHostGenerator(
  private val env: XProcessingEnv,
  private val metadata: ComposeComponentHostMetadata
) {

  /**
   * Generates three separate classes for a given $CLASS annotated as `@ComposeComponentHost`:
   * Hilt_${CLASS}, Hilt_${CLASS}ComponentHostCreator, and ${CLASS}_HiltModule.
   */
  fun generate() {
    env.filer.write(componentHolderJavaFile())
    env.filer.write(componentHostEntryPointJavaFile())
    env.filer.write(componentHostCreatorJavaFile())
    env.filer.write(moduleJavaFile())
  }

  /**
   * Creates a JavaFile representing the generated Hilt_${CLASS}.
   *
   * For a class "SpecificHost", the JavaFile contains the following:
   * ```java
   * @Generated("ComposeComponentHostGenerator")
   * class Hilt_SpecificHost {}
   * ```
   */
  private fun componentHolderJavaFile(): JavaFile {
    val typeSpec =
      TypeSpec.classBuilder(metadata.componentHolderClassName)
        .addGeneratedCodeAnnotations(metadata.element, env)
        .addModifiers(*metadata.componentHolderModifiers())
        .build()
    return JavaFile.builder(metadata.componentHolderClassName.packageName(), typeSpec).build()
  }

  /**
   * Creates a JavaFile representing the generated Hilt_${CLASS}EntryPoint.
   *
   * For a class "SpecificHost", the JavaFile contains the following:
   * ```java
   * @OriginatingElement(topLevelClass = SpecificHost.class)
   * @Generated("ComposeComponentHostGenerator")
   * @GeneratedEntryPoint
   * @InstallIn(ComposeComponent.class)
   * interface Hilt_SpecificHostEntryPoint {
   *   SpecificHost getSpecificHost();
   * }
   * ```
   */
  private fun componentHostEntryPointJavaFile(): JavaFile {
    val typeSpec =
      TypeSpec.interfaceBuilder(metadata.hostEntryPointClassName)
        .addGeneratedCodeAnnotations(metadata.element, env)
        .addAnnotation(Processors.getOriginatingElementAnnotation(metadata.element))
        .addAnnotation(ClassNames.GENERATED_ENTRY_POINT)
        .addAnnotation(
          AnnotationSpec.builder(ClassNames.INSTALL_IN)
            .addMember("value", "\$T.class", COMPOSE_COMPONENT_CLASS)
            .build()
        )
        .addMethod(
          MethodSpec.methodBuilder("get" + metadata.elementClassName.simpleName())
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(metadata.elementClassName)
            .build()
        )
        .build()

    return JavaFile.builder(metadata.hostEntryPointClassName.packageName(), typeSpec).build()
  }

  /**
   * Creates a JavaFile representing the generated Hilt_${CLASS}ComponentHostCreator.
   *
   * For a class "SpecificHost", the JavaFile contains the following:
   * ```java
   * @Generated("ComposeComponentHostGenerator")
   * final class Hilt_SpecificHostComponentHostCreator
   *     implements ComponentHostCreator<SpecificHost>, InternalComponentHostCreator<SpecificHost> {
   *   private final ComposeComponentBuilder composeComponentBuilder;
   *   private final ComposeRetainedComponentBuilder composeRetainedComponentBuilder;
   *
   *   @Inject
   *   Hilt_SpecificHostComponentHostCreator(
   *       ComposeComponentBuilder componentBuilder,
   *       ComposeRetainedComponentBuilder composeRetainedComponentBuilder,
   *       RetainedViewModelManager retainedViewModelManager) {
   *     this.composeComponentBuilder = composeComponentBuilder;
   *     this.composeRetainedComponentBuilder = composeRetainedComponentBuilder;
   *     retainedViewModelManager.addAsObserver()
   *   }
   *
   *   @Override
   *   public SpecificHost createComponentHost(
   *       @Nullable ComposeComponentHostExtras extras
   *       ComposeRetainedComponent composeRetainedComponent) {
   *     ComposeComponent composeComponent =
   *       composeComponentBuilder
   *         .bindComposeComponentHostExtras(extras)
   *         .bindComposeRetainedComponent(composeRetainedComponent)
   *         .build();
   *     return ((Hilt_SpecificHostEntryPoint) composeComponent).getSpecificHost();
   *   }
   *
   *   @Override
   *   public ComposeRetainedComponent createRetainedComponent(
   *       @Nullable ComposeComponentHostExtras extras) {
   *     return composeRetainedComponentBuilder
   *       .bindComposeComponentHostExtras(extras)
   *       .build();
   *   }
   * }
   * ```
   */
  private fun componentHostCreatorJavaFile(): JavaFile {
    val composeComponentBuilder =
      ParameterSpec.builder(COMPOSE_COMPONENT_BUILDER_CLASS, "composeComponentBuilder").build()
    val composeRetainedComponentBuilder =
      ParameterSpec.builder(
          COMPOSE_RETAINED_COMPONENT_BUILDER_CLASS,
          "composeRetainedComponentBuilder"
        )
        .build()

    val typeSpec =
      TypeSpec.classBuilder(metadata.creatorClassName)
        .addGeneratedCodeAnnotations(metadata.element, env)
        .addModifiers(Modifier.FINAL)
        .addSuperinterface(metadata.componentHostCreatorInterfaceTypeName)
        .addSuperinterface(metadata.internalComponentHostCreatorInterfaceTypeName)
        .addField(
          composeComponentBuilder.type,
          composeComponentBuilder.name,
          Modifier.PRIVATE,
          Modifier.FINAL
        )
        .addField(
          composeRetainedComponentBuilder.type,
          composeRetainedComponentBuilder.name,
          Modifier.PRIVATE,
          Modifier.FINAL
        )
        .addMethod(
          componentHostConstructor(composeComponentBuilder, composeRetainedComponentBuilder)
        )
        .addMethod(createComponentHost(composeComponentBuilder.name))
        .addMethod(createRetainedComponent(composeRetainedComponentBuilder.name))
        .build()

    return JavaFile.builder(metadata.creatorClassName.packageName(), typeSpec).build()
  }

  /**
   * ```java
   *   @Inject
   *   Hilt_SpecificHostComponentHostCreator(
   *       ComposeComponentBuilder componentBuilder,
   *       ComposeRetainedComponentBuilder composeRetainedComponentBuilder,
   *       RetainedViewModelManager retainedViewModelManager) {
   *     this.composeComponentBuilder = composeComponentBuilder;
   *     this.composeRetainedComponentBuilder = composeRetainedComponentBuilder;
   *     retainedViewModelManager.addAsObserver()
   *   }
   * ```
   */
  private fun componentHostConstructor(
    composeComponentBuilderParam: ParameterSpec,
    composeRetainedComponentBuilderParam: ParameterSpec,
  ): MethodSpec {
    val retainedViewModelManagerParam =
      ParameterSpec.builder(RETAINED_VIEW_MODEL_MANAGER_CLASS, "retainedViewModelManager").build()

    return MethodSpec.constructorBuilder()
      .addAnnotation(ClassNames.INJECT)
      .addParameter(composeComponentBuilderParam)
      .addParameter(composeRetainedComponentBuilderParam)
      .addParameter(retainedViewModelManagerParam)
      .addStatement(
        "this.\$L = \$L",
        composeComponentBuilderParam.name,
        composeComponentBuilderParam.name
      )
      .addStatement(
        "this.\$L = \$L",
        composeRetainedComponentBuilderParam.name,
        composeRetainedComponentBuilderParam.name
      )
      .addStatement("\$L.addAsObserver()", retainedViewModelManagerParam.name)
      .build()
  }

  /**
   * ```java
   * @Override
   * public SpecificHost createComponentHost(
   *     @Nullable ComposeComponentHostExtras extras
   *     ComposeRetainedComponent composeRetainedComponent) {
   *   ComposeComponent composeComponent =
   *     composeComponentBuilder
   *       .bindComposeComponentHostExtras(extras)
   *       .bindComposeRetainedComponent(composeRetainedComponent)
   *       .build();
   *   return ((Hilt_SpecificEntryPoint) composeComponent).getSpecificHost()
   * }
   * ```
   */
  private fun createComponentHost(composeComponentBuilderName: String): MethodSpec {
    val composeRetainedComponentParam =
      ParameterSpec.builder(COMPOSE_RETAINED_COMPONENT_CLASS, "composeRetainedComponent").build()

    val composeComponentHostExtrasParam =
      ParameterSpec.builder(COMPOSE_COMPONENT_HOST_EXTRAS, "extras")
        .addAnnotation(AndroidClassNames.NULLABLE)
        .build()

    return MethodSpec.methodBuilder("createComponentHost")
      .addAnnotation(Override::class.java)
      .addModifiers(Modifier.PUBLIC)
      .returns(metadata.element.type.typeName)
      .addParameter(composeComponentHostExtrasParam)
      .addParameter(composeRetainedComponentParam)
      .addStatement(
        "\$T composeComponent = \$L" +
          ".bindComposeComponentHostExtras(\$L)" +
          ".bindComposeRetainedComponent(\$L)" +
          ".build()",
        COMPOSE_COMPONENT_CLASS,
        composeComponentBuilderName,
        composeComponentHostExtrasParam.name,
        composeRetainedComponentParam.name,
      )
      .addStatement(
        "return ((\$T) composeComponent).get\$L()",
        metadata.hostEntryPointClassName,
        metadata.elementClassName.simpleName()
      )
      .build()
  }

  /**
   * ```java
   * @Override
   * public ComposeRetainedComponent createRetainedComponent(
   *     @Nullable ComposeComponentHostExtras extras) {
   *   return composeRetainedComponentBuilder
   *     .bindComposeComponentHostExtras(extras)
   *     .build();
   * }
   * ```
   */
  private fun createRetainedComponent(composeRetainedComponentBuilderName: String): MethodSpec {
    val composeComponentHostExtrasParam =
      ParameterSpec.builder(COMPOSE_COMPONENT_HOST_EXTRAS, "extras")
        .addAnnotation(AndroidClassNames.NULLABLE)
        .build()

    return MethodSpec.methodBuilder("createRetainedComponent")
      .addAnnotation(Override::class.java)
      .addModifiers(Modifier.PUBLIC)
      .returns(COMPOSE_RETAINED_COMPONENT_CLASS)
      .addParameter(composeComponentHostExtrasParam)
      .addStatement(
        "return \$L.bindComposeComponentHostExtras(\$L).build()",
        composeRetainedComponentBuilderName,
        composeComponentHostExtrasParam.name
      )
      .build()
  }

  /**
   * Creates a JavaFile representing the generated Hilt_${CLASS}Module.
   *
   * For a class "SpecificHost", the JavaFile contains the following:
   * ```java
   * @Generated("ComposeComponentHostGenerator")
   * @OriginatingElement(topLevelClass = SpecificHost.class)
   * @Module
   * @InstallIn(ActivityComponent.class)
   * abstract class Hilt_SpecificHostCreatorModule {
   *   @Binds
   *   abstract ComponentHostCreator<SpecificHost> bindComponentHostCreator(
   *       Hilt_SpecificHostComponentHostCreator impl);
   * }
   * ```
   */
  private fun moduleJavaFile(): JavaFile {
    val typeSpec =
      TypeSpec.classBuilder(metadata.moduleClassName)
        .addGeneratedCodeAnnotations(metadata.element, env)
        .addAnnotation(Processors.getOriginatingElementAnnotation(metadata.element))
        .addAnnotation(ClassNames.MODULE)
        .addAnnotation(
          AnnotationSpec.builder(ClassNames.INSTALL_IN)
            .addMember("value", "\$T.class", AndroidClassNames.ACTIVITY_COMPONENT)
            .build()
        )
        .addModifiers(Modifier.ABSTRACT)
        .addMethod(
          MethodSpec.methodBuilder("bindComponentHostCreator")
            .addAnnotation(ClassNames.BINDS)
            .addModifiers(Modifier.ABSTRACT)
            .returns(metadata.componentHostCreatorInterfaceTypeName)
            .addParameter(ParameterSpec.builder(metadata.creatorClassName, "impl").build())
            .build()
        )
        .build()

    return JavaFile.builder(metadata.moduleClassName.packageName(), typeSpec).build()
  }

  companion object {
    /**
     * ClassName for [dagger.hilt.android.compose.components.ComposeComponent].
     *
     * TODO(b/281594970): Move to appropriate constants file.
     */
    private val COMPOSE_COMPONENT_CLASS: ClassName =
      ClassName.get("dagger.hilt.android.compose.components", "ComposeComponent")

    /**
     * ClassName for [dagger.hilt.android.compose.internal.builders.ComposeComponentBuilder].
     *
     * TODO(b/281594970): Move to appropriate constants file.
     */
    private val COMPOSE_COMPONENT_BUILDER_CLASS: ClassName =
      ClassName.get("dagger.hilt.android.compose.internal.builders", "ComposeComponentBuilder")

    /**
     * ClassName for [dagger.hilt.android.compose.RetainedViewModelManager].
     *
     * TODO(b/281594970): Move to appropriate constants file.
     */
    private val RETAINED_VIEW_MODEL_MANAGER_CLASS: ClassName =
      ClassName.get("dagger.hilt.android.compose", "RetainedViewModelManager")

    /**
     * ClassName for [dagger.hilt.android.compose.ComposeComponentHostExtras].
     *
     * TODO(b/281594970): Move to appropriate constants file.
     */
    private val COMPOSE_COMPONENT_HOST_EXTRAS: ClassName =
      ClassName.get("dagger.hilt.android.compose", "ComposeComponentHostExtras")

    /**
     * ClassName for [dagger.hilt.android.compose.components.ComposeRetainedComponent].
     *
     * TODO(b/281594970): Move to appropriate constants file.
     */
    private val COMPOSE_RETAINED_COMPONENT_CLASS: ClassName =
      ClassName.get("dagger.hilt.android.compose.components", "ComposeRetainedComponent")

    /**
     * ClassName for
     * [dagger.hilt.android.compose.internal.builders.ComposeRetainedComponentBuilder].
     *
     * TODO(b/281594970): Move to appropriate constants file.
     */
    private val COMPOSE_RETAINED_COMPONENT_BUILDER_CLASS: ClassName =
      ClassName.get(
        "dagger.hilt.android.compose.internal.builders",
        "ComposeRetainedComponentBuilder"
      )
  }
}

/**
 * Adds default required code generation annotations to the TypeSpec.Builder.
 *
 * For a class "SpecificHost", the Type will be annotated as follows:
 * ```java
 * @Generated("ComposeComponentHostGenerator")
 * ```
 */
@OptIn(ExperimentalProcessingApi::class)
private fun TypeSpec.Builder.addGeneratedCodeAnnotations(
  originatingElement: XTypeElement,
  env: XProcessingEnv
): TypeSpec.Builder {
  this.addOriginatingElement(originatingElement)
  Processors.addGeneratedAnnotation(this, env, ComposeComponentHostGenerator::class.java)
  return this
}
