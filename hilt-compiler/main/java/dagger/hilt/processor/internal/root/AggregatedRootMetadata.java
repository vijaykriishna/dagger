/*
 * Copyright (C) 2021 The Dagger Authors.
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

package dagger.hilt.processor.internal.root;

import static dagger.internal.codegen.extension.DaggerStreams.toImmutableSet;

import androidx.room3.compiler.processing.XAnnotation;
import androidx.room3.compiler.processing.XProcessingEnv;
import androidx.room3.compiler.processing.XTypeElement;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import dagger.hilt.processor.internal.AggregatedElements;
import dagger.hilt.processor.internal.ClassNames;
import dagger.hilt.processor.internal.root.ir.AggregatedRootIr;
import java.util.List;

/**
 * Represents the values stored in an {@link dagger.hilt.internal.aggregatedroot.AggregatedRoot}.
 */
@AutoValue
abstract class AggregatedRootMetadata {

  /** Returns the aggregating element */
  public abstract XTypeElement aggregatingElement();

  /** Returns the element that was annotated with the root annotation. */
  abstract XTypeElement rootElement();

  /**
   * Returns the originating root element. In most cases this will be the same as {@link
   * #rootElement()}.
   */
  abstract XTypeElement originatingRootElement();

  /** Returns the root annotation as an element. */
  abstract XTypeElement rootAnnotation();

  /** Returns the name of the root component for this root. */
  abstract ClassName rootComponentName();

  /** Returns whether this root can use a shared component. */
  abstract boolean allowsSharingComponent();

  @Memoized
  RootType rootType() {
    return RootType.of(rootElement());
  }

  static ImmutableSet<AggregatedRootMetadata> from(XProcessingEnv env) {
    return from(
        AggregatedElements.from(
            ClassNames.AGGREGATED_ROOT_PACKAGE, ClassNames.AGGREGATED_ROOT, env),
        env);
  }

  /** Returns metadata for each aggregated element. */
  public static ImmutableSet<AggregatedRootMetadata> from(
      ImmutableSet<XTypeElement> aggregatedElements, XProcessingEnv env) {
    return aggregatedElements.stream()
        .map(aggregatedElement -> create(aggregatedElement, env))
        .collect(toImmutableSet());
  }

  public static AggregatedRootIr toIr(AggregatedRootMetadata metadata) {
    return new AggregatedRootIr(
        metadata.aggregatingElement().getClassName(),
        metadata.rootElement().getClassName(),
        metadata.originatingRootElement().getClassName(),
        metadata.rootAnnotation().getClassName(),
        metadata.rootComponentName(),
        metadata.allowsSharingComponent());
  }

  private static AggregatedRootMetadata create(XTypeElement element, XProcessingEnv env) {
    XAnnotation annotation = element.getAnnotation(ClassNames.AGGREGATED_ROOT);

    XTypeElement rootElement = env.requireTypeElement(annotation.getAsString("root"));
    boolean allowSharingComponent = true;
    return new AutoValue_AggregatedRootMetadata(
        element,
        rootElement,
        env.requireTypeElement(annotation.getAsString("originatingRoot")),
        annotation.getAsType("rootAnnotation").getTypeElement(),
        parseRootComponentClassName(
            annotation.getAsString("rootComponentPackage"),
            annotation.getAsStringList("rootComponentSimpleNames")),
        allowSharingComponent);
  }

  private static ClassName parseRootComponentClassName(
      String rootComponentPackage, List<String> rootComponentSimpleNames) {
    // If rootComponentPackage isn't there, that means that this is likely coming from an older
    // Dagger version, so assume the root component is the SingletonComponent for backwards
    // compatibility.
    if (rootComponentPackage.isEmpty()) {
      return ClassNames.SINGLETON_COMPONENT;
    }
    return ClassName.get(
        rootComponentPackage,
        rootComponentSimpleNames.get(0),
        rootComponentSimpleNames.subList(1, rootComponentSimpleNames.size())
            .toArray(new String[0]));
  }
}
