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

load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_android_local_test")

package(default_visibility = ["//:src"])

kt_android_local_test(
    name = "ComposeComponentHostExtrasTest",
    srcs = ["ComposeComponentHostExtrasTest.kt"],
    manifest = "ComposeComponentHostExtrasTest_AndroidManifest.xml",
    manifest_values = {
        "minSdkVersion": "21",
    },
    deps = [
        "//:dagger_with_compiler/hilt/testing",
        "//java/dagger/hilt/android/compose:compose_component_host_extras",
        "//third_party/java/android_libs/guava_jdk5:guava_testlib",
        "//third_party/java/androidx/compose/runtime",
        "//third_party/java/androidx/compose/ui/test_junit4",
        "//third_party/java/truth",
    ],
)

kt_android_local_test(
    name = "RememberComponentHostTest",
    srcs = ["RememberComponentHostTest.kt"],
    manifest = "RememberComponentHostTest_AndroidManifest.xml",
    manifest_values = {
        "minSdkVersion": "21",
    },
    resource_files = glob(["res/**"]),
    deps = [
        "//:dagger_with_compiler/hilt:android_entry_point",
        "//:dagger_with_compiler/hilt:view_model",
        "//:dagger_with_compiler/hilt/testing",
        "//:dagger_with_compiler/hilt/testing:bind_value",
        "//java/dagger/hilt/android/compose:component_host_creator",
        "//java/dagger/hilt/android/compose:compose_component_host",
        "//java/dagger/hilt/android/compose:compose_retained_lifecycle",
        "//java/dagger/hilt/android/compose:compose_retained_provided",
        "//java/dagger/hilt/android/compose:remember_component_host",
        "//java/dagger/hilt/android/compose/internal/testing",
        "//java/dagger/hilt/android/compose/scopes",
        "//java/dagger/hilt/android/qualifiers",
        "//java/dagger/hilt/android/testing:hilt_android_rule",
        "//third_party/java/androidx/activity",
        "//third_party/java/androidx/activity/compose",
        "//third_party/java/androidx/compose/foundation",
        "//third_party/java/androidx/compose/foundation/layout",
        "//third_party/java/androidx/compose/material",
        "//third_party/java/androidx/compose/runtime",
        "//third_party/java/androidx/compose/ui",
        "//third_party/java/androidx/compose/ui/test",
        "//third_party/java/androidx/compose/ui/test_junit4",
        "//third_party/java/androidx/compose/ui/text",
        "//third_party/java/androidx/lifecycle/common",
        "//third_party/java/error_prone:annotations",
        "//third_party/java/jsr305_annotations",
        "//third_party/java/jsr330_inject",
        "//third_party/java/truth",
        "@maven//:androidx_lifecycle_lifecycle_viewmodel",
    ],
)

kt_android_local_test(
    name = "RememberComponentHostExtrasTest",
    srcs = ["RememberComponentHostExtrasTest.kt"],
    manifest = "RememberComponentHostExtrasTest_AndroidManifest.xml",
    manifest_values = {
        "minSdkVersion": "21",
    },
    deps = [
        "//:dagger_with_compiler/hilt:android_entry_point",
        "//:dagger_with_compiler/hilt/testing",
        "//:dagger_with_compiler/hilt/testing:bind_value",
        "//java/dagger/hilt/android/compose:compose_component_host",
        "//java/dagger/hilt/android/compose:compose_component_host_extras",
        "//java/dagger/hilt/android/compose:compose_retained_lifecycle",
        "//java/dagger/hilt/android/compose:compose_retained_provided",
        "//java/dagger/hilt/android/compose:remember_component_host",
        "//java/dagger/hilt/android/compose/internal/testing",
        "//java/dagger/hilt/android/compose/scopes",
        "//third_party/java/androidx/activity",
        "//third_party/java/androidx/activity/compose",
        "//third_party/java/androidx/compose/foundation/layout",
        "//third_party/java/androidx/compose/runtime",
        "//third_party/java/androidx/compose/ui/test_junit4",
        "//third_party/java/jsr330_inject",
        "//third_party/java/truth",
    ],
)
