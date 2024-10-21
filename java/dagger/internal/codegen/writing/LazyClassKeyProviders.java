/*
 * Copyright (C) 2024 The Dagger Authors.
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

package dagger.internal.codegen.writing;

import static dagger.internal.codegen.base.MapKeyAccessibility.isMapKeyAccessibleFrom;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;

import androidx.room.compiler.processing.XAnnotation;
import com.google.common.base.Preconditions;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import dagger.internal.codegen.base.UniqueNameSet;
import dagger.internal.codegen.javapoet.TypeNames;
import dagger.internal.codegen.model.Key;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;

/**
 * Keeps track of all providers for DaggerMap keys.
 *
 * <p>Generated class looks like below:
 *
 * <pre>{@code
 *  @IdentifierNameString
 *  static final class  LazyClassKeyProvider {
 *    static final String com_google_foo_Bar = "com.google.foo.Bar";
 *    @KeepFieldType static final com.google.foo.Bar com_google_foo_Bar2;
 * }
 * }</pre>
 */
public final class LazyClassKeyProviders {
  @PerGeneratedFile
  static final class LazyClassKeyProviderCache {
    // Map key to its corresponding the field reference expression from LazyClassKeyProvider.
    final Map<ClassName, CodeBlock> mapKeyToProvider = new LinkedHashMap<>();
    private final Map<ClassName, FieldSpec> entries = new LinkedHashMap<>();
    private final Map<ClassName, FieldSpec> keepClassNamesFields = new LinkedHashMap<>();
    private final UniqueNameSet uniqueFieldNames = new UniqueNameSet();
    private ClassName mapKeyProviderType;

    @Inject
    LazyClassKeyProviderCache() {}

    private CodeBlock getMapKeyExpression(Key key) {
      Preconditions.checkArgument(
          key.multibindingContributionIdentifier().isPresent()
              && key.multibindingContributionIdentifier()
                  .get()
                  .bindingMethod()
                  .xprocessing()
                  .hasAnnotation(TypeNames.LAZY_CLASS_KEY));
      XAnnotation lazyClassKeyAnnotation = getLazyClassKeyAnnotation(key);
      ClassName lazyClassKey = getLazyClassKey(lazyClassKeyAnnotation);
      if (mapKeyToProvider.containsKey(lazyClassKey)) {
        return mapKeyToProvider.get(lazyClassKey);
      }
      addField(lazyClassKey, lazyClassKeyAnnotation, mapKeyProviderType.packageName());
      mapKeyToProvider.put(
          lazyClassKey, CodeBlock.of("$T.$N", mapKeyProviderType, entries.get(lazyClassKey)));
      return mapKeyToProvider.get(lazyClassKey);
    }

    private ClassName getLazyClassKey(XAnnotation lazyClassKeyAnnotation) {
      return lazyClassKeyAnnotation.getAsType("value").getTypeElement().getClassName();
    }

    private XAnnotation getLazyClassKeyAnnotation(Key key) {
      return key.multibindingContributionIdentifier()
          .get()
          .bindingMethod()
          .xprocessing()
          .getAnnotation(TypeNames.LAZY_CLASS_KEY);
    }

    private void addField(
        ClassName lazyClassKey, XAnnotation lazyClassKeyAnnotation, String accessingPackage) {
      entries.put(
          lazyClassKey,
          FieldSpec.builder(
                  TypeNames.STRING,
                  uniqueFieldNames.getUniqueName(lazyClassKey.canonicalName().replace('.', '_')))
              // TODO(b/217435141): Leave the field as non-final. We will apply
              // @IdentifierNameString on the field, which doesn't work well with static final
              // fields.
              .addModifiers(STATIC)
              .initializer("$S", lazyClassKey.reflectionName())
              .build());
      // To be able to apply -includedescriptorclasses rule to keep the class names referenced by
      // LazyClassKey, we need to generate fields that uses those classes as type in
      // LazyClassKeyProvider. For types that are not accessible from the generated component, we
      // generate fields in the proxy class.
      // Note: the generated field should not be initialized to avoid class loading.
      if (isMapKeyAccessibleFrom(lazyClassKeyAnnotation, accessingPackage)) {
        keepClassNamesFields.put(
            lazyClassKey,
            FieldSpec.builder(
                    lazyClassKey,
                    uniqueFieldNames.getUniqueName(lazyClassKey.canonicalName().replace('.', '_')))
                .addAnnotation(TypeNames.KEEP_FIELD_TYPE)
                .build());
      }
    }

    private TypeSpec build() {
      return TypeSpec.classBuilder(mapKeyProviderType)
          .addAnnotation(TypeNames.IDENTIFIER_NAME_STRING)
          .addModifiers(PRIVATE, STATIC, FINAL)
          .addFields(entries.values())
          .addFields(keepClassNamesFields.values())
          .build();
    }
  }

  public static final String MAP_KEY_PROVIDER_NAME = "LazyClassKeyProvider";
  private final GeneratedImplementation topLevelImplementation;
  private final LazyClassKeyProviderCache cache;

  @Inject
  LazyClassKeyProviders(
      @TopLevel GeneratedImplementation topLevelImplementation, LazyClassKeyProviderCache cache) {
    this.topLevelImplementation = topLevelImplementation;
    this.cache = cache;
  }

  /** Returns a reference to a field in LazyClassKeyProvider that corresponds to this binding. */
  CodeBlock getMapKeyExpression(Key key) {
    // This is for avoid generating empty LazyClassKeyProvider.
    if (cache.entries.isEmpty()) {
      String name = topLevelImplementation.getUniqueClassName(MAP_KEY_PROVIDER_NAME);
      cache.mapKeyProviderType = topLevelImplementation.name().nestedClass(name);
      topLevelImplementation.addTypeSupplier(this::build);
    }
    return cache.getMapKeyExpression(key);
  }

  private TypeSpec build() {
    return cache.build();
  }
}
