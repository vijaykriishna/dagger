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

package dagger.internal.codegen;

import static com.google.common.truth.Truth.assertThat;
import static dagger.internal.codegen.extension.DaggerCollectors.onlyElement;
import static dagger.internal.codegen.xprocessing.XMethodElements.hasOverride;
import static dagger.testing.compile.CompilerTests.javaSource;
import static dagger.testing.compile.CompilerTests.kotlinSource;

import androidx.room.compiler.processing.XMethodElement;
import androidx.room.compiler.processing.XTypeElement;
import androidx.room.compiler.processing.util.Source;
import dagger.testing.compile.CompilerTests;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class XMethodElementsTest {
  @Test
  public void javaHasOverride() {
    Source javaBase =
        javaSource(
            "test.JavaBase",
            "package test;",
            "",
            "class JavaBase {",
            "  void method() {}",
            "}");
    Source javaChild =
        javaSource(
            "test.JavaChild",
            "package test;",
            "",
            "class JavaChild extends JavaBase {",
            "  @Override",
            "  void method() {}",
            "",
            "  void anotherMethod() {}",
            "}");
    CompilerTests.invocationCompiler(javaBase, javaChild)
        .compile(
            (invocation) -> {
              XTypeElement childElement =
                  invocation.getProcessingEnv().requireTypeElement("test.JavaChild");
              XMethodElement overridingMethod = getMethod(childElement, "method");
              XMethodElement nonOverridingMethod = getMethod(childElement, "anotherMethod");
              assertThat(hasOverride(overridingMethod)).isTrue();
              assertThat(hasOverride(nonOverridingMethod)).isFalse();
            });
  }

  @Test
  public void kotlinHasOverride() {
    Source kotlinBase =
        kotlinSource(
            "test.KotlinBase.kt",
            "package test;",
            "",
            "open class KotlinBase {",
            "  open fun method() {}",
            "  open val property: String = \"test\"",
            "}");
    Source kotlinChild =
        kotlinSource(
            "test.KotlinChild.kt",
            "package test;",
            "",
            "class KotlinChild : KotlinBase() {",
            "  override fun method() {}",
            "  override val property: String = \"test\"",
            "",
            "  fun anotherMethod() {}",
            "  val anotherProperty: String = \"test\"",
            "}");
    CompilerTests.invocationCompiler(kotlinBase, kotlinChild)
        .compile(
            (invocation) -> {
              XTypeElement childElement =
                  invocation.getProcessingEnv().requireTypeElement("test.KotlinChild");
              XMethodElement overridingMethod = getMethod(childElement, "method");
              XMethodElement nonOverridingMethod = getMethod(childElement, "anotherMethod");
              // Note: XProcessing doesn't expose properties directly. Instead, it matches the
              // behavior of KAPT and exposes the getter/setter methods and backing field (if one
              // exists).
              XMethodElement overridingProperty = getMethod(childElement, "getProperty");
              XMethodElement nonOverridingProperty = getMethod(childElement, "getAnotherProperty");
              assertThat(hasOverride(overridingMethod)).isTrue();
              assertThat(hasOverride(nonOverridingMethod)).isFalse();
              assertThat(hasOverride(overridingProperty)).isTrue();
              assertThat(hasOverride(nonOverridingProperty)).isFalse();
            });
  }

  private XMethodElement getMethod(XTypeElement typeElement, String methodName) {
    return typeElement.getDeclaredMethods().stream()
        .filter(method -> method.getName().equals(methodName))
        .collect(onlyElement());
  }
}
