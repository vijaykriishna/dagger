/*
 * Copyright (C) 2024 The Dagger Authors.
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

package dagger.functional.producers.kotlin

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.Module
import dagger.Provides
import dagger.producers.ProducerModule
import dagger.producers.Produces
import dagger.producers.Production
import dagger.producers.ProductionComponent
import dagger.multibindings.IntoSet
import java.util.concurrent.Executor
import javax.inject.Named

@ProductionComponent(
  modules = [
    ExecutorModule::class,
    TestKotlinObjectModule::class,
    TestModuleForNesting.TestNestedKotlinObjectModule::class
  ]
)
interface TestKotlinComponentWithObjectModule {
  fun getDataA(): ListenableFuture<TestDataA>
  @Named("nested-data-a")
  fun getDataAFromNestedModule(): ListenableFuture<TestDataA>
  fun getDataB(): ListenableFuture<TestDataB>
  fun getSetOfDataA(): ListenableFuture<Set<TestDataA>>
}

@ProducerModule
object TestKotlinObjectModule {
  @Produces
  fun provideDataA() = TestDataA("test")

  @Produces
  @JvmStatic
  fun provideDataB() = TestDataB("static-test")

  @Produces
  @IntoSet
  fun provideIntoMapDataA() = TestDataA("set-test")
}

class TestModuleForNesting {
  @ProducerModule
  object TestNestedKotlinObjectModule {
    @Produces
    @Named("nested-data-a")
    fun provideDataA() = TestDataA("nested-test")
  }
}

data class TestDataA(val data: String)
data class TestDataB(val data: String)

@Module
object ExecutorModule {
  @Provides
  @Production
  fun executor(): Executor = MoreExecutors.directExecutor()
}
