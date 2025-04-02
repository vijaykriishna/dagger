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

import androidx.room.compiler.codegen.XParameterSpec;
import androidx.room.compiler.codegen.XTypeName;

// TODO(bcorso): Consider moving these methods into XProcessing library.
/** A utility class for {@link XParameterSpec} helper methods. */
public final class XParameterSpecs {
  public static XParameterSpec of(String name, XTypeName typeName) {
    return XParameterSpec.builder(
            name,
            typeName,
            /* addJavaNullabilityAnnotation= */ false)
        .build();
  }

  private XParameterSpecs() {}
}
