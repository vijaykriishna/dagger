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
import static androidx.room.compiler.codegen.compat.XConverters.toXPoet;
import static com.google.common.base.Preconditions.checkState;
import static dagger.internal.codegen.xprocessing.XCodeBlocks.toXPoet;

import androidx.room.compiler.codegen.VisibilityModifier;
import androidx.room.compiler.codegen.XAnnotationSpec;
import androidx.room.compiler.codegen.XClassName;
import androidx.room.compiler.codegen.XCodeBlock;
import androidx.room.compiler.codegen.XFunSpec;
import androidx.room.compiler.codegen.XPropertySpec;
import androidx.room.compiler.codegen.XTypeName;
import androidx.room.compiler.codegen.XTypeSpec;
import androidx.room.compiler.codegen.compat.XConverters;
import androidx.room.compiler.processing.XElement;
import androidx.room.compiler.processing.XType;
import androidx.room.compiler.processing.XTypeElement;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.kotlinpoet.KModifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;

/** Utility methods for building {@link XTypeSpec}s. */
public final class XTypeSpecs {
  public static Builder classBuilder(String name) {
    return new Builder(Builder.Kind.CLASS).name(name);
  }

  public static Builder classBuilder(XClassName className) {
    return new Builder(Builder.Kind.CLASS).name(className.getSimpleName());
  }

  public static Builder interfaceBuilder(String name) {
    return new Builder(Builder.Kind.INTERFACE).name(name);
  }

  public static Builder interfaceBuilder(XClassName className) {
    return new Builder(Builder.Kind.INTERFACE).name(className.getSimpleName());
  }

  public static Builder objectBuilder(String name) {
    return new Builder(Builder.Kind.OBJECT).name(name);
  }

  public static Builder anonymousClassBuilder() {
    return new Builder(Builder.Kind.ANONYMOUS_CLASS);
  }

  public static Builder annotationBuilder(String name) {
    return new Builder(Builder.Kind.ANNOTATION).name(name);
  }

  public static XTypeSpec.Builder toBuilder(XTypeSpec typeSpec) {
    return toXPoet(
        toJavaPoet(typeSpec).toBuilder(),
        toKotlinPoet(typeSpec).toBuilder());
  }

  /** Builds an {@link XTypeSpec} in a way that is more similar to the JavaPoet API. */
  public static class Builder {
    private static enum Kind {
      CLASS,
      INTERFACE,
      ANNOTATION,
      OBJECT,
      ANONYMOUS_CLASS
    }

    private final Kind kind;
    private String name;
    private boolean isOpen;
    private boolean isStatic;
    private boolean isAbstract;
    private VisibilityModifier visibility = null;
    private XElement originatingElement;
    private final Set<String> alwaysQualifyNames = new LinkedHashSet<>();
    private final List<XCodeBlock> javadocs = new ArrayList<>();
    // For migration purposes, use a Object to allow for both XPoet and JavaPoet types.
    private Object superclass;
    private final List<Object> superinterfaces = new ArrayList<>();
    private final List<Object> typeVariableNames = new ArrayList<>();
    private final List<Object> annotations = new ArrayList<>();
    private final List<Object> types = new ArrayList<>();
    private final List<Object> properties = new ArrayList<>();
    private final List<Object> functions = new ArrayList<>();

    private Builder(Kind kind) {
      this.kind = kind;
    }

    @CanIgnoreReturnValue
    private Builder name(String name) {
      this.name = name;
      return this;
    }

    /** Sets the static modifier on the type. */
    @CanIgnoreReturnValue
    public Builder isStatic(boolean isStatic) {
      this.isStatic = isStatic;
      return this;
    }

    /** Sets the final/open modifier on the type. */
    @CanIgnoreReturnValue
    public Builder isOpen(boolean isOpen) {
      this.isOpen = isOpen;
      return this;
    }

    /** Sets the abstract modifier on the type. */
    @CanIgnoreReturnValue
    public Builder isAbstract(boolean isAbstract) {
      this.isAbstract = isAbstract;
      return this;
    }

    /** Sets the visibility of the type. */
    @CanIgnoreReturnValue
    public Builder visibility(VisibilityModifier visibility) {
      this.visibility = visibility;
      return this;
    }

    /** Sets the originating element of the type. */
    @CanIgnoreReturnValue
    public Builder addJavadoc(String format, Object... args) {
      javadocs.add(toXPoet(CodeBlock.of(format, args)));
      return this;
    }

    /** Sets the originating element of the type. */
    @CanIgnoreReturnValue
    public Builder addOriginatingElement(XElement originatingElement) {
      this.originatingElement = originatingElement;
      return this;
    }

    /** Sets the super class/interface of the type and handles nested class name clashes. */
    @CanIgnoreReturnValue
    public Builder superType(XTypeElement superType) {
      if (superType.isClass()) {
        return avoidClashesWithNestedClasses(superType).superclass(superType.asClassName());
      } else if (superType.isInterface()) {
        return avoidClashesWithNestedClasses(superType).addSuperinterface(superType.asClassName());
      }
      throw new AssertionError(superType + " is neither a class nor an interface.");
    }

    /**
     * Configures the given {@link XTypeSpec.Builder} so that it fully qualifies all classes nested
     * in the given {@link XTypeElement} and all classes nested within any super type of the given
     * {@link XTypeElement}.
     */
    @CanIgnoreReturnValue
    public Builder avoidClashesWithNestedClasses(XTypeElement typeElement) {
      typeElement.getEnclosedTypeElements().stream()
          .map(XElements::getSimpleName)
          .forEach(alwaysQualifyNames::add);

      typeElement.getType().getSuperTypes().stream()
          .filter(XTypes::isDeclared)
          .map(XType::getTypeElement)
          .forEach(this::avoidClashesWithNestedClasses);

      return this;
    }

    /** Adds the name to the set of always-qualified names to avoid clashes. */
    @CanIgnoreReturnValue
    public Builder alwaysQualify(String name) {
      alwaysQualifyNames.add(name);
      return this;
    }

    /** Sets the super class of the type. */
    @CanIgnoreReturnValue
    public Builder superclass(XTypeName superclass) {
      this.superclass = superclass;
      return this;
    }

    /**
     * Sets the super class of the type.
     *
     * <p>@deprecated Use {@link #superclass(XTypeName)} instead.
     */
    @Deprecated
    @CanIgnoreReturnValue
    public Builder superclass(TypeName superclass) {
      this.superclass = superclass;
      return this;
    }

    /** Adds the super interfaces to the type. */
    @CanIgnoreReturnValue
    public Builder addSuperinterfaces(Collection<XTypeName> superinterfaces) {
      superinterfaces.forEach(this::addSuperinterface);
      return this;
    }

    /**
     * Adds the super interfaces to the type.
     *
     * <p>@deprecated Use {@link #addSuperinterfaces(Collection<XTypeName>)} instead.
     */
    @Deprecated
    @CanIgnoreReturnValue
    public Builder addJavaSuperinterfaces(Collection<TypeName> superinterfaces) {
      superinterfaces.forEach(this::addSuperinterface);
      return this;
    }

    /** Adds the super interface to the type. */
    @CanIgnoreReturnValue
    public Builder addSuperinterface(XTypeName superInterface) {
      this.superinterfaces.add(superInterface);
      return this;
    }

    /**
     * Adds the super interface to the type.
     *
     * <p>@deprecated Use {@link #addSuperinterface(XTypeName)} instead.
     */
    @Deprecated
    @CanIgnoreReturnValue
    public Builder addSuperinterface(TypeName superInterface) {
      this.superinterfaces.add(superInterface);
      return this;
    }

    /**
     * Sets the modifiers of the type.
     *
     * <p>@deprecated Use the individual setter methods instead.
     */
    @Deprecated
    @CanIgnoreReturnValue
    public Builder addModifiers(Modifier... modifiers) {
      for (Modifier modifier : modifiers) {
        switch (modifier) {
          case PUBLIC:
            visibility(VisibilityModifier.PUBLIC);
            break;
          case PRIVATE:
            visibility(VisibilityModifier.PRIVATE);
            break;
          case PROTECTED:
            visibility(VisibilityModifier.PROTECTED);
            break;
          case ABSTRACT:
            isOpen(true);
            isAbstract(true);
            break;
          case STATIC:
            isStatic(true);
            break;
          case FINAL:
            isOpen(false);
            break;
          default:
            throw new AssertionError("Unexpected modifier: " + modifier);
        }
      }
      return this;
    }

    /** Adds the given type variables to the type. */
    @CanIgnoreReturnValue
    public Builder addTypeVariables(Collection<? extends XType> typeVariables) {
      typeVariables.forEach(this::addTypeVariable);
      return this;
    }

    /** Adds the given type variable names to the type. */
    @CanIgnoreReturnValue
    public Builder addTypeVariableNames(Collection<XTypeName> typeVariableNames) {
      typeVariableNames.forEach(this::addTypeVariable);
      return this;
    }

    /**
     * Adds the given type variable names to the type.
     *
     * @deprecated Use {@link #addTypeVariableNames(Collection<XTypeName>)} instead.
     */
    @Deprecated
    @CanIgnoreReturnValue
    public Builder addJavaTypeVariableNames(Collection<TypeVariableName> typeVariableNames) {
      typeVariableNames.forEach(this::addTypeVariable);
      return this;
    }

    /** Adds the given type variable to the type. */
    @CanIgnoreReturnValue
    public Builder addTypeVariable(XType type) {
      return addTypeVariable(type.asTypeName());
    }

    /** Adds the given type variable name to the type. */
    @CanIgnoreReturnValue
    public Builder addTypeVariable(XTypeName typeName) {
      typeVariableNames.add(typeName);
      return this;
    }

    /**
     * Adds the given type variable name to the type.
     *
     * <p>@deprecated Use {@link #addTypeVariable(XTypeName)} instead.
     */
    @Deprecated
    @CanIgnoreReturnValue
    public Builder addTypeVariable(TypeVariableName typeVariableName) {
      typeVariableNames.add(typeVariableName);
      return this;
    }

    /** Adds the given annotations to the type. */
    @CanIgnoreReturnValue
    public Builder addAnnotations(Collection<XAnnotationSpec> annotations) {
      annotations.forEach(this::addAnnotation);
      return this;
    }

    /**
     * Adds the given annotations to the type.
     *
     * @deprecated Use {@link #addAnnotation(XAnnotationSpec)} instead.
     */
    @Deprecated
    @CanIgnoreReturnValue
    public Builder addJavaAnnotations(Collection<AnnotationSpec> annotations) {
      annotations.forEach(this::addAnnotation);
      return this;
    }

    /** Adds the given annotation names to the type. */
    @CanIgnoreReturnValue
    public Builder addAnnotationNames(Collection<XClassName> annotationNames) {
      annotationNames.forEach(this::addAnnotation);
      return this;
    }

    /** Adds the given annotation to the type. */
    @CanIgnoreReturnValue
    public Builder addAnnotation(XAnnotationSpec annotation) {
      annotations.add(annotation);
      return this;
    }

    /** Adds the given annotation name to the type. */
    @CanIgnoreReturnValue
    public Builder addAnnotation(XClassName annotationName) {
      return addAnnotation(XAnnotationSpec.of(annotationName));
    }

    /**
     * Adds the given annotation to the type.
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
     * Adds the given annotation to the type.
     *
     * @deprecated Use {@link #addAnnotation(XAnnotationSpec)} instead.
     */
    @Deprecated
    @CanIgnoreReturnValue
    public Builder addAnnotation(AnnotationSpec annotation) {
      annotations.add(annotation);
      return this;
    }

    /** Adds the given types to the type. */
    @CanIgnoreReturnValue
    public Builder addTypes(Collection<XTypeSpec> types) {
      types.forEach(this::addType);
      return this;
    }

    /**
     * Adds the given types to the type.
     *
     * @deprecated Use {@link #addTypes(Collection<XTypeSpec>)} instead.
     */
    @Deprecated
    @CanIgnoreReturnValue
    public Builder addJavaTypes(Collection<TypeSpec> javaTypes) {
      javaTypes.forEach(this::addType);
      return this;
    }

    /** Adds the given annotation name to the type. */
    @CanIgnoreReturnValue
    public Builder addType(XTypeSpec type) {
      types.add(type);
      return this;
    }

    /**
     * Adds the given annotation to the type.
     *
     * @deprecated Use {@link #addAnnotation(XAnnotationSpec)} instead.
     */
    @Deprecated
    @CanIgnoreReturnValue
    public Builder addType(TypeSpec type) {
      types.add(type);
      return this;
    }

    /** Adds the given properties to the type. */
    @CanIgnoreReturnValue
    public Builder addProperties(Collection<XPropertySpec> properties) {
      properties.forEach(this::addProperty);
      return this;
    }

    /**
     * Adds the given fields to the type.
     *
     * @deprecated Use {@link #addProperties(Collection<XPropertySpec>)} instead.
     */
    @Deprecated
    @CanIgnoreReturnValue
    public Builder addFields(Collection<FieldSpec> fields) {
      fields.forEach(this::addField);
      return this;
    }

    /** Adds the given property to the type. */
    @CanIgnoreReturnValue
    public Builder addProperty(XPropertySpec property) {
      properties.add(property);
      return this;
    }

    /**
     * Adds the given property to the type.
     *
     * @deprecated Use {@link #addProperty(XPropertySpec)} instead.
     */
    @Deprecated
    @CanIgnoreReturnValue
    public Builder addField(FieldSpec field) {
      properties.add(field);
      return this;
    }

    /**
     * Adds the given field to the type.
     *
     * @deprecated Use {@link #addProperty(XPropertySpec)} instead.
     */
    @Deprecated
    @CanIgnoreReturnValue
    public Builder addField(TypeName type, String name, Modifier... modifiers) {
      properties.add(FieldSpec.builder(type, name, modifiers).build());
      return this;
    }

    /** Adds the given functions to the type. */
    @CanIgnoreReturnValue
    public Builder addFunctions(Collection<XFunSpec> functions) {
      functions.forEach(this::addFunction);
      return this;
    }

    /** Adds the given function to the type. */
    @CanIgnoreReturnValue
    public Builder addFunction(XFunSpec function) {
      functions.add(function);
      return this;
    }

    /**
     * Adds the given methods to the type.
     *
     * @deprecated Use {@link #addFunctions(Collection<XFunSpec>)} instead.
     */
    @Deprecated
    @CanIgnoreReturnValue
    public Builder addMethods(Collection<MethodSpec> methods) {
      methods.forEach(this::addMethod);
      return this;
    }

    /**
     * Adds the given method to the type.
     *
     * @deprecated Use {@link #addFunction(XFunSpec)} instead.
     */
    @Deprecated
    @CanIgnoreReturnValue
    public Builder addMethod(MethodSpec method) {
      functions.add(method);
      return this;
    }

    /** Builds the type and returns an {@link XTypeSpec}. */
    public XTypeSpec build() {
      XTypeSpec.Builder builder;
      switch (kind) {
        case CLASS:
          builder = XTypeSpec.classBuilder(name, isOpen);
          break;
        case INTERFACE:
          // TODO(bcorso): Add support for interfaces in XPoet.
          builder = XConverters.toXPoet(
              com.squareup.javapoet.TypeSpec.interfaceBuilder(name),
              com.squareup.kotlinpoet.TypeSpec.interfaceBuilder(name));
          if (isOpen) {
            toKotlinPoet(builder).addModifiers(KModifier.OPEN);
          } else {
            toJavaPoet(builder).addModifiers(Modifier.FINAL);
          }
          break;
        case ANNOTATION:
          // TODO(bcorso): Add support for annotations in XPoet.
          builder = XConverters.toXPoet(
              com.squareup.javapoet.TypeSpec.annotationBuilder(name),
              com.squareup.kotlinpoet.TypeSpec.annotationBuilder(name));
          break;
        case OBJECT:
          builder = XTypeSpec.objectBuilder(name);
          break;
        case ANONYMOUS_CLASS:
          checkState(name == null);
          builder = XTypeSpec.anonymousClassBuilder("");
          break;
        default:
          throw new AssertionError();
      }

      if (originatingElement != null) {
        builder.addOriginatingElement(originatingElement);
      }

      if (isStatic) {
        // TODO(bcorso): Handle the KotlinPoet side of this implementation.
        toJavaPoet(builder).addModifiers(Modifier.STATIC);
      }

      if (isAbstract) {
        builder.addAbstractModifier();
      }

      if (visibility != null) {
        builder.setVisibility(visibility);
      }

      for (String name : alwaysQualifyNames) {
        // TODO(bcorso): Handle the KotlinPoet side of this implementation.
        toJavaPoet(builder).alwaysQualify(name);
      }

      for (XCodeBlock javadoc : javadocs) {
        // TODO(bcorso): Handle the KotlinPoet side of this implementation.
        toJavaPoet(builder).addJavadoc(toJavaPoet(javadoc));
      }

      if (superclass != null) {
        if (superclass instanceof XTypeName) {
          builder.superclass((XTypeName) superclass);
        } else if (superclass instanceof TypeName) {
          toJavaPoet(builder).superclass((TypeName) superclass);
        } else {
          throw new AssertionError("Unexpected superclass class: " + superclass.getClass());
        }
      }

      for (Object superinterface : superinterfaces) {
        if (superinterface instanceof XTypeName) {
          builder.addSuperinterface((XTypeName) superinterface);
        } else if (superinterface instanceof TypeName) {
          toJavaPoet(builder).addSuperinterface((TypeName) superinterface);
        } else {
          throw new AssertionError("Unexpected superinterface class: " + superinterface.getClass());
        }
      }

      for (Object typeVariableName : typeVariableNames) {
        if (typeVariableName instanceof XTypeName) {
          builder.addTypeVariable((XTypeName) typeVariableName);
        } else if (typeVariableName instanceof TypeVariableName) {
          toJavaPoet(builder).addTypeVariable((TypeVariableName) typeVariableName);
        } else {
          throw new AssertionError("Unexpected typeVariableName class: " + typeVariableName.getClass());
        }
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

      for (Object type : types) {
        if (type instanceof XTypeSpec) {
          builder.addType((XTypeSpec) type);
        } else if (type instanceof TypeSpec) {
          toJavaPoet(builder).addType((TypeSpec) type);
        } else {
          throw new AssertionError("Unexpected type class: " + type.getClass());
        }
      }

      for (Object property : properties) {
        if (property instanceof XPropertySpec) {
          builder.addProperty((XPropertySpec) property);
        } else if (property instanceof FieldSpec) {
          toJavaPoet(builder).addField((FieldSpec) property);
        } else {
          throw new AssertionError("Unexpected property class: " + property.getClass());
        }
      }

      for (Object function : functions) {
        if (function instanceof XFunSpec) {
          builder.addFunction((XFunSpec) function);
        } else if (function instanceof MethodSpec) {
          toJavaPoet(builder).addMethod((MethodSpec) function);
        } else {
          throw new AssertionError("Unexpected function class: " + function.getClass());
        }
      }

      return builder.build();
    }
  }

  private XTypeSpecs() {}
}
