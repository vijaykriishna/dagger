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

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import javax.inject.Inject

/** Manages aspects of the RetainedViewModel that it can't handle internally. */
class RetainedViewModelManager @Inject internal constructor(private val activity: Activity) {

  /**
   * Adds the [RetainedViewModel] as a LifecycleObserver of the associated Activity.
   *
   * The RetainedViewModel needs to be added as an observer to prevent leaking
   * ComposeRetainedComponents that aren't used in a composition after Activity recreation.
   * [rememberComponentHost] is the natural place to add the ViewModel as an observer, but it's not
   * guaranteed to be called - the composition could change such that rememberComponentHost is no
   * longer called at all. Since the ComponentHostCreator should be injected regardless of the
   * composition structure, the Hilt generated implementation calls this method in its constructor.
   */
  fun addAsObserver() {
    check(activity is ComponentActivity) {
      "ComposeComponentHosts must be used within the context of a ComponentActivity."
    }

    val retainedViewModel = ViewModelProvider(activity)[RetainedViewModel::class.java]
    activity.lifecycle.addObserver(retainedViewModel)
  }
}
