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

package dagger.internal.codegen.xprocessing;

import static androidx.room.compiler.processing.compat.XConverters.getProcessingEnv;
import static androidx.room.compiler.processing.compat.XConverters.toKS;

import androidx.room.compiler.processing.XElement;
import androidx.room.compiler.processing.XMethodElement;
import androidx.room.compiler.processing.XProcessingEnv;
import androidx.room.compiler.processing.XTypeElement;
import com.google.devtools.ksp.symbol.KSDeclaration;
import com.google.devtools.ksp.symbol.Modifier;

// TODO(bcorso): Consider moving these methods into XProcessing library.
/** A utility class for {@link XMethodElement} helper methods. */
public final class XMethodElements {

  /** Returns the type this method is enclosed in. */
  public static XTypeElement getEnclosingTypeElement(XMethodElement method) {
    // TODO(bcorso): In Javac, a method is always enclosed in a type; however, once we start
    //  processing Kotlin we'll want to check this explicitly and add an error to the validation
    //  report if the method is not enclosed in a type.
    return method.getEnclosingElement().getType().getTypeElement();
  }

  /** Returns {@code true} if the given method has type parameters. */
  public static boolean hasTypeParameters(XMethodElement method) {
    return !method.getExecutableType().getTypeVariableNames().isEmpty();
  }

  public static boolean hasOverride(XMethodElement method) {
    XProcessingEnv.Backend backend = getProcessingEnv(method).getBackend();
    switch (backend) {
      case JAVAC:
        return method.hasAnnotation(XTypeNames.OVERRIDE);
      case KSP:
        return method.getEnclosingElement().isFromJava()
            ? method.hasAnnotation(XTypeNames.OVERRIDE)
            // TODO(bcorso): Fix XConverters.toKS to avoid the two casts below.
            //   1. The cast to XElement is needed to force the use of toKS(XElement) rather than
            //      toKS(XExecutableElement) -- the former returns the underlying property
            //      declaration when used with a KspSyntheticPropertyMethodElement, while the latter
            //      will throw an exception.
            //   2. The cast to KSDeclaration is needed because toKS(XElement) returns an XAnnotated
            //      rather than KSDeclaration, and KSDeclaration is needed to access the modifiers.
            : ((KSDeclaration) toKS((XElement) method)).getModifiers().contains(Modifier.OVERRIDE);
    }
    throw new AssertionError("Unsupported backend: " + backend);
  }

  private XMethodElements() {}
}
