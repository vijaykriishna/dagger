/*
 * Copyright (C) 2025 The Dagger Authors.
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

package dagger.internal.codegen.xprocessing;

import static androidx.room.compiler.codegen.compat.XConverters.toJavaPoet;
import static androidx.room.compiler.codegen.compat.XConverters.toKotlinPoet;

import androidx.room.compiler.codegen.XAnnotationSpec;
import androidx.room.compiler.codegen.XClassName;
import androidx.room.compiler.codegen.XParameterSpec;
import androidx.room.compiler.codegen.XTypeName;
import androidx.room.compiler.processing.XExecutableParameterElement;
import androidx.room.compiler.processing.XType;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.kotlinpoet.KModifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.lang.model.element.Modifier;

// TODO(bcorso): Consider moving these methods into XProcessing library.
/** A utility class for {@link XParameterSpec} helper methods. */
public final class XParameterSpecs {
  public static XParameterSpec parameterSpecOf(XExecutableParameterElement parameter) {
    return builder(parameter.getJvmName(), parameter.getType().asTypeName()).build();
  }

  public static XParameterSpec parameterSpecOf(
      XExecutableParameterElement parameter, XType parameterType) {
    Nullability nullability = Nullability.of(parameter);
    return builder(
            parameter.getJvmName(),
            XTypeNames.withTypeNullability(parameterType.asTypeName(), nullability))
        .addAnnotationNames(nullability.nonTypeUseNullableAnnotations())
        .build();
  }

  /**
   * Creates a {@link XParameterSpec} with the given {@code name} and {@code typeName} and adds the
   * type-use nullability annotations to the type and the non-type-use annotations to the parameter.
   */
  public static XParameterSpec of(String name, XTypeName typeName, Nullability nullability) {
    return builder(name, typeName, nullability).build();
  }

  /** Creates a {@link XParameterSpec} with the given {@code name} and {@code typeName}. */
  public static XParameterSpec of(String name, XTypeName typeName) {
    return builder(name, typeName).build();
  }

  /**
   * Creates a {@link Builder} with the given {@code name} and {@code typeName} and adds the
   * type-use nullability annotations to the type and the non-type-use annotations to the parameter.
   */
  public static Builder builder(String name, XTypeName typeName, Nullability nullability) {
    return new Builder(name, XTypeNames.withTypeNullability(typeName, nullability))
        .addAnnotationNames(nullability.nonTypeUseNullableAnnotations());
  }

  /** Creates a {@link Builder} with the given {@code name} and {@code typeName}. */
  public static Builder builder(String name, XTypeName typeName) {
    return new Builder(name, typeName);
  }

  /** Builds an {@link XParameterSpec} in a way that is more similar to the JavaPoet API. */
  public static class Builder {
    private final String name;
    private final XTypeName typeName;
    private Boolean isFinal = null;
    // For now, we use a Object to allow for both XPoet and JavaPoet types.
    private final List<Object> annotations = new ArrayList<>();

    Builder(String name, XTypeName typeName) {
      this.name = name;
      this.typeName = typeName;
    }

    /** Sets the modifiers of the method. */
    @CanIgnoreReturnValue
    public Builder addModifiers(Collection<Modifier> modifiers) {
      return addModifiers(modifiers.toArray(new Modifier[0]));
    }

    /** Sets the modifiers of the method. */
    @CanIgnoreReturnValue
    public Builder addModifiers(Modifier... modifiers) {
      for (Modifier modifier : modifiers) {
        switch (modifier) {
          case FINAL:
            isFinal = true;
            break;
          default:
            throw new AssertionError("Unexpected modifier: " + modifier);
        }
      }
      return this;
    }

    /** Adds the given annotations to the method. */
    @CanIgnoreReturnValue
    public Builder addAnnotations(Collection<XAnnotationSpec> annotations) {
      annotations.forEach(this::addAnnotation);
      return this;
    }

    /**
     * Adds the given annotations to the method.
     *
     * @deprecated Use {@link #addAnnotation(XAnnotationSpec)} instead.
     */
    @Deprecated
    @CanIgnoreReturnValue
    public Builder addJavaAnnotations(Collection<AnnotationSpec> annotations) {
      annotations.forEach(this::addAnnotation);
      return this;
    }

    /** Adds the given annotation names to the method. */
    @CanIgnoreReturnValue
    public Builder addAnnotationNames(Collection<XClassName> annotationNames) {
      annotationNames.forEach(this::addAnnotation);
      return this;
    }

    /** Adds the given annotation to the method. */
    @CanIgnoreReturnValue
    public Builder addAnnotation(XAnnotationSpec annotation) {
      annotations.add(annotation);
      return this;
    }

    /** Adds the given annotation name to the method. */
    @CanIgnoreReturnValue
    public Builder addAnnotation(XClassName annotationName) {
      return addAnnotation(XAnnotationSpec.of(annotationName));
    }

    /**
     * Adds the given annotation to the method.
     *
     * @deprecated Use {@link #addAnnotation(XClassName)} instead.
     */
    @Deprecated
    @CanIgnoreReturnValue
    public Builder addAnnotation(Class<?> clazz) {
      addAnnotation(AnnotationSpec.builder(ClassName.get(clazz)).build());
      return this;
    }

    /**
     * Adds the given annotation to the method.
     *
     * @deprecated Use {@link #addAnnotation(XAnnotationSpec)} instead.
     */
    @Deprecated
    @CanIgnoreReturnValue
    public Builder addAnnotation(AnnotationSpec annotation) {
      annotations.add(annotation);
      return this;
    }

    /** Builds the method and returns an {@link XFunSpec}. */
    public XParameterSpec build() {
      XParameterSpec.Builder builder =
          XParameterSpec.builder(name, typeName, /* addJavaNullabilityAnnotation= */ false);

      // XPoet makes all parameters final by default for both JavaPoet and KotlinPoet so rather than
      // adding the final modifier if it is final, we need to remove the modifier if it's not final.
      if (isFinal == null) {
        // If the final modifier isn't set explicitly, then default to false for JavaPoet but leave
        // it as final for KotlinPoet since that's the default behavior in Kotlin.
        toJavaPoet(builder).modifiers.remove(Modifier.FINAL);
      } else if (!isFinal) {
        toJavaPoet(builder).modifiers.remove(Modifier.FINAL);
        toKotlinPoet(builder).getModifiers().remove(KModifier.FINAL);
      }

      for (Object annotation : annotations) {
        if (annotation instanceof XAnnotationSpec) {
          builder.addAnnotation((XAnnotationSpec) annotation);
        } else if (annotation instanceof AnnotationSpec) {
          toJavaPoet(builder).addAnnotation((AnnotationSpec) annotation);
        } else {
          throw new AssertionError("Unexpected annotation class: " + annotation.getClass());
        }
      }

      return builder.build();
    }
  }

  private XParameterSpecs() {}
}
