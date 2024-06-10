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
#   Internal Hilt Android classes for Jetpack Compose.

load("//third_party/kotlin/build_extensions:rules.bzl", "kt_android_library")

package(default_visibility = ["//:src"])

kt_android_library(
    name = "internal",
    srcs = ["InternalComponentHostCreator.kt"],
    deps = [
        "//java/dagger/hilt/android/compose:component_host_creator",
        "//java/dagger/hilt/android/compose:compose_component_host_extras",
        "//java/dagger/hilt/android/compose/components",
    ],
)
