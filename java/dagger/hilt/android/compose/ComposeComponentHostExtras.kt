/*
 * Copyright (C) 2023 The Dagger Authors.
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

package dagger.hilt.android.compose

import dagger.hilt.android.internal.ThreadUtil

/**
 * Read only version of extras provided to a ComposeComponentHost.
 *
 * Use [buildComposeComponentHostExtras] to create an instance of ComposeComponentHostExtras.
 *
 * ComposeComponentHostExtras support a specific use case and should not replace alternate means of
 * providing objects to a host:
 * - Objects that are part of the Dagger graph should be injected directly where they're used.
 * - Objects that aren't part of the Dagger graph can be provided through ComposeComponentHostExtras
 *   if they're used in enough places to benefit from making them globally accessible in the
 *   ComposeComponent and ComposeRetainedComponent.
 * - Objects that are not used in enough places to warrant use of ComposeComponentHostExtras can be
 *   passed as parameters to `@Composable` functions.
 */
class ComposeComponentHostExtras internal constructor(private val keyToExtra: Map<Key<*>, Any>) {

  /** Returns the argument associated with the given key, if available. */
  @Suppress("UNCHECKED_CAST") // extras can only be inserted with a type that matches the key
  fun <T> get(key: Key<T>): T? = keyToExtra[key] as T

  /**
   * Represents the key for a given argument.
   *
   * Key subclasses require no implementation and should generally be implemented as a Kotlin
   * object. The generic type parameter should be set as the type of the argument. Example key for a
   * specific argument of type String:
   * ```kotlin
   * object MyStringKey: Key<String>
   * ```
   */
  interface Key<out T>

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ComposeComponentHostExtras) return false
    return keyToExtra == other.keyToExtra
  }

  override fun hashCode(): Int = keyToExtra.hashCode()
}

/**
 * Builds a new instance of [ComposeComponentHostExtras] by populating a
 * [MutableComposeComponentHostExtras] instance using the given [builderAction].
 *
 * Build and use the extras in a host with the following:
 * ```kotlin
 * @Composable
 * fun MyContent() {
 *   val extras = buildComposeComponentHostExtras {
 *     put(MyStringKey, "someStringExtra")
 *     put(MyIntKey, 5)
 *     put(MyBooleanKey, true)
 *   }
 *
 *   val host = rememberComponentHost(myHost, extras)
 * }
 * ```
 */
fun buildComposeComponentHostExtras(
  builderAction: MutableComposeComponentHostExtras.() -> Unit
): ComposeComponentHostExtras {
  ThreadUtil.ensureMainThread()
  return MutableComposeComponentHostExtras().apply { this.builderAction() }.build()
}

/**
 * The write-only version of extras provided to a ComposeComponentHost.
 *
 * For all [put] methods, calling the method multiple times with the same key will overwrite the
 * stored value. For example:
 * ```kotlin
 * @Composable fun Content() {
 *   val extras = buildComposeComponentHostExtras {
 *     put(SomeStringKey, "hello") // "hello" is associated with SomeStringKey
 *     put(SomeStringKey, "world") // now "world" is associated with SomeStringKey
 *   }
 * }
 * ```
 */
class MutableComposeComponentHostExtras internal constructor() {
  private val keyToExtra: MutableMap<ComposeComponentHostExtras.Key<*>, Any> = mutableMapOf()

  fun put(key: ComposeComponentHostExtras.Key<Int>, value: Int) {
    keyToExtra[key] = value
  }

  fun put(key: ComposeComponentHostExtras.Key<Long>, value: Long) {
    keyToExtra[key] = value
  }

  fun put(key: ComposeComponentHostExtras.Key<Float>, value: Float) {
    keyToExtra[key] = value
  }

  fun put(key: ComposeComponentHostExtras.Key<Double>, value: Double) {
    keyToExtra[key] = value
  }

  fun put(key: ComposeComponentHostExtras.Key<Boolean>, value: Boolean) {
    keyToExtra[key] = value
  }

  fun put(key: ComposeComponentHostExtras.Key<String>, value: String) {
    keyToExtra[key] = value
  }

  // TODO: b/303256918 - Also support immutable data types and protos

  internal fun build() = ComposeComponentHostExtras(keyToExtra)
}
