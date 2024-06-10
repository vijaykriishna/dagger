# Copyright (C) 2023 The Dagger Authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Description:
#   Tests for the @dagger.hilt.android.compose.ComposeRetainedProvided
#   annotation processor.

load("//java/dagger/testing/compile:macros.bzl", "kt_compiler_test")

package(default_visibility = ["//:src"])

kt_compiler_test(
    name = "ComposeRetainedProvidedProcessorTest",
    srcs = ["ComposeRetainedProvidedProcessorTest.kt"],
    compiler_deps = [
        "//java/dagger/hilt/android/compose:compose_retained_provided",
        "//third_party/java/jsr330_inject",
        "//java/dagger/hilt/android/compose/scopes",
        "//java/dagger/hilt/components",
    ],
    deps = [
        "//:dagger_with_compiler/hilt/testing:compiler",
        "//java/dagger/hilt/android/processor/internal/compose/composeretainedprovided:processor_lib",
        "//java/dagger/internal/codegen/xprocessing",
        "//java/dagger/internal/codegen/xprocessing:xprocessing-testing",
        "//third_party/java/compile_testing",
        "//third_party/java/junit",
    ],
)
