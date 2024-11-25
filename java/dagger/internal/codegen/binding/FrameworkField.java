/*
 * Copyright (C) 2014 The Dagger Authors.
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

package dagger.internal.codegen.binding;

import static androidx.room.compiler.processing.XElementKt.isConstructor;
import static androidx.room.compiler.processing.XElementKt.isMethod;
import static androidx.room.compiler.processing.XElementKt.isMethodParameter;
import static androidx.room.compiler.processing.XElementKt.isTypeElement;
import static dagger.internal.codegen.model.BindingKind.MEMBERS_INJECTOR;
import static dagger.internal.codegen.xprocessing.XElements.getSimpleName;

import androidx.room.compiler.processing.XElement;
import androidx.room.compiler.processing.XType;
import com.google.auto.value.AutoValue;
import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import dagger.internal.codegen.base.MapType;
import java.util.Optional;

/**
 * A value object that represents a field in the generated Component class.
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>{@code Provider<String>}
 *   <li>{@code Producer<Widget>}
 *   <li>{@code Provider<Map<SomeMapKey, MapValue>>}.
 * </ul>
 */
@AutoValue
public abstract class FrameworkField {

  /**
   * Creates a framework field.
   *
   * @param fieldName the base name of the field. The name of the raw type of the field will be
   *     added as a suffix
   * @param frameworkClassName the framework class that wraps the type (e.g., {@code Provider}).
   * @param type the base type of the field (e.g., {@code Foo}).
   */
  public static FrameworkField create(String fieldName, ClassName frameworkClassName, XType type) {
    return createInternal(fieldName, frameworkClassName, Optional.of(type));
  }

  /**
   * A framework field for a {@link ContributionBinding}.
   *
   * @param frameworkClass if present, the field will use this framework class instead of the normal
   *     one for the binding's type.
   */
  public static FrameworkField forBinding(
      ContributionBinding binding, Optional<ClassName> frameworkClassName) {
    return createInternal(
        bindingName(binding),
        frameworkClassName.orElse(binding.frameworkType().frameworkClassName()),
        bindingType(binding));
  }

  private static String bindingName(ContributionBinding binding) {
    if (binding.bindingElement().isPresent()) {
      String name = bindingElementName(binding.bindingElement().get());
      return binding.kind().equals(MEMBERS_INJECTOR) ? name + "MembersInjector" : name;
    }
    return KeyVariableNamer.name(binding.key());
  }

  private static Optional<XType> bindingType(ContributionBinding binding) {
    if (binding.contributionType().isMultibinding()) {
      return Optional.of(binding.contributedType());
    }

    // If the binding key type is a Map<K, Provider<V>>, we need to change field type to a raw
    // type. This is because it actually needs to be changed to Map<K, dagger.internal.Provider<V>>,
    // but that gets into assignment issues when the field is passed to methods that expect
    // Map<K, javax.inject.Provider<V>>. We could add casts everywhere, but it is easier to just
    // make the field itself a raw type.
    if (MapType.isMapOfProvider(binding.contributedType())) {
      return Optional.empty();
    }

    return Optional.of(binding.key().type().xprocessing());
  }

  private static FrameworkField createInternal(
      String fieldName, ClassName frameworkClassName, Optional<XType> type) {
    return new AutoValue_FrameworkField(
        frameworkFieldName(fieldName, frameworkClassName),
        type.isPresent()
            ? ParameterizedTypeName.get(frameworkClassName, type.get().getTypeName())
            // Use a raw framework classname, e.g. Provider
            : frameworkClassName);
  }

  private static String frameworkFieldName(String fieldName, ClassName frameworkClassName) {
    String suffix = frameworkClassName.simpleName();
    return fieldName.endsWith(suffix) ? fieldName : fieldName + suffix;
  }

  private static String bindingElementName(XElement bindingElement) {
    if (isConstructor(bindingElement)) {
      return bindingElementName(bindingElement.getEnclosingElement());
    } else if (isMethod(bindingElement)) {
      return getSimpleName(bindingElement);
    } else if (isTypeElement(bindingElement)) {
      return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, getSimpleName(bindingElement));
    } else if (isMethodParameter(bindingElement)) {
      return getSimpleName(bindingElement);
    } else {
      throw new IllegalArgumentException("Unexpected binding " + bindingElement);
    }
  }

  public abstract String name();

  public abstract TypeName type();
}
