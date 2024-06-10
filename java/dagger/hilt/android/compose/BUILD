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
#   Hilt Android classes for Jetpack Compose.

load("//third_party/kotlin/build_extensions:rules.bzl", "kt_android_library")

package(default_visibility = ["//:src"])

java_library(
    name = "package_info",
    srcs = ["package-info.java"],
)

kt_android_library(
    name = "retained_view_model_manager",
    srcs = ["RetainedViewModelManager.kt"],
    visibility = ["//visibility:private"],
    deps = [
        ":remember_component_host",
        "//:dagger_with_compiler",
        "//third_party/java/androidx/activity",
        "//third_party/java/androidx/lifecycle",
    ],
)

kt_android_library(
    name = "component_host_creator",
    srcs = ["ComponentHostCreator.kt"],
)

kt_android_library(
    name = "compose_component_host",
    srcs = ["ComposeComponentHost.kt"],
    exported_plugins = [
        "//java/dagger/hilt/android/processor/internal/compose/composecomponenthost:processor",
    ],
    exports = [
        ":component_host_creator",
        ":compose_component_host_extras",
        ":retained_view_model_manager",
        "//:dagger_with_compiler",
        "//java/dagger/hilt:install_in",
        "//java/dagger/hilt/android/components",
        "//java/dagger/hilt/android/compose/components",
        "//java/dagger/hilt/android/compose/internal",
        "//java/dagger/hilt/android/compose/internal/builders",
        "//java/dagger/hilt/codegen:originating_element",
        "//java/dagger/hilt/internal:generated_entry_point",
    ],
    deps = [
        "//java/dagger/hilt:generates_root_input",
    ],
)

kt_android_library(
    name = "compose_component_host_extras",
    srcs = ["ComposeComponentHostExtras.kt"],
    deps = ["//java/dagger/hilt/android/internal"],
)

kt_android_library(
    name = "compose_retained_lifecycle",
    srcs = ["ComposeRetainedLifecycle.kt"],
    deps = ["//java/dagger/hilt/android/lifecycle:retained_lifecycle"],
)

kt_android_library(
    name = "compose_retained_provided",
    srcs = ["ComposeRetainedProvided.kt"],
    exported_plugins = [
        "//java/dagger/hilt/android/processor/internal/compose/composeretainedprovided:processor",
    ],
    exports = [
        "//:dagger_with_compiler",
        "//java/dagger/hilt:install_in",
        "//java/dagger/hilt/android/compose/components",
        "//java/dagger/hilt/android/compose/internal/qualifiers",
        "//java/dagger/hilt/codegen:originating_element",
        "//java/dagger/hilt/internal:generated_entry_point",
    ],
)

kt_android_library(
    name = "remember_component_host",
    srcs = ["RememberComponentHost.kt"],
    deps = [
        ":component_host_creator",
        ":compose_component_host_extras",
        ":compose_retained_lifecycle",
        ":compose_retained_provided",
        "//:dagger_with_compiler",
        "//java/dagger/hilt:entry_point",
        "//java/dagger/hilt:install_in",
        "//java/dagger/hilt/android/compose/components",
        "//java/dagger/hilt/android/compose/internal",
        "//java/dagger/hilt/android/compose/scopes",
        "//java/dagger/hilt/android/internal",
        "//java/dagger/hilt/android/internal/lifecycle",
        "//third_party/java/androidx/compose/runtime",
        "//third_party/java/androidx/compose/ui",
        "@maven//:androidx_lifecycle_lifecycle_viewmodel",
        "@maven//:androidx_lifecycle_lifecycle_viewmodel_compose",
    ],
)
