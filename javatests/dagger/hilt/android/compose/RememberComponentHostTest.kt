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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.compose.components.ComposeRetainedComponent
import dagger.hilt.android.compose.internal.testing.LabeledNode
import dagger.hilt.android.compose.internal.testing.NodeLabel
import dagger.hilt.android.compose.internal.testing.assertHasCustomInfo
import dagger.hilt.android.compose.internal.testing.customInfo
import dagger.hilt.android.compose.internal.testing.onAllNodesWithLabel
import dagger.hilt.android.compose.internal.testing.onNodeWithLabel
import dagger.hilt.android.compose.scopes.ComposeRetainedScoped
import dagger.hilt.android.compose.scopes.ComposeScoped
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.Nullable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Qualifier
import javax.inject.Singleton
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class)
@HiltAndroidTest
class RememberComponentHostTest {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val composeTestRule = createEmptyComposeRule()

  @BindValue
  @OnClearedComposeHost.RetainedLifecycleCleared
  val isOnClearedComposeHostRetainedLifecycleCleared = AtomicBoolean(false)

  @BindValue
  @OnClearedComposeHost2.RetainedLifecycleCleared
  val isOnClearedComposeHost2RetainedLifecycleCleared = AtomicBoolean(false)

  @Inject @ApplicationContext lateinit var appContext: Context

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @Test
  fun rememberComponentHost_invalidCreator_throws() {
    ActivityScenario.launch(TestActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        activity.setContent {
          @Suppress("CheckReturnValue") // calling rememberComponentHost is sufficient
          rememberComponentHost(creator = object : ComponentHostCreator<ComposeHost> {})
        }
      }

      // This has to be tested in a roundabout way. Calling setContent doesn't immediately cause
      // the content lambda to be run, instead it's stored to be run later. In order to make that
      // lambda run, we need to interact with the composition, which is done here with the assertion
      assertThrows(IllegalArgumentException::class.java) { composeTestRule.onRoot().assertExists() }
    }
  }

  @Test
  fun clickRecomposeButton_recompositionTriggered() {
    ActivityScenario.launch(TestActivity::class.java).use { scenario ->
      // This test verifies that the testing infrastructure in this class actually works,
      // specifically that clicking the recompose button triggers recomposition. Several later tests
      // make sure the same instance is used on recomposition so it's important that this button
      // works.
      scenario.onActivity { activity -> activity.triggerRecomposition() }

      composeTestRule.onNodeWithLabel(TestActivity.recompositionCountLabel).assertHasCustomInfo(1)
    }
  }

  @Test
  fun rememberComponentHost_validCreator_hostIsRememberedAcrossRecomposition() {
    ActivityScenario.launch(TestActivity::class.java).use { scenario ->
      scenario.onActivity { activity -> activity.triggerRecomposition() }
      composeTestRule.onNodeWithLabel(TestActivity.recompositionCountLabel).assertHasCustomInfo(1)

      // If the componentHost wasn't remembered, a new instance would be created and its instance
      // number would change.
      composeTestRule.onNodeWithLabel(ComposeHost.hostLabel).assertHasCustomInfo(0)
    }
  }

  @Test
  fun rememberComponentHost_composeScopedDep_depInstanceIsSharedInHost() {
    ActivityScenario.launch(TestActivity::class.java).use {
      composeTestRule.onNodeWithLabel(ComposeHost.dep1ScopedDepLabel).assertHasCustomInfo(0)
      composeTestRule.onNodeWithLabel(ComposeHost.dep2ScopedDepLabel).assertHasCustomInfo(0)
    }
  }

  @Test
  fun rememberComponentHost_composeScopedDep_depInstanceNotSharedAcrossHosts() {
    ActivityScenario.launch(TestActivity::class.java).use {
      // ComposeComponentHosts define the scope of the ComposeComponent. Two separate hosts mean two
      // separate components and two separate ScopedDeps.
      composeTestRule.onNodeWithLabel(ComposeHost2.scopedDepLabel).assertHasCustomInfo(1)
    }
  }

  @Test
  fun rememberComponentHost_nestedComponentHost_isAddedCorrectly() {
    ActivityScenario.launch(TestActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(TestActivity.NestedComponentHost.nestedHostLabel)
        .assertExists()
    }
  }

  @Test
  fun rememberComponentHost_retainedScopedDep_depInstanceIsSharedInHost() {
    ActivityScenario.launch(TestActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDep.usesRetainedDepRetainedDepLabel)
        .assertHasCustomInfo(0)

      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDep.usesRetainedDep2RetainedDepLabel)
        .assertHasCustomInfo(0)
    }
  }

  @Test
  fun rememberComponentHost_composeScopedAndRetainedScopedDep_onlyRetainedKeptAfterRecreate() {
    ActivityScenario.launch(TestActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDep.usesRetainedDepLabel)
        .assertHasCustomInfo(0)
      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDep.usesRetainedDepRetainedDepLabel)
        .assertHasCustomInfo(0)

      it.recreate()

      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDep.usesRetainedDepLabel)
        .assertHasCustomInfo(1)
      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDep.usesRetainedDepRetainedDepLabel)
        .assertHasCustomInfo(0)
    }
  }

  @Test
  fun rememberComponentHost_composableWithRetainedRemovedAndReAdded_retainedNotKept() {
    ActivityScenario.launch(TestActivity::class.java).use { scenario ->
      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDep.usesRetainedDepRetainedDepLabel)
        .assertHasCustomInfo(0)

      // Remove ComposeHostWithRetainedDep from the composition entirely.
      scenario.onActivity { activity -> activity.setComposeHostWithRetainedDepEnabled(false) }
      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDep.usesRetainedDepRetainedDepLabel)
        .assertDoesNotExist()

      // Re-add ComposeHostWithRetainedDep to the composition. Since it was completely removed, the
      // retained dep is not kept and a new instance is created
      scenario.onActivity { activity -> activity.setComposeHostWithRetainedDepEnabled(true) }
      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDep.usesRetainedDepRetainedDepLabel)
        .assertHasCustomInfo(1)
    }
  }

  @Test
  fun rememberComponentHost_multipleComposeViewsWithIds_noKeyInterference() {
    ActivityScenario.launch(MultipleComposeViewActivity::class.java).use {
      // The same retainedHost1 was added in two separate ComposeViews. Each will have their own
      // instance of dep1 since they're separate hosts.

      composeTestRule
        .onAllNodesWithLabel(ComposeHostWithRetainedDep.usesRetainedDepRetainedDepLabel)
        .apply {
          this[0].assertHasCustomInfo(0)
          this[1].assertHasCustomInfo(1)
        }

      // Remove the first retainedHost1 from the composition. Its ComposeRetainedComponent is fully
      // removed and will not be retained. The second retainedHost1 remains.
      composeTestRule
        .onAllNodesWithText("click to toggle composeHostWithRetainedDep enabled")[0]
        .performClick()
      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDep.usesRetainedDepRetainedDepLabel)
        .assertHasCustomInfo(1)

      // Host2 should still be retained, so recreating the activity should still keep the same
      // instance of retainedDep. If there's key interference, removing host1 would remove host2's
      // ComposeRetainedComponent, and a new retainedDep instance would be created. Since we're
      // recreating, the removal of retainedHost1 is forgotten and it's part of the composition
      // again.
      it.recreate()
      composeTestRule
        .onAllNodesWithLabel(ComposeHostWithRetainedDep.usesRetainedDepRetainedDepLabel)
        .apply {
          this[0].assertHasCustomInfo(2)
          this[1].assertHasCustomInfo(1)
        }
    }
  }

  @Test
  fun rememberComponentHost_nonQualifiedRetainedDep_isRetainedAfterRecreate() {
    ActivityScenario.launch(TestActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDepsFromModules.nonQualifiedTypeDepLabel)
        .assertHasCustomInfo(0)

      it.recreate()

      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDepsFromModules.nonQualifiedTypeDepLabel)
        .assertHasCustomInfo(0)
    }
  }

  @Test
  fun rememberComponentHost_parameterizedTypeRetainedDep_isRetainedAfterRecreate() {
    ActivityScenario.launch(TestActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDepsFromModules.parameterizedTypeDepLabel)
        .assertHasCustomInfo(0)

      it.recreate()

      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDepsFromModules.parameterizedTypeDepLabel)
        .assertHasCustomInfo(0)
    }
  }

  @Test
  fun rememberComponentHost_primitiveTypeRetainedDep_isInjectedCorrectly() {
    ActivityScenario.launch(TestActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDepsFromModules.primitiveTypeDepLabel)
        .assertHasCustomInfo(5)
    }
  }

  @Test
  fun rememberComponentHost_providerPrimitiveTypeRetainedDep_isInjectedCorrectly() {
    ActivityScenario.launch(TestActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDepsFromModules.providerPrimitiveTypeDepLabel)
        .assertHasCustomInfo(5)
    }
  }

  @Test
  fun rememberComponentHost_multipleBindingsModuleFirstTypeRetainedDep_isRetainedAfterRecreate() {
    ActivityScenario.launch(TestActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDepsFromModules.moduleFirstTypeDepLabel)
        .assertHasCustomInfo(0)

      it.recreate()

      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDepsFromModules.moduleFirstTypeDepLabel)
        .assertHasCustomInfo(0)
    }
  }

  @Test
  fun rememberComponentHost_multipleBindingsModuleSecondTypeRetainedDep_isRetainedAfterRecreate() {
    ActivityScenario.launch(TestActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDepsFromModules.moduleSecondTypeDepLabel)
        .assertHasCustomInfo(0)

      it.recreate()

      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDepsFromModules.moduleSecondTypeDepLabel)
        .assertHasCustomInfo(0)
    }
  }

  @Test
  fun rememberComponentHost_withQualifiedRetainedDepByProvides_isRetainedAfterRecreate() {
    ActivityScenario.launch(TestActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDepsFromModules.providedRetainedDepLabel)
        .assertHasCustomInfo(0)

      it.recreate()

      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDepsFromModules.providedRetainedDepLabel)
        .assertHasCustomInfo(0)
    }
  }

  @Test
  fun rememberComponentHost_withQualifiedRetainedDepByBinds_isRetainedAfterRecreate() {
    ActivityScenario.launch(TestActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDepsFromModules.boundRetainedDepLabel)
        .assertHasCustomInfo(0)

      it.recreate()

      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDepsFromModules.boundRetainedDepLabel)
        .assertHasCustomInfo(0)
    }
  }

  @Test
  fun rememberComponentHost_withNamedRetainedDep_isRetainedAfterRecreate() {
    ActivityScenario.launch(TestActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDepsFromModules.namedRetainedDepLabel)
        .assertHasCustomInfo(0)

      it.recreate()

      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDepsFromModules.namedRetainedDepLabel)
        .assertHasCustomInfo(0)
    }
  }

  @Test
  fun rememberComponentHost_moduleHasSameProvidesMethodNameAsAnother_injectsTypeCorrectly() {
    ActivityScenario.launch(TestActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDepsFromModules.otherNamedRetainedDepLabel)
        .assertHasCustomInfo(1)
    }
  }

  @Test
  fun rememberComponentHost_withNullableRetainedDep_isInjectedCorrectly() {
    ActivityScenario.launch(TestActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(ComposeHostWithRetainedDepsFromModules.nullableRetainedDepLabel)
        .assertHasCustomInfo(null)
    }
  }

  @Test
  fun rememberComponentHost_innerClassesWithSameName_injectsTypesCorrectly() {
    ActivityScenario.launch(TestActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(DuplicateInnerClassComposeHost.outerClass1InnerClassLabel)
        .assertHasCustomInfo(0)
      composeTestRule
        .onNodeWithLabel(DuplicateInnerClassComposeHost.outerClass2InnerClassLabel)
        .assertHasCustomInfo(0)
      composeTestRule
        .onNodeWithLabel(DuplicateInnerClassComposeHost.outerClass1ModuleProvidedTypeLabel)
        .assertHasCustomInfo(0)
      composeTestRule
        .onNodeWithLabel(DuplicateInnerClassComposeHost.outerClass2ModuleProvidedTypeLabel)
        .assertHasCustomInfo(0)
    }
  }

  @Test
  fun rememberComponentHost_twoOfTheSameHost_useCorrectRetainedComponent() {
    ActivityScenario.launch(EmptyTestActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        activity.setContent {
          Column {
            val host1 = rememberComponentHost(activity.keyInterferenceHost1Creator)
            host1.Composable()

            val host2 = rememberComponentHost(activity.keyInterferenceHost1Creator)
            host2.Composable()
          }
        }
      }

      composeTestRule
        .onAllNodesWithLabel(KeyInterferenceHost1.retainedScopedStateLabel)[0]
        .performClick()

      val nodes = composeTestRule.onAllNodesWithLabel(KeyInterferenceHost1.retainedScopedStateLabel)
      nodes[0].assertHasCustomInfo(1)
      nodes[1].assertHasCustomInfo(0)
    }
  }

  @Test
  fun rememberComponentHost_differentHosts_useCorrectRetainedComponent() {
    ActivityScenario.launch(EmptyTestActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        activity.setContent {
          Column {
            val host1 = rememberComponentHost(activity.keyInterferenceHost1Creator)
            host1.Composable()

            val host2 = rememberComponentHost(activity.keyInterferenceHost2Creator)
            host2.Composable()
          }
        }
      }

      composeTestRule.onNodeWithLabel(KeyInterferenceHost1.retainedScopedStateLabel).performClick()

      composeTestRule
        .onNodeWithLabel(KeyInterferenceHost1.retainedScopedStateLabel)
        .assertHasCustomInfo(1)
      composeTestRule
        .onNodeWithLabel(KeyInterferenceHost2.retainedScopedStateLabel)
        .assertHasCustomInfo(0)
    }
  }

  @Test
  fun rememberComponentHost_hostsRememberedBeforeCallingComposable_useCorrectRetainedComponent() {
    ActivityScenario.launch(EmptyTestActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        activity.setContent {
          Column {
            val host1 = rememberComponentHost(activity.keyInterferenceHost1Creator)
            val host2 = rememberComponentHost(activity.keyInterferenceHost1Creator)

            host1.Composable()
            host2.Composable()
          }
        }
      }

      composeTestRule
        .onAllNodesWithLabel(KeyInterferenceHost1.retainedScopedStateLabel)[0]
        .performClick()

      val nodes = composeTestRule.onAllNodesWithLabel(KeyInterferenceHost1.retainedScopedStateLabel)
      nodes[0].assertHasCustomInfo(1)
      nodes[1].assertHasCustomInfo(0)
    }
  }

  @Test
  fun rememberComopnentHost_hostsCreatedWithDelegate_useCorrectRetainedComponent() {
    ActivityScenario.launch(EmptyTestActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        activity.setContent {
          Column {
            activity.Delegate {
              val host1 = rememberComponentHost(activity.keyInterferenceHost1Creator)
              host1.Composable()
            }

            activity.Delegate {
              val host2 = rememberComponentHost(activity.keyInterferenceHost1Creator)
              host2.Composable()
            }
          }
        }
      }

      composeTestRule
        .onAllNodesWithLabel(KeyInterferenceHost1.retainedScopedStateLabel)[0]
        .performClick()

      val nodes = composeTestRule.onAllNodesWithLabel(KeyInterferenceHost1.retainedScopedStateLabel)
      nodes[0].assertHasCustomInfo(1)
      nodes[1].assertHasCustomInfo(0)
    }
  }

  @Test
  fun rememberComponentHost_hostsCreatedByMethodCallingDelegate_useCorrectRetainedComponent() {
    ActivityScenario.launch(EmptyTestActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        activity.setContent {
          Column {
            activity.CallHost(activity.keyInterferenceHost1Creator)
            activity.CallHost(activity.keyInterferenceHost1Creator)
          }
        }
      }

      composeTestRule
        .onAllNodesWithLabel(KeyInterferenceHost1.retainedScopedStateLabel)[0]
        .performClick()

      val nodes = composeTestRule.onAllNodesWithLabel(KeyInterferenceHost1.retainedScopedStateLabel)
      nodes[0].assertHasCustomInfo(1)
      nodes[1].assertHasCustomInfo(0)
    }
  }

  @Test
  fun rememberComponentHost_firstHostRemovedConditionally_useCorrectRetainedComponent() {
    ActivityScenario.launch(EmptyTestActivity::class.java).use { scenario ->
      val enableHostLabel = NodeLabel("EmptyKeyInterferenceTestActivity", "enableHost")

      scenario.onActivity { activity ->
        activity.setContent {
          Column {
            val enableHost = remember { mutableStateOf(true) }
            LabeledNode(
              enableHostLabel,
              Modifier.clickable { enableHost.value = !enableHost.value }
            )

            activity.ConditionalHost(enableHost.value, activity.keyInterferenceHost1Creator)
            activity.ConditionalHost(true, activity.keyInterferenceHost1Creator)
          }
        }
      }

      // Update each host's retained state so it's not the default value.
      composeTestRule
        .onAllNodesWithLabel(KeyInterferenceHost1.retainedScopedStateLabel)[0]
        .performClick()
      composeTestRule
        .onAllNodesWithLabel(KeyInterferenceHost1.retainedScopedStateLabel)[1]
        .performClick()
        .performClick()

      var nodes = composeTestRule.onAllNodesWithLabel(KeyInterferenceHost1.retainedScopedStateLabel)
      nodes[0].assertHasCustomInfo(1)
      nodes[1].assertHasCustomInfo(2)

      // Disable the first host. This should remove its retained component, leaving only the second
      composeTestRule.onNodeWithLabel(enableHostLabel).performClick()
      composeTestRule
        .onNodeWithLabel(KeyInterferenceHost1.retainedScopedStateLabel)
        .assertHasCustomInfo(2)

      // Re-enable the first host. Since it was fully removed, doing so should create a new retained
      // component and its retained state should be reset to 0
      composeTestRule.onNodeWithLabel(enableHostLabel).performClick()

      nodes = composeTestRule.onAllNodesWithLabel(KeyInterferenceHost1.retainedScopedStateLabel)
      nodes[0].assertHasCustomInfo(0)
      nodes[1].assertHasCustomInfo(2)
    }
  }

  @Test
  fun rememberComponentHost_secondHostRemovedConditionally_useCorrectRetainedComponent() {
    ActivityScenario.launch(EmptyTestActivity::class.java).use { scenario ->
      val enableHostLabel = NodeLabel("EmptyKeyInterferenceTestActivity", "enableHost")

      scenario.onActivity { activity ->
        activity.setContent {
          Column {
            val enableHost = remember { mutableStateOf(true) }
            LabeledNode(
              enableHostLabel,
              Modifier.clickable { enableHost.value = !enableHost.value }
            )

            activity.ConditionalHost(true, activity.keyInterferenceHost1Creator)
            activity.ConditionalHost(enableHost.value, activity.keyInterferenceHost1Creator)
          }
        }
      }

      // Update each host's retained state so it's not the default value.
      composeTestRule
        .onAllNodesWithLabel(KeyInterferenceHost1.retainedScopedStateLabel)[0]
        .performClick()
      composeTestRule
        .onAllNodesWithLabel(KeyInterferenceHost1.retainedScopedStateLabel)[1]
        .performClick()
        .performClick()

      var nodes = composeTestRule.onAllNodesWithLabel(KeyInterferenceHost1.retainedScopedStateLabel)
      nodes[0].assertHasCustomInfo(1)
      nodes[1].assertHasCustomInfo(2)

      // Disable the second host. This should remove its retained component, leaving only the first
      composeTestRule.onNodeWithLabel(enableHostLabel).performClick()

      composeTestRule
        .onNodeWithLabel(KeyInterferenceHost1.retainedScopedStateLabel)
        .assertHasCustomInfo(1)

      // Re-enable the second host. Since the it was fully removed, doing so should create a new
      // retained component and its retained state should be reset to 0
      composeTestRule.onNodeWithLabel(enableHostLabel).performClick()

      nodes = composeTestRule.onAllNodesWithLabel(KeyInterferenceHost1.retainedScopedStateLabel)
      nodes[0].assertHasCustomInfo(1)
      nodes[1].assertHasCustomInfo(0)
    }
  }

  @Test
  fun rememberComponentHost_keyInterferenceWithRecreate_usesCorrectRetainedComponent() {
    ActivityScenario.launch(KeyInterferenceTestActivity::class.java).use {
      var nodes = composeTestRule.onAllNodesWithLabel(KeyInterferenceHost1.retainedScopedStateLabel)
      nodes[0].performClick()
      nodes[0].assertHasCustomInfo(1)
      nodes[1].assertHasCustomInfo(0)

      it.recreate()

      nodes = composeTestRule.onAllNodesWithLabel(KeyInterferenceHost1.retainedScopedStateLabel)
      nodes[0].assertHasCustomInfo(1)
      nodes[1].assertHasCustomInfo(0)
    }
  }

  @Test
  fun rememberComponentHost_onRecomposition_onClearedListenerNotCalled() {
    ActivityScenario.launch(TestActivity::class.java).use { scenario ->
      composeTestRule
        .onNodeWithLabel(RetainedComponentClearedTracker.clickToResetLabel)
        .performScrollTo()
        .performClick()
      assertThat(isOnClearedComposeHostRetainedLifecycleCleared.get()).isFalse()

      scenario.onActivity { activity -> activity.triggerRecomposition() }
      composeTestRule.onNodeWithLabel(TestActivity.recompositionCountLabel).assertHasCustomInfo(1)

      assertThat(isOnClearedComposeHostRetainedLifecycleCleared.get()).isFalse()
    }
  }

  @Test
  fun rememberComponentHost_onActivityRecreate_onClearedListenerNotCalled() {
    ActivityScenario.launch(TestActivity::class.java).use {
      composeTestRule
        .onNodeWithLabel(RetainedComponentClearedTracker.clickToResetLabel)
        .performScrollTo()
        .performClick()
      assertThat(isOnClearedComposeHostRetainedLifecycleCleared.get()).isFalse()

      it.recreate()

      assertThat(isOnClearedComposeHostRetainedLifecycleCleared.get()).isFalse()
    }
  }

  @Test
  fun rememberComponentHost_hostRemovedFromComposition_onClearedListenerIsCalled() {
    ActivityScenario.launch(TestActivity::class.java).use { scenario ->
      composeTestRule
        .onNodeWithLabel(RetainedComponentClearedTracker.clickToResetLabel)
        .performScrollTo()
        .performClick()
      assertThat(isOnClearedComposeHostRetainedLifecycleCleared.get()).isFalse()

      scenario.onActivity { activity -> activity.setOnClearedComposeHostEnabled(false) }
      composeTestRule
        .onNodeWithLabel(RetainedComponentClearedTracker.clickToResetLabel)
        .assertDoesNotExist()

      assertThat(isOnClearedComposeHostRetainedLifecycleCleared.get()).isTrue()
    }
  }

  @Test
  fun rememberComponentHost_activityDestroyed_onClearedListenerIsCalled() {
    ActivityScenario.launch(TestActivity::class.java).use { scenario ->
      composeTestRule
        .onNodeWithLabel(RetainedComponentClearedTracker.clickToResetLabel)
        .performScrollTo()
        .performClick()
      assertThat(isOnClearedComposeHostRetainedLifecycleCleared.get()).isFalse()

      scenario.moveToState(Lifecycle.State.DESTROYED)

      assertThat(isOnClearedComposeHostRetainedLifecycleCleared.get()).isTrue()
    }
  }

  @Test
  fun rememberComponentHost_onClearedCalled_noInterferenceBetweenHosts() {
    ActivityScenario.launch(TestActivity::class.java).use { scenario ->
      composeTestRule
        .onNodeWithLabel(RetainedComponentClearedTracker.clickToResetLabel)
        .performScrollTo()
        .performClick()
      composeTestRule
        .onNodeWithLabel(RetainedComponentClearedTracker2.clickToResetLabel)
        .performScrollTo()
        .performClick()
      assertThat(isOnClearedComposeHostRetainedLifecycleCleared.get()).isFalse()
      assertThat(isOnClearedComposeHost2RetainedLifecycleCleared.get()).isFalse()

      scenario.onActivity { activity -> activity.setOnClearedComposeHostEnabled(false) }
      composeTestRule
        .onNodeWithLabel(RetainedComponentClearedTracker.clickToResetLabel)
        .assertDoesNotExist()

      // Since the first host is removed, its retained lifecycle should be cleared. The second host
      // is not removed, so its retained lifecycle should not be cleared
      assertThat(isOnClearedComposeHostRetainedLifecycleCleared.get()).isTrue()
      assertThat(isOnClearedComposeHost2RetainedLifecycleCleared.get()).isFalse()
    }
  }

  @Test
  fun rememberComponentHost_compositionRemovedDuringRecreate_onClearedListenerIsCalled() {
    ActivityScenario.launch(RecreateTestActivity::class.java).use {
      // Enable the host as it's disabled by default
      composeTestRule.onNodeWithLabel(RecreateTestActivity.enableHostLabel).performClick()

      composeTestRule
        .onNodeWithLabel(RetainedComponentClearedTracker.clickToResetLabel)
        .performClick()
      assertThat(isOnClearedComposeHostRetainedLifecycleCleared.get()).isFalse()

      // Recreate the activity, which resets the enableHost value to false. As a result the host is
      // removed from the composition.
      it.recreate()

      // Since the host was removed from the composition, its retained lifecycle should be cleared
      assertThat(isOnClearedComposeHostRetainedLifecycleCleared.get()).isTrue()
    }
  }

  @Test
  fun rememberComponentHost_lazyColumnHost_notClearedAfterRecreate() {
    val defaultEnableHostIntent =
      Intent(appContext, LazyColumnTestActivity::class.java).apply {
        putExtra(LazyColumnTestActivity.enableHostIntentKey, true)
      }

    ActivityScenario.launch<LazyColumnTestActivity>(defaultEnableHostIntent).use {
      composeTestRule
        .onNodeWithLabel(RetainedComponentClearedTracker.clickToResetLabel)
        .performClick()
      assertThat(isOnClearedComposeHostRetainedLifecycleCleared.get()).isFalse()

      // Recreate the activity, this should restore the host's ComposeRetainedComponent
      it.recreate()

      // Since the host wasn't removed from the composition, its retained lifecycle should not be
      // cleared
      assertThat(isOnClearedComposeHostRetainedLifecycleCleared.get()).isFalse()
    }
  }

  @Test
  fun rememberComponentHost_lazyColumnCompositionRemovedDuringRecreate_callsOnClearedListener() {
    ActivityScenario.launch(LazyColumnTestActivity::class.java).use {
      // Enable the host as it's disabled by default
      composeTestRule.onNodeWithLabel(LazyColumnTestActivity.enableHostLabel).performClick()

      composeTestRule
        .onNodeWithLabel(RetainedComponentClearedTracker.clickToResetLabel)
        .performClick()
      assertThat(isOnClearedComposeHostRetainedLifecycleCleared.get()).isFalse()

      // Recreate the activity, which resets the enableHost value to false. As a result the host is
      // removed from the composition.
      it.recreate()

      // Since the host was removed from the composition, its retained lifecycle should be cleared
      assertThat(isOnClearedComposeHostRetainedLifecycleCleared.get()).isTrue()
    }
  }

  @Test
  fun rememberComponentHost_withRememberSaveableVariable_variableIsRestored() {
    ActivityScenario.launch(TestActivity::class.java).use {
      composeTestRule.onNodeWithLabel(ComposeHost.rememberSaveableValueLabel).performClick()
      composeTestRule.onNodeWithLabel(ComposeHost.rememberSaveableValueLabel).assertHasCustomInfo(1)

      it.recreate()

      composeTestRule.onNodeWithLabel(ComposeHost.rememberSaveableValueLabel).assertHasCustomInfo(1)
    }
  }

  @AndroidEntryPoint(ComponentActivity::class)
  class TestActivity : Hilt_RememberComponentHostTest_TestActivity() {
    private lateinit var viewModel: TestActivityViewModel

    @Inject lateinit var composeHostCreator: ComponentHostCreator<ComposeHost>
    @Inject lateinit var composeHostCreator2: ComponentHostCreator<ComposeHost2>

    @Inject lateinit var nestedComponentHost: ComponentHostCreator<NestedComponentHost>

    @Inject
    lateinit var composeHostWithRetainedDepCreator: ComponentHostCreator<ComposeHostWithRetainedDep>

    @Inject
    lateinit var composeHostWithRetainedDepsFromModulesCreator:
      ComponentHostCreator<ComposeHostWithRetainedDepsFromModules>

    @Inject
    lateinit var duplicateInnerClassComposeHostCreator:
      ComponentHostCreator<DuplicateInnerClassComposeHost>

    @Inject lateinit var onClearedComposeHostCreator: ComponentHostCreator<OnClearedComposeHost>

    @Inject lateinit var onClearedComposeHost2Creator: ComponentHostCreator<OnClearedComposeHost2>

    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      viewModel = ViewModelProvider(this)[TestActivityViewModel::class.java]

      setContent {
        Column {
          LabeledNode(
            recompositionCountLabel,
            Modifier.customInfo(viewModel.referenceMeToBeRecomposed)
          )
          RecomposableContent(viewModel.referenceMeToBeRecomposed)
        }
      }
    }

    /**
     * referenceMeToBeRecomposed is accepted as a parameter so this function is recomposed when
     * [triggerRecomposition] is called.
     */
    @Composable
    fun RecomposableContent(@Suppress("UNUSED_PARAMETER") referenceMeToBeRecomposed: Int) {
      // There's enough items in this Column to push the later ones "off screen". This prevents
      // Compose from being able to click on them, so we need the column to be scrollable so we can
      // first scroll to the item and then click on it.
      Column(Modifier.verticalScroll(rememberScrollState())) {
        val creator = rememberComponentHost(composeHostCreator)
        creator.Composable()

        val creator2 = rememberComponentHost(composeHostCreator2)
        creator2.Composable()

        val nestedCreator = rememberComponentHost(nestedComponentHost)
        nestedCreator.NestedComposable()

        if (viewModel.isComposeHostWithRetainedDepEnabled) {
          val composeHostWithRetainedDep = rememberComponentHost(composeHostWithRetainedDepCreator)
          composeHostWithRetainedDep.Composable()
        }

        val composeHostWithRetainedDepFromModules =
          rememberComponentHost(composeHostWithRetainedDepsFromModulesCreator)
        composeHostWithRetainedDepFromModules.Composable()

        val duplicateInnerClassComposeHost =
          rememberComponentHost(duplicateInnerClassComposeHostCreator)
        duplicateInnerClassComposeHost.Composable()

        if (viewModel.isOnClearedComposeHostEnabled) {
          val onClearedComposeHost = rememberComponentHost(onClearedComposeHostCreator)
          onClearedComposeHost.Composable()
        }

        val onClearedComposeHost2 = rememberComponentHost(onClearedComposeHost2Creator)
        onClearedComposeHost2.Composable()
      }
    }

    internal fun triggerRecomposition() {
      viewModel.referenceMeToBeRecomposed = viewModel.referenceMeToBeRecomposed + 1
    }

    internal fun setComposeHostWithRetainedDepEnabled(enabled: Boolean) {
      viewModel.isComposeHostWithRetainedDepEnabled = enabled
    }

    internal fun setOnClearedComposeHostEnabled(enabled: Boolean) {
      viewModel.isOnClearedComposeHostEnabled = enabled
    }

    @HiltViewModel
    class TestActivityViewModel @Inject constructor() : ViewModel() {
      var referenceMeToBeRecomposed by mutableStateOf(0)
      var isComposeHostWithRetainedDepEnabled by mutableStateOf(true)
      var isOnClearedComposeHostEnabled by mutableStateOf(true)
    }

    companion object {
      val recompositionCountLabel = NodeLabel("TestActivity", "recomposition count")
    }

    @ComposeComponentHost
    class NestedComponentHost @Inject constructor() :
      Hilt_RememberComponentHostTest_TestActivity_NestedComponentHost() {
      @Composable
      fun NestedComposable() {
        LabeledNode(nestedHostLabel)
      }

      companion object {
        val nestedHostLabel = NodeLabel("NestedComponentHost", "NestedHost")
      }
    }
  }

  @AndroidEntryPoint(ComponentActivity::class)
  class MultipleComposeViewActivity : Hilt_RememberComponentHostTest_MultipleComposeViewActivity() {
    @Inject lateinit var hostWithRetainedDep: ComponentHostCreator<ComposeHostWithRetainedDep>

    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      setContentView(
        LinearLayout(this).apply {
          layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
          addView(createComposeView(R.id.compose_view1))
          addView(createComposeView(R.id.compose_view2))
        }
      )
    }

    private fun createComposeView(id: Int): ComposeView {
      val composeView = ComposeView(this)
      composeView.id = id
      composeView.setContent {
        val enableHost = remember { mutableStateOf(true) }
        Text(
          "click to toggle composeHostWithRetainedDep enabled",
          Modifier.clickable { enableHost.value = !enableHost.value }
        )

        if (enableHost.value) {
          val host = rememberComponentHost(hostWithRetainedDep)
          host.Composable()
        }
      }
      return composeView
    }
  }

  /**
   * Empty Activity which just injects hosts. Tests should manually set the content on the activity:
   * ```kotlin
   * ActivityScenario.launch(KeyInterferenceTestActivity::class.java).use { scenario ->
   *   scenario.onActivity { activity ->
   *     activity.setContent {...}
   *   }
   *
   *   // Perform test...
   * }
   * ```
   *
   * Note that this will not work well with tests which need to recreate the Activity since the call
   * to setContent is lost after the recreation.
   */
  @AndroidEntryPoint(ComponentActivity::class)
  class EmptyTestActivity : Hilt_RememberComponentHostTest_EmptyTestActivity() {
    @Inject lateinit var keyInterferenceHost1Creator: ComponentHostCreator<KeyInterferenceHost1>
    @Inject lateinit var keyInterferenceHost2Creator: ComponentHostCreator<KeyInterferenceHost2>

    @Composable fun Delegate(impl: @Composable () -> Unit) = impl()

    @Composable
    fun CallHost(creator: ComponentHostCreator<HasComposable>): Unit = Delegate {
      val host = rememberComponentHost(creator)
      host.Composable()
    }

    @Composable
    fun ConditionalHost(enable: Boolean, creator: ComponentHostCreator<HasComposable>) {
      if (enable) {
        val host = rememberComponentHost(creator)
        host.Composable()
      }
    }
  }

  @AndroidEntryPoint(ComponentActivity::class)
  class KeyInterferenceTestActivity : Hilt_RememberComponentHostTest_KeyInterferenceTestActivity() {
    @Inject lateinit var keyInterferenceHost1Creator: ComponentHostCreator<KeyInterferenceHost1>

    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      setContent {
        Column {
          val host1 = rememberComponentHost(keyInterferenceHost1Creator)
          host1.Composable()

          val host2 = rememberComponentHost(keyInterferenceHost1Creator)
          host2.Composable()
        }
      }
    }
  }

  @AndroidEntryPoint(ComponentActivity::class)
  class RecreateTestActivity : Hilt_RememberComponentHostTest_RecreateTestActivity() {
    @Inject lateinit var onClearedHostCreator: ComponentHostCreator<OnClearedComposeHost>

    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      setContent {
        Column {
          val enableHost = remember { mutableStateOf(false) }

          LabeledNode(enableHostLabel, Modifier.clickable { enableHost.value = !enableHost.value })

          if (enableHost.value) {
            val host = rememberComponentHost(onClearedHostCreator)
            host.Composable()
          }
        }
      }
    }

    companion object {
      val enableHostLabel = NodeLabel("RecreateTestActivity", "enableHost")
    }
  }

  @AndroidEntryPoint(ComponentActivity::class)
  class LazyColumnTestActivity : Hilt_RememberComponentHostTest_LazyColumnTestActivity() {
    @Inject lateinit var onClearedHostCreator: ComponentHostCreator<OnClearedComposeHost>

    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      val defaultEnableHost = intent.getBooleanExtra(enableHostIntentKey, false)

      setContent {
        val enableHost = remember { mutableStateOf(defaultEnableHost) }
        LazyColumn {
          item {
            LabeledNode(
              enableHostLabel,
              Modifier.clickable { enableHost.value = !enableHost.value }
            )
          }
          item {
            if (enableHost.value) {
              val host = rememberComponentHost(onClearedHostCreator)
              host.Composable()
            }
          }
        }
      }
    }

    companion object {
      val enableHostLabel = NodeLabel("LazyColumnTestActivity", "enableHost")

      const val enableHostIntentKey = "enable_host"
    }
  }
}

@ComposeComponentHost
class ComposeHost
@Inject
constructor(
  typeBasedCounter: TypeBasedCounter,
  private val dep: UnscopedDep,
  private val dep2: UnscopedDep2,
) : Hilt_ComposeHost() {
  private val instanceNum: Int = typeBasedCounter.getAndIncrementCountFor<ComposeHost>()

  @Composable
  fun Composable() {
    Column {
      LabeledNode(hostLabel, Modifier.customInfo(instanceNum))

      LabeledNode(dep1ScopedDepLabel, Modifier.customInfo(dep.scopedDep.instanceNum))

      LabeledNode(dep2ScopedDepLabel, Modifier.customInfo(dep2.scopedDep.instanceNum))

      var rememberSaveableValue by rememberSaveable { mutableIntStateOf(0) }
      LabeledNode(
        rememberSaveableValueLabel,
        Modifier.clickable { ++rememberSaveableValue }.customInfo(rememberSaveableValue)
      )
    }
  }

  companion object {
    val hostLabel = NodeLabel("ComposeHost", "Host")
    val dep1ScopedDepLabel = NodeLabel("ComposeHost", "Dep1", "ScopedDep")
    val dep2ScopedDepLabel = NodeLabel("ComposeHost", "Dep2", "ScopedDep")
    val rememberSaveableValueLabel = NodeLabel("ComposeHost", "rememberSaveableValue")
  }
}

class UnscopedDep @Inject internal constructor(val scopedDep: ComposeScopedDep)

class UnscopedDep2 @Inject internal constructor(val scopedDep: ComposeScopedDep)

@ComposeScoped
class ComposeScopedDep @Inject internal constructor(typeBasedCounter: TypeBasedCounter) {
  val instanceNum: Int = typeBasedCounter.getAndIncrementCountFor<ComposeScopedDep>()
}

@ComposeComponentHost
class ComposeHost2 @Inject constructor(private val scopedDep: ComposeScopedDep) :
  Hilt_ComposeHost2() {
  @Composable
  fun Composable() {
    LabeledNode(scopedDepLabel, Modifier.customInfo(scopedDep.instanceNum))
  }

  companion object {
    val scopedDepLabel = NodeLabel("ComposeHost2", "ScopedDep")
  }
}

@ComposeComponentHost
class ComposeHostWithRetainedDep
@Inject
constructor(
  private val usesRetainedDep: UsesRetainedDep,
  private val usesRetainedDep2: UsesRetainedDep2
) : Hilt_ComposeHostWithRetainedDep() {

  @Composable
  fun Composable() {
    Column {
      LabeledNode(usesRetainedDepLabel, Modifier.customInfo(usesRetainedDep.instanceNum))

      LabeledNode(
        usesRetainedDepRetainedDepLabel,
        Modifier.customInfo(usesRetainedDep.calculateRetainedDepInstanceNum())
      )

      LabeledNode(
        usesRetainedDep2RetainedDepLabel,
        Modifier.customInfo(usesRetainedDep2.calculateRetainedDepInstanceNum())
      )
    }
  }

  companion object {
    val usesRetainedDepLabel = NodeLabel("ComposeHostWithRetainedDep", "UsesRetainedDep")
    val usesRetainedDepRetainedDepLabel =
      NodeLabel("ComposeHostWithRetainedDep", "UsesRetainedDep", "RetainedDep")
    val usesRetainedDep2RetainedDepLabel =
      NodeLabel("ComposeHostWithRetainedDep", "UsesRetainedDep2", "RetainedDep")
  }
}

@ComposeScoped
class UsesRetainedDep
@Inject
internal constructor(typeBasedCounter: TypeBasedCounter, private val retainedDep: RetainedDep) {

  val instanceNum = typeBasedCounter.getAndIncrementCountFor<UsesRetainedDep>()

  fun calculateRetainedDepInstanceNum(): Int {
    return retainedDep.instanceNum
  }
}

@ComposeScoped
class UsesRetainedDep2 @Inject internal constructor(private val retainedDep: RetainedDep) {

  fun calculateRetainedDepInstanceNum(): Int {
    return retainedDep.instanceNum
  }
}

@ComposeRetainedScoped
@ComposeRetainedProvided
class RetainedDep @Inject constructor(typeBasedCounter: TypeBasedCounter) {
  val instanceNum: Int = typeBasedCounter.getAndIncrementCountFor<RetainedDep>()
}

@ComposeComponentHost
class ComposeHostWithRetainedDepsFromModules
@Inject
constructor(
  private val nonQualifiedType: NonQualifiedType,
  private val parameterizedType: ParameterizedProvidedType<String>,
  private val primitiveType: Int,
  private val providerPrimitiveType: Provider<Int>,
  @FirstType private val moduleFirstType: FirstTypeClass,
  @SecondType private val moduleSecondType: SecondTypeClass,
  @ByProvides private val providedDep: ProvidedRetainedDep,
  @ByBinds private val boundDep: BoundRetainedDep,
  @Named("NamedRetainedDep") private val namedDep: NamedRetainedDep,
  @Named("OtherNamedRetainedDep") private val otherNamedDep: NamedRetainedDep,
  private val nullableDep: NullableRetainedDep?,
) : Hilt_ComposeHostWithRetainedDepsFromModules() {

  @Composable
  fun Composable() {
    Column {
      LabeledNode(nonQualifiedTypeDepLabel, Modifier.customInfo(nonQualifiedType.instanceNum))

      LabeledNode(parameterizedTypeDepLabel, Modifier.customInfo(parameterizedType.instanceNum))

      LabeledNode(primitiveTypeDepLabel, Modifier.customInfo(primitiveType))

      LabeledNode(providerPrimitiveTypeDepLabel, Modifier.customInfo(providerPrimitiveType.get()))

      LabeledNode(moduleFirstTypeDepLabel, Modifier.customInfo(moduleFirstType.instanceNum))

      LabeledNode(moduleSecondTypeDepLabel, Modifier.customInfo(moduleSecondType.instanceNum))

      LabeledNode(providedRetainedDepLabel, Modifier.customInfo(providedDep.instanceNum))

      LabeledNode(boundRetainedDepLabel, Modifier.customInfo(boundDep.instanceNum))

      LabeledNode(namedRetainedDepLabel, Modifier.customInfo(namedDep.instanceNum))

      LabeledNode(otherNamedRetainedDepLabel, Modifier.customInfo(otherNamedDep.instanceNum))

      LabeledNode(nullableRetainedDepLabel, Modifier.customInfo(nullableDep))
    }
  }

  companion object {
    val nonQualifiedTypeDepLabel =
      NodeLabel("ComposeHostWithQualifiedRetainedDeps", "NonQualifiedType")
    val parameterizedTypeDepLabel =
      NodeLabel("ComposeHostWithQualifiedRetainedDeps", "ParameterizedProvidedType<String>")
    val primitiveTypeDepLabel = NodeLabel("ComposeHostWithQualifiedRetainedDeps", "Integer")
    val providerPrimitiveTypeDepLabel =
      NodeLabel("ComposeHostWithQualifiedRetainedDeps", "Provider<Integer>")
    val moduleFirstTypeDepLabel =
      NodeLabel("ComposeHostWithQualifiedRetainedDeps", "@FirstType FirstTypeClass")
    val moduleSecondTypeDepLabel =
      NodeLabel("ComposeHostWithQualifiedRetainedDeps", "@SecondType SecondTypeClass")
    val providedRetainedDepLabel =
      NodeLabel("ComposeHostWithQualifiedRetainedDeps", "@ByProvides ProvidedRetainedDep")
    val boundRetainedDepLabel =
      NodeLabel("ComposeHostWithQualifiedRetainedDeps", "@ByBinds BoundRetainedDep")
    val namedRetainedDepLabel =
      NodeLabel("ComposeHostWithQualifiedRetainedDeps", "@Named(NamedRetainedDep) NamedRetainedDep")
    val otherNamedRetainedDepLabel =
      NodeLabel(
        "ComposeHostWithQualifiedRetainedDeps",
        "@Named(OtherNamedRetainedDep) NamedRetainedDep"
      )
    val nullableRetainedDepLabel =
      NodeLabel("ComposeHostWithQualifiedRetainedDeps", "NullableRetainedDep")
  }
}

class NonQualifiedType(typeBasedCounter: TypeBasedCounter) {
  val instanceNum = typeBasedCounter.getAndIncrementCountFor<NonQualifiedType>()
}

@Module
@InstallIn(ComposeRetainedComponent::class)
internal object ProvidesNonQualifiedRetainedDepModule {

  @Provides
  @ComposeRetainedScoped
  @ComposeRetainedProvided
  fun provideNonQualifiedDep(typeBasedCounter: TypeBasedCounter): NonQualifiedType {
    return NonQualifiedType(typeBasedCounter)
  }
}

class ParameterizedProvidedType<T>(typeBasedCounter: TypeBasedCounter) {
  val instanceNum = typeBasedCounter.getAndIncrementCountFor<ParameterizedProvidedType<T>>()
}

@Module
@InstallIn(ComposeRetainedComponent::class)
internal object ProvidesParameterizedRetainedDepTypeModule {

  @Provides
  @ComposeRetainedScoped
  @ComposeRetainedProvided
  fun provideParameterizedType(
    typeBasedCounter: TypeBasedCounter
  ): ParameterizedProvidedType<String> {
    return ParameterizedProvidedType(typeBasedCounter)
  }
}

@Module
@InstallIn(ComposeRetainedComponent::class)
internal object ProvidesPrimitiveRetainedDepTypeModule {

  @Provides @ComposeRetainedScoped @ComposeRetainedProvided fun providePrimitiveType(): Int = 5
}

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class FirstType

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class SecondType

@Module
@InstallIn(ComposeRetainedComponent::class)
internal object ProvidesMultipleRetainedDepsModule {

  @Provides
  @FirstType
  @ComposeRetainedScoped
  @ComposeRetainedProvided
  fun provideFirstType(typeBasedCounter: TypeBasedCounter) = FirstTypeClass(typeBasedCounter)

  @Provides
  @SecondType
  @ComposeRetainedScoped
  @ComposeRetainedProvided
  fun provideSecondType(typeBasedCounter: TypeBasedCounter) = SecondTypeClass(typeBasedCounter)
}

class FirstTypeClass constructor(typeBasedCounter: TypeBasedCounter) {
  val instanceNum = typeBasedCounter.getAndIncrementCountFor<FirstTypeClass>()
}

class SecondTypeClass constructor(typeBasedCounter: TypeBasedCounter) {
  val instanceNum = typeBasedCounter.getAndIncrementCountFor<SecondTypeClass>()
}

@Module
@InstallIn(ComposeRetainedComponent::class)
internal object ProvidesProvidedRetainedDepModule {
  @Provides
  @ByProvides
  @ComposeRetainedScoped
  @ComposeRetainedProvided
  fun provideProvidedRetainedDep(typeBasedCounter: TypeBasedCounter): ProvidedRetainedDep {
    return ProvidedRetainedDep(typeBasedCounter)
  }
}

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ByProvides

class ProvidedRetainedDep @Inject constructor(typeBasedCounter: TypeBasedCounter) {
  val instanceNum = typeBasedCounter.getAndIncrementCountFor<ProvidedRetainedDep>()
}

@Module
@InstallIn(ComposeRetainedComponent::class)
internal abstract class BindsBoundRetainedDepModule {

  @Binds
  @ByBinds
  @ComposeRetainedScoped
  @ComposeRetainedProvided
  abstract fun bindBoundRetainedDep(instance: BoundRetainedDep): BoundRetainedDep
}

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ByBinds

@ComposeRetainedScoped
class BoundRetainedDep @Inject constructor(typeBasedCounter: TypeBasedCounter) {
  val instanceNum = typeBasedCounter.getAndIncrementCountFor<BoundRetainedDep>()
}

@Module
@InstallIn(ComposeRetainedComponent::class)
internal object NamedRetainedDepModule {
  @Provides
  @Named("NamedRetainedDep")
  @ComposeRetainedScoped
  @ComposeRetainedProvided
  fun provideNamedRetainedDep(typeBasedCounter: TypeBasedCounter): NamedRetainedDep {
    return NamedRetainedDep(typeBasedCounter)
  }
}

class NamedRetainedDep(typeBasedCounter: TypeBasedCounter) {
  val instanceNum = typeBasedCounter.getAndIncrementCountFor<NamedRetainedDep>()
}

@Module
@InstallIn(ComposeRetainedComponent::class)
internal object NamedRetainedDepModuleWithSameProvidesMethodName {
  @Provides
  @Named("OtherNamedRetainedDep")
  @ComposeRetainedScoped
  @ComposeRetainedProvided
  fun provideNamedRetainedDep(typeBasedCounter: TypeBasedCounter): NamedRetainedDep {
    return NamedRetainedDep(typeBasedCounter)
  }
}

@Module
@InstallIn(ComposeRetainedComponent::class)
internal object NullableRetainedDepModule {

  @Provides
  @Nullable
  @ComposeRetainedScoped
  @ComposeRetainedProvided
  fun provideNullableType(): NullableRetainedDep? = null
}

@ComposeComponentHost
class DuplicateInnerClassComposeHost
@Inject
constructor(
  private val outerClass1InnerClass: OuterClass1.InnerClass,
  private val outerClass2InnerClass: OuterClass2.InnerClass,
  private val outerClass1ModuleProvidedType: OuterClass1InnerModuleProvidedType,
  private val outerClass2ModuleProvidedType: OuterClass2InnerModuleProvidedType,
) : Hilt_DuplicateInnerClassComposeHost() {
  @Composable
  fun Composable() {
    Column {
      LabeledNode(
        outerClass1InnerClassLabel,
        Modifier.customInfo(outerClass1InnerClass.instanceNum)
      )
      LabeledNode(
        outerClass2InnerClassLabel,
        Modifier.customInfo(outerClass2InnerClass.instanceNum)
      )
      LabeledNode(
        outerClass1ModuleProvidedTypeLabel,
        Modifier.customInfo(outerClass1ModuleProvidedType.instanceNum)
      )
      LabeledNode(
        outerClass2ModuleProvidedTypeLabel,
        Modifier.customInfo(outerClass2ModuleProvidedType.instanceNum)
      )
    }
  }

  companion object {
    val outerClass1InnerClassLabel =
      NodeLabel("DuplicateInnerClassComposeHost", "outerClass1InnerClass")
    val outerClass2InnerClassLabel =
      NodeLabel("DuplicateInnerClassComposeHost", "outerClass2InnerClass")
    val outerClass1ModuleProvidedTypeLabel =
      NodeLabel("DuplicateInnerClassComposeHost", "outerClass1ModuleProvidedType")
    val outerClass2ModuleProvidedTypeLabel =
      NodeLabel("DuplicateInnerClassComposeHost", "outerClass2ModuleProvidedType")
  }
}

class NullableRetainedDep

class OuterClass1 {
  @ComposeRetainedScoped
  @ComposeRetainedProvided
  class InnerClass @Inject constructor(typeBasedCounter: TypeBasedCounter) {
    val instanceNum = typeBasedCounter.getAndIncrementCountFor<OuterClass1.InnerClass>()
  }

  @Module
  @InstallIn(ComposeRetainedComponent::class)
  internal object InnerModule {
    @Provides
    @ComposeRetainedScoped
    @ComposeRetainedProvided
    fun provide(typeBasedCounter: TypeBasedCounter) =
      OuterClass1InnerModuleProvidedType(typeBasedCounter)
  }
}

class OuterClass2 {
  @ComposeRetainedScoped
  @ComposeRetainedProvided
  class InnerClass @Inject constructor(typeBasedCounter: TypeBasedCounter) {
    val instanceNum = typeBasedCounter.getAndIncrementCountFor<OuterClass2.InnerClass>()
  }

  @Module
  @InstallIn(ComposeRetainedComponent::class)
  internal object InnerModule {
    @Provides
    @ComposeRetainedScoped
    @ComposeRetainedProvided
    fun provide(typeBasedCounter: TypeBasedCounter) =
      OuterClass2InnerModuleProvidedType(typeBasedCounter)
  }
}

class OuterClass1InnerModuleProvidedType(typeBasedCounter: TypeBasedCounter) {
  val instanceNum = typeBasedCounter.getAndIncrementCountFor<OuterClass1InnerModuleProvidedType>()
}

class OuterClass2InnerModuleProvidedType(typeBasedCounter: TypeBasedCounter) {
  val instanceNum = typeBasedCounter.getAndIncrementCountFor<OuterClass2InnerModuleProvidedType>()
}

@ComposeComponentHost
class OnClearedComposeHost
@Inject
constructor(private val retainedComponentClearedTracker: RetainedComponentClearedTracker) :
  Hilt_OnClearedComposeHost() {

  @Composable
  fun Composable() {
    retainedComponentClearedTracker.Content()
  }

  @Qualifier annotation class RetainedLifecycleCleared
}

@ComposeComponentHost
class OnClearedComposeHost2
@Inject
constructor(private val retainedComponentClearedTracker: RetainedComponentClearedTracker2) :
  Hilt_OnClearedComposeHost2() {

  @Composable
  fun Composable() {
    retainedComponentClearedTracker.Content()
  }

  @Qualifier annotation class RetainedLifecycleCleared
}

@ComposeRetainedScoped
@ComposeRetainedProvided
class RetainedComponentClearedTracker
@Inject
internal constructor(
  composeRetainedLifecycle: ComposeRetainedLifecycle,
  @OnClearedComposeHost.RetainedLifecycleCleared
  private val retainedLifecycleCleared: AtomicBoolean,
) {

  init {
    composeRetainedLifecycle.addOnClearedListener { retainedLifecycleCleared.set(true) }
  }

  @Composable
  fun Content() {
    LabeledNode(clickToResetLabel, Modifier.clickable { retainedLifecycleCleared.set(false) })
  }

  companion object {
    val clickToResetLabel =
      NodeLabel("OnClearedHost", "RetainedComponentClearedTracker", "clickToReset")
  }
}

@ComposeRetainedScoped
@ComposeRetainedProvided
class RetainedComponentClearedTracker2
@Inject
internal constructor(
  composeRetainedLifecycle: ComposeRetainedLifecycle,
  @OnClearedComposeHost2.RetainedLifecycleCleared
  private val retainedLifecycleCleared: AtomicBoolean,
) {

  init {
    composeRetainedLifecycle.addOnClearedListener { retainedLifecycleCleared.set(true) }
  }

  @Composable
  fun Content() {
    LabeledNode(clickToResetLabel, Modifier.clickable { retainedLifecycleCleared.set(false) })
  }

  companion object {
    val clickToResetLabel =
      NodeLabel("OnClearedHost2", "RetainedComponentClearedTracker2", "clickToReset")
  }
}

interface HasComposable {
  @Composable fun Composable()
}

@ComposeComponentHost
class KeyInterferenceHost1
@Inject
constructor(private val retainedScopedDep: KeyInterferenceRetainedState) :
  Hilt_KeyInterferenceHost1(), HasComposable {

  @Composable
  override fun Composable() {
    LabeledNode(
      retainedScopedStateLabel,
      modifier =
        Modifier.clickable { retainedScopedDep.value++ }.customInfo(retainedScopedDep.value)
    )
  }

  companion object {
    val retainedScopedStateLabel =
      NodeLabel("KeyInterferenceHost1", "KeyInterferenceRetainedScoped.state")
  }
}

@ComposeComponentHost
class KeyInterferenceHost2
@Inject
constructor(private val retainedScopedDep: KeyInterferenceRetainedState) :
  Hilt_KeyInterferenceHost2(), HasComposable {
  @Composable
  override fun Composable() {
    LabeledNode(
      retainedScopedStateLabel,
      Modifier.clickable { retainedScopedDep.value++ }.customInfo(retainedScopedDep.value),
    )
  }

  companion object {
    val retainedScopedStateLabel =
      NodeLabel("KeyInterferenceHost2", "KeyInterferenceRetainedScoped.state")
  }
}

@ComposeRetainedScoped
@ComposeRetainedProvided
class KeyInterferenceRetainedState @Inject constructor() {
  var value by mutableStateOf(0)
}

/**
 * Used to store a count which is unique to a given type.
 *
 * For example, this can be used to track how many instances have been created for a given type:
 * during initialization of the type call [getAndIncrementCountFor] to count the newly created
 * instance:
 * ```
 * class MyType @Inject constructor(typeBasedCounter: TypeBasedCounter) {
 *   val instanceNum = typeBasedCounter.getAndIncrementCountFor<MyType>()
 * }
 * ```
 */
@Singleton
class TypeBasedCounter @Inject constructor() {
  val classToInstanceCount = mutableMapOf<Class<*>, Int>()

  inline fun <reified T> getAndIncrementCountFor(): Int {
    val instanceCount = classToInstanceCount.getOrPut(T::class.java) { 0 }
    classToInstanceCount[T::class.java] = instanceCount + 1
    return instanceCount
  }
}
