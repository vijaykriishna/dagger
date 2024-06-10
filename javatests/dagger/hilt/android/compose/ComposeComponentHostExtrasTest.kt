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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.testing.EqualsTester
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class)
@HiltAndroidTest
class ComposeComponentHostExtrasTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun get_notPresent_isNull() {
    composeTestRule.setContent {
      val value: Int? = buildComposeComponentHostExtras {}.get(IntKey)
      assertThat(value).isNull()
    }
  }

  @Test
  fun get_intKey_returnsProperValue() {
    composeTestRule.setContent {
      val value: Int? = buildComposeComponentHostExtras { put(IntKey, 5) }.get(IntKey)
      assertThat(value).isEqualTo(5)
    }
  }

  @Test
  fun get_longKey_returnsProperValue() {
    composeTestRule.setContent {
      val value: Long? = buildComposeComponentHostExtras { put(LongKey, 5) }.get(LongKey)
      assertThat(value).isEqualTo(5)
    }
  }

  @Test
  fun get_floatKey_returnsProperValue() {
    composeTestRule.setContent {
      val value: Float? = buildComposeComponentHostExtras { put(FloatKey, 5.5f) }.get(FloatKey)
      assertThat(value).isEqualTo(5.5f)
    }
  }

  @Test
  fun get_doubleKey_returnsProperValue() {
    composeTestRule.setContent {
      val value: Double? = buildComposeComponentHostExtras { put(DoubleKey, 5.5) }.get(DoubleKey)
      assertThat(value).isEqualTo(5.5)
    }
  }

  @Test
  fun get_booleanKey_returnsProperValue() {
    composeTestRule.setContent {
      val value: Boolean? =
        buildComposeComponentHostExtras { put(BooleanKey, true) }.get(BooleanKey)
      assertThat(value).isNotNull()
      assertThat(value).isTrue()
    }
  }

  @Test
  fun get_stringKey_returnsProperValue() {
    composeTestRule.setContent {
      val value: String? =
        buildComposeComponentHostExtras { put(StringKey, "hello") }.get(StringKey)
      assertThat(value).isNotNull()
      assertThat(value).isEqualTo("hello")
    }
  }

  @Test
  fun get_sameKeyPutMultipleTimes_overwritesValue() {
    composeTestRule.setContent {
      val extras = buildComposeComponentHostExtras {
        put(StringKey, "hello")
        put(StringKey, "world")
      }

      assertThat(extras.get(StringKey)).isEqualTo("world")
    }
  }

  @Test
  fun get_multipleSameTypeExtrasInserted_returnsProperValue() {
    composeTestRule.setContent {
      val extras = buildComposeComponentHostExtras {
        put(StringKey, "hello")
        put(StringKey2, "world")
      }

      assertThat(extras.get(StringKey)).isEqualTo("hello")
      assertThat(extras.get(StringKey2)).isEqualTo("world")
    }
  }

  @Test
  fun get_multipleDifferentTypeExtrasInserted_allReturnProperValues() {
    composeTestRule.setContent {
      val extras = buildComposeComponentHostExtras {
        put(IntKey, 5)
        put(FloatKey, 5.5f)
        put(StringKey, "hello")
      }

      assertThat(extras.get(IntKey)).isEqualTo(5)
      assertThat(extras.get(FloatKey)).isEqualTo(5.5f)
      assertThat(extras.get(StringKey)).isEqualTo("hello")
    }
  }

  @Test
  fun equals() {
    composeTestRule.setContent {
      EqualsTester()
        .addEqualityGroup(buildComposeComponentHostExtras { put(IntKey, 5) })
        .addEqualityGroup(buildComposeComponentHostExtras { put(IntKey, 10) })
        .addEqualityGroup(buildComposeComponentHostExtras { put(FloatKey, 10f) })
        .addEqualityGroup(
          buildComposeComponentHostExtras { put(StringKey, "hello") },
          buildComposeComponentHostExtras { put(StringKey, "hello") }
        )
        .addEqualityGroup(
          buildComposeComponentHostExtras {
            put(IntKey, 5)
            put(StringKey, "hello")
          },
          buildComposeComponentHostExtras {
            put(IntKey, 5)
            put(StringKey, "hello")
          }
        )
        .testEquals()
    }
  }

  private object IntKey : ComposeComponentHostExtras.Key<Int>

  private object LongKey : ComposeComponentHostExtras.Key<Long>

  private object FloatKey : ComposeComponentHostExtras.Key<Float>

  private object DoubleKey : ComposeComponentHostExtras.Key<Double>

  private object BooleanKey : ComposeComponentHostExtras.Key<Boolean>

  private object StringKey : ComposeComponentHostExtras.Key<String>

  private object StringKey2 : ComposeComponentHostExtras.Key<String>
}
