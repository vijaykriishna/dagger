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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.compose.RememberComponentHostExtrasTest.TestHost.Companion.getOnlyOrNull
import dagger.hilt.android.compose.internal.testing.assertHasCustomInfo
import dagger.hilt.android.compose.internal.testing.customInfo
import dagger.hilt.android.compose.internal.testing.LabeledNode
import dagger.hilt.android.compose.internal.testing.onNodeWithLabel
import dagger.hilt.android.compose.internal.testing.NodeLabel
import dagger.hilt.android.compose.scopes.ComposeRetainedScoped
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class)
@HiltAndroidTest
class RememberComponentHostExtrasTest {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val composeTestRule = createEmptyComposeRule()

  @BindValue val isRetainedLifecycleCleared: AtomicBoolean = AtomicBoolean(false)

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @Test
  fun rememberComponentHost_argsAvailableInHost() {
    ActivityScenario.launch(ConstantHostExtrasActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(TestHost.extraLabel)
        .assertHasCustomInfo(ConstantHostExtrasActivity.EXTRA_VALUE)
    }
  }

  @Test
  fun rememberComponentHost_argsAvailableInComposeComponentDep() {
    ActivityScenario.launch(ConstantHostExtrasActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(ComposeComponentDep.extraLabel)
        .assertHasCustomInfo(ConstantHostExtrasActivity.EXTRA_VALUE)
    }
  }

  @Test
  fun rememberComponentHost_argsAvailableInComposeRetainedDep() {
    ActivityScenario.launch(ConstantHostExtrasActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(ComposeRetainedComponentDep.extraLabel)
        .assertHasCustomInfo(ConstantHostExtrasActivity.EXTRA_VALUE)
    }
  }

  @Test
  fun rememberComponentHost_rememberedExtraChanges_componentsAreConsistent() {
    ActivityScenario.launch(RememberedExtrasActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(RememberedExtrasActivity.clickToChangeExtraLabel)
        .performClick()
      composeTestRule
        .onNodeWithLabel(RememberedExtrasActivity.clickToChangeExtraLabel)
        .assertHasCustomInfo(1)

      // The above click changes the value of the extra provided to the host. Both the
      // ComposeComponent and the ComposeRetainedComponent should be recreated and the dependencies
      // injected from each should see a consistent value for the extra.
      composeTestRule.onNodeWithLabel(ComposeComponentDep.extraLabel).assertHasCustomInfo(1)
      composeTestRule.onNodeWithLabel(ComposeRetainedComponentDep.extraLabel).assertHasCustomInfo(1)
    }
  }

  @Test
  fun rememberComponentHost_rememberedExtraConfigurationChange_componentsAreConsistent() {
    ActivityScenario.launch(RememberedExtrasActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(RememberedExtrasActivity.clickToChangeExtraLabel)
        .performClick()
      composeTestRule
        .onNodeWithLabel(RememberedExtrasActivity.clickToChangeExtraLabel)
        .assertHasCustomInfo(1)

      it.recreate()

      // The extras's value is only remembered, so it's reset after activity recreation. Resetting
      // the remembered value means the extra changes, so both the ComposeComponent and the
      // ComposeRetainedComponent should be recreated and the dependencies injected from each should
      // see a consistent value for the extra.
      composeTestRule.onNodeWithLabel(ComposeComponentDep.extraLabel).assertHasCustomInfo(0)
      composeTestRule.onNodeWithLabel(ComposeRetainedComponentDep.extraLabel).assertHasCustomInfo(0)
    }
  }

  @Test
  fun rememberComponentHost_rememberSaveabledExtraChanges_componentsAreConsistent() {
    ActivityScenario.launch(RemeberSaveabledExtrasActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(RemeberSaveabledExtrasActivity.clickToChangeExtraLabel)
        .performClick()
      composeTestRule
        .onNodeWithLabel(RemeberSaveabledExtrasActivity.clickToChangeExtraLabel)
        .assertHasCustomInfo(1)

      // The above click changes the value of the extra provided to the host. Both the
      // ComposeComponent and the ComposeRetainedComponent should be recreated and the dependencies
      // injected from each should see a consistent value for the extra.
      composeTestRule.onNodeWithLabel(ComposeComponentDep.extraLabel).assertHasCustomInfo(1)
      composeTestRule.onNodeWithLabel(ComposeRetainedComponentDep.extraLabel).assertHasCustomInfo(1)
    }
  }

  @Test
  fun rememberComponentHost_rememberSaveabledExtraConfigurationChange_componentsAreConsistent() {
    ActivityScenario.launch(RemeberSaveabledExtrasActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(RemeberSaveabledExtrasActivity.clickToChangeExtraLabel)
        .performClick()
      composeTestRule
        .onNodeWithLabel(RemeberSaveabledExtrasActivity.clickToChangeExtraLabel)
        .assertHasCustomInfo(1)

      it.recreate()

      // The extra's value is rememberSaveable, so it doesn't change after activity recreation.
      // The host and the ComposeComponent are recreated, but the ComposeRetainedComponent is not.
      // Dependencies injected from each component should still see a consistent value.
      composeTestRule.onNodeWithLabel(ComposeComponentDep.extraLabel).assertHasCustomInfo(1)
      composeTestRule.onNodeWithLabel(ComposeRetainedComponentDep.extraLabel).assertHasCustomInfo(1)
    }
  }

  @Test
  fun rememberComponentHost_extrasWithSameHashCodeChange_extrasAreCorrect() {
    ActivityScenario.launch(HashCollisionActivity::class.java).use {
      // Switch to using extras2
      composeTestRule
        .onNodeWithLabel(HashCollisionActivity.clickToToggleUseExtra1Label)
        .performClick()
      composeTestRule
        .onNodeWithLabel(ComposeComponentDep.extraLabel)
        .assertHasCustomInfo(TestHost.CollidingExtraKey2.value)
      composeTestRule
        .onNodeWithLabel(ComposeRetainedComponentDep.extraLabel)
        .assertHasCustomInfo(TestHost.CollidingExtraKey2.value)

      // Recreating switches back to using extras1, but extras1 and extras2 have the same hashCode
      it.recreate()

      // Even though the hashCodes for the extras are the same, they aren't equal so the retained
      // component should be recreated and pick up the new extra value
      composeTestRule
        .onNodeWithLabel(ComposeComponentDep.extraLabel)
        .assertHasCustomInfo(TestHost.CollidingExtraKey1.value)
      composeTestRule
        .onNodeWithLabel(ComposeRetainedComponentDep.extraLabel)
        .assertHasCustomInfo(TestHost.CollidingExtraKey1.value)
    }
  }

  @Test
  fun rememberComponentHost_extrasWithSameHashCodeChange_retainedComponentCleared() {
    ActivityScenario.launch(HashCollisionActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(HashCollisionActivity.clickToToggleUseExtra1Label)
        .performClick()
      composeTestRule
        .onNodeWithLabel(RetainedComponentClearedTracker.clickToResetLabel)
        .performClick()
      assertThat(isRetainedLifecycleCleared.get()).isFalse()

      it.recreate()

      // The extras changed, but have the same hashCode. This means we'll restore the retained
      // component, see that the extras are not equal, then discard and clear the component
      assertThat(isRetainedLifecycleCleared.get()).isTrue()
    }
  }

  @Test
  fun rememberComponentHost_constantExtras_retainedComponentRetained() {
    ActivityScenario.launch(ConstantHostExtrasActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(RetainedComponentClearedTracker.clickToResetLabel)
        .performClick()
      assertThat(isRetainedLifecycleCleared.get()).isFalse()

      it.recreate()

      // Since the extra is constant, the ComposeRetainedComponent should not be cleared or
      // recreated after activity recreation
      assertThat(isRetainedLifecycleCleared.get()).isFalse()
    }
  }

  @Test
  fun rememberComponentHost_extrasChange_previousRetainedComponentCleared() {
    ActivityScenario.launch(RememberedExtrasActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(RetainedComponentClearedTracker.clickToResetLabel)
        .performClick()
      assertThat(isRetainedLifecycleCleared.get()).isFalse()

      composeTestRule
        .onNodeWithLabel(RememberedExtrasActivity.clickToChangeExtraLabel)
        .performClick()
      composeTestRule
        .onNodeWithLabel(RememberedExtrasActivity.clickToChangeExtraLabel)
        .assertHasCustomInfo(1)

      // Changing the extra causes the previous ComposeRetainedComponent to be cleared
      assertThat(isRetainedLifecycleCleared.get()).isTrue()
    }
  }

  /** Sets [TestHost's][TestHost] extras to constant values. */
  @AndroidEntryPoint(ComponentActivity::class)
  class ConstantHostExtrasActivity :
    Hilt_RememberComponentHostExtrasTest_ConstantHostExtrasActivity() {
    @Inject lateinit var testHostCreator: ComponentHostCreator<TestHost>

    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      setContent {
        val extras = buildComposeComponentHostExtras { put(TestHost.ExtraKey, EXTRA_VALUE) }

        val host = rememberComponentHost(testHostCreator, extras)
        host.Content()
      }
    }

    companion object {
      const val EXTRA_VALUE = 99
    }
  }

  /** Preserves extra values across recomposition by using [remember]. */
  @AndroidEntryPoint(ComponentActivity::class)
  class RememberedExtrasActivity : Hilt_RememberComponentHostExtrasTest_RememberedExtrasActivity() {
    @Inject lateinit var testHostCreator: ComponentHostCreator<TestHost>

    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      setContent {
        Column {
          var extraValue by remember { mutableStateOf(0) }

          LabeledNode(
            clickToChangeExtraLabel,
            Modifier.clickable { extraValue++ }.customInfo(extraValue)
          )

          val extras = buildComposeComponentHostExtras { put(TestHost.ExtraKey, extraValue) }

          val host = rememberComponentHost(testHostCreator, extras)
          host.Content()
        }
      }
    }

    companion object {
      val clickToChangeExtraLabel = NodeLabel("RememberedExtrasActivity", "clickToChangeExtra")
    }
  }

  /** Preserves extra values across configuration changes by using [rememberSaveable]. */
  @AndroidEntryPoint(ComponentActivity::class)
  class RemeberSaveabledExtrasActivity :
    Hilt_RememberComponentHostExtrasTest_RemeberSaveabledExtrasActivity() {
    @Inject lateinit var testHostCreator: ComponentHostCreator<TestHost>

    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      setContent {
        Column {
          var extraValue by rememberSaveable { mutableStateOf(0) }

          LabeledNode(
            clickToChangeExtraLabel,
            Modifier.clickable { extraValue++ }.customInfo(extraValue),
          )

          val extras = buildComposeComponentHostExtras { put(TestHost.ExtraKey, extraValue) }

          val host = rememberComponentHost(testHostCreator, extras)
          host.Content()
        }
      }
    }

    companion object {
      val clickToChangeExtraLabel =
        NodeLabel("RemeberSaveabledExtrasActivity", "clickToChangeExtra")
    }
  }

  @AndroidEntryPoint(ComponentActivity::class)
  class HashCollisionActivity : Hilt_RememberComponentHostExtrasTest_HashCollisionActivity() {
    @Inject lateinit var testHostCreator: ComponentHostCreator<TestHost>

    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      setContent {
        Column {
          var useExtra1 by remember { mutableStateOf(true) }

          LabeledNode(
            clickToToggleUseExtra1Label,
            Modifier.clickable { useExtra1 = !useExtra1 }.customInfo(useExtra1)
          )

          val extras = buildComposeComponentHostExtras {
            if (useExtra1) {
              put(TestHost.CollidingExtraKey1, TestHost.CollidingExtraKey1.value)
            } else {
              put(TestHost.CollidingExtraKey2, TestHost.CollidingExtraKey2.value)
            }
          }

          val host = rememberComponentHost(testHostCreator, extras)
          host.Content()
        }
      }
    }

    companion object {
      val clickToToggleUseExtra1Label = NodeLabel("HashCollisionActivity", "clickToToggleUseExtra1")
    }
  }

  @ComposeComponentHost
  class TestHost
  @Inject
  internal constructor(
    private val extras: ComposeComponentHostExtras?,
    private val composeComponentDep: ComposeComponentDep,
    private val composeRetainedComponentDep: ComposeRetainedComponentDep,
    private val retainedComponentClearedTracker: RetainedComponentClearedTracker,
  ) : Hilt_RememberComponentHostExtrasTest_TestHost() {

    @Composable
    fun Content() {
      Column {
        LabeledNode(extraLabel, Modifier.customInfo(extras?.get(ExtraKey)))

        composeComponentDep.Content()

        composeRetainedComponentDep.Content()

        retainedComponentClearedTracker.Content()
      }
    }

    object ExtraKey : ComposeComponentHostExtras.Key<Int>

    open class CollidingExtraKey constructor(val value: Int) : ComposeComponentHostExtras.Key<Int> {

      /**
       * Returns a hashCode which will easily cause hash collisions for ComposeComponentHostExtras.
       *
       * Map's hashCode is a composition of the hashCodes of all of the entries, and each entry's
       * hashCode is `key.hashCode() xor value.hashCode()`. If the key and the value have the same
       * hashCode, they'll xor to 0. We can create two non-equal maps with the same hashCode by
       * mapping two different objects to themselves. ComposeComponentHostExtras doesn't allow
       * arbitrary values, so we can't directly map a key to itself. However, we can instead
       * override the key's hashCode to be the same as its value. By using the subclasses
       * [CollidingExtraKey1] and [CollidingExtraKey2] we can create two unequal
       * ComposeComponentHostExtras that have the same hashCode:
       * ```kotlin
       * val extras1 = buildComposeComponentHostExtras {
       *   put(CollidingExtraKey1, CollidingExtraKey1.value)
       * }
       *
       * val extras2 = buildComposeComponentHostExtras {
       *   put(CollidingExtraKey2, CollidingExtraKey2.value)
       * }
       * ```
       */
      final override fun hashCode(): Int = value.hashCode()

      /** Not needed for this test, included to avoid overriding hashCode without equals warning. */
      override fun equals(other: Any?): Boolean {
        if (other !is CollidingExtraKey) return false
        return value == other.value
      }
    }

    object CollidingExtraKey1 : CollidingExtraKey(5)

    object CollidingExtraKey2 : CollidingExtraKey(10)

    companion object {
      val extraLabel = NodeLabel("TestHost", "extra")

      /**
       * Picks the first extra that's present in the order of [ExtraKey], [CollidingExtraKey1], then
       * [CollidingExtraKey2], crashing if more than one is set.
       */
      fun ComposeComponentHostExtras.getOnlyOrNull(): Int? {
        val extrasInOrder =
          listOfNotNull(get(ExtraKey), get(CollidingExtraKey1), get(CollidingExtraKey2))
        require(extrasInOrder.size < 2) {
          "Multiple extras found: $extrasInOrder. Tests assume only one extra is set."
        }
        return extrasInOrder.firstOrNull()
      }
    }
  }

  class ComposeComponentDep @Inject constructor(private val extras: ComposeComponentHostExtras?) {

    @Composable
    fun Content() {
      LabeledNode(extraLabel, Modifier.customInfo(extras?.getOnlyOrNull()))
    }

    companion object {
      val extraLabel = NodeLabel("TestHost", "ComposeComponentDep", "extra")
    }
  }

  @ComposeRetainedScoped
  @ComposeRetainedProvided
  class ComposeRetainedComponentDep
  @Inject
  internal constructor(private val extras: ComposeComponentHostExtras?) {

    @Composable
    fun Content() {
      LabeledNode(extraLabel, Modifier.customInfo(extras?.getOnlyOrNull()))
    }

    companion object {
      val extraLabel = NodeLabel("TestHost", "ComposeRetainedDep", "extra")
    }
  }

  @ComposeRetainedScoped
  @ComposeRetainedProvided
  class RetainedComponentClearedTracker
  @Inject
  internal constructor(
    composeRetainedLifecycle: ComposeRetainedLifecycle,
    private val isRetainedLifecycleCleared: AtomicBoolean,
  ) {

    init {
      composeRetainedLifecycle.addOnClearedListener { isRetainedLifecycleCleared.set(true) }
    }

    @Composable
    fun Content() {
      LabeledNode(
        clickToResetLabel,
        Modifier.clickable { isRetainedLifecycleCleared.set(false) },
      )
    }

    companion object {
      val clickToResetLabel =
        NodeLabel("TestHost", "RetainedComponentClearedTracker", "clickToReset")
    }
  }
}
