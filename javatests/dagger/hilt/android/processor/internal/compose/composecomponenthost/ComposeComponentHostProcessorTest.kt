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

package dagger.hilt.android.processor.internal.compose.composecomponenthost

import androidx.room.compiler.processing.ExperimentalProcessingApi
import androidx.room.compiler.processing.util.Source
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.compile.HiltCompilerTests
import javax.tools.Diagnostic
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@ExperimentalProcessingApi
@RunWith(JUnit4::class)
class ComposeComponentHostProcessorTest {

  @get:Rule val tempFolderRule = TemporaryFolder()

  @Test
  fun compile_noInjectConstructor_failsCompilation() {
    val hostFile =
      Source.java(
        "$TEST_PACKAGE.Host1",
        """
      package $TEST_PACKAGE;

      import androidx.compose.runtime.Composable;
      import dagger.hilt.android.compose.ComposeComponentHost;

      @ComposeComponentHost
      final class Host1 extends Hilt_Host1 {

        Host1() {}

        @Composable
        public void composable() {}
      }
      """
          .trimIndent()
      )

    kspCompiler(hostFile).compile() { subject ->
      subject.hasErrorContaining(
        "@ComposeComponentHost annotated classes should have an @Inject constructor"
      )
    }
  }

  @Test
  fun compile_hasNoComposableFunctions_failsCompilation() {
    val hostFile =
      Source.java(
        "$TEST_PACKAGE.Host1",
        """
      package $TEST_PACKAGE;

      import dagger.hilt.android.compose.ComposeComponentHost;
      import javax.inject.Inject;

      @ComposeComponentHost
      final class Host1 extends Hilt_Host1 {

        @Inject
        Host1() {}

        public void nonComposable() {}
      }
      """
          .trimIndent()
      )

    kspCompiler(hostFile).compile() { subject ->
      subject.hasErrorContaining(
        "@ComposeComponentHost annotated classes should have at least one @Composable " +
          "function. Use a regular class with an @Inject constructor for classes without " +
          "composables."
      )
    }
  }

  @Test
  fun compile_noSuperType_failsCompilation() {
    val hostFile =
      Source.java(
        "$TEST_PACKAGE.Host1",
        """
      package $TEST_PACKAGE;

      import androidx.compose.runtime.Composable;
      import dagger.hilt.android.compose.ComposeComponentHost;
      import javax.inject.Inject;

      @ComposeComponentHost
      final class Host1 {

        @Inject
        Host1() {}

        @Composable
        public void Composable() {}
      }
      """
          .trimIndent()
      )

    // TODO(b/288210593): Change this to KSP once this bug is fixed
    HiltCompilerTests.compileWithKapt(
      listOf(hostFile),
      listOf(ComposeComponentHostProcessor()),
      tempFolderRule
    ) { result ->
      assertThat(result.success).isFalse()

      val errors = result.diagnostics[Diagnostic.Kind.ERROR]
      val expectedError =
        "@ComposeComponentHost annotated class expected to extend Hilt_Host1. Found: Object"
      val foundError = errors?.filter { it.msg.contains(expectedError) }?.firstOrNull()
      assertThat(foundError).isNotNull()
    }
  }

  @Test
  fun compile_wrongSuperType_failsCompilation() {
    val hostSuperClass =
      Source.java(
        "$TEST_PACKAGE.HostSuperClass",
        """
      package $TEST_PACKAGE;

        class HostSuperClass {}
      """
          .trimIndent()
      )

    val hostFile =
      Source.java(
        "$TEST_PACKAGE.Host1",
        """
      package $TEST_PACKAGE;

      import androidx.compose.runtime.Composable;
      import dagger.hilt.android.compose.ComposeComponentHost;
      import javax.inject.Inject;

      @ComposeComponentHost
      final class Host1 extends HostSuperClass {

        @Inject
        Host1() {}

        @Composable
        public void Composable() {}
      }
      """
          .trimIndent()
      )

    // TODO(b/288210593): Change this to KSP once this bug is fixed
    HiltCompilerTests.compileWithKapt(
      listOf(hostSuperClass, hostFile),
      listOf(ComposeComponentHostProcessor()),
      tempFolderRule
    ) { result ->
      assertThat(result.success).isFalse()

      val errors = result.diagnostics[Diagnostic.Kind.ERROR]
      val expectedError =
        "@ComposeComponentHost annotated class expected to extend Hilt_Host1. Found: HostSuperClass"
      val foundError = errors?.filter { it.msg.contains(expectedError) }?.firstOrNull()
      assertThat(foundError).isNotNull()
    }
  }

  @Test
  fun compile_ksp_correctHost_succeedsCompilation() {
    val hostFile =
      Source.java(
        "$TEST_PACKAGE.Host1",
        """
      package $TEST_PACKAGE;

      import androidx.compose.runtime.Composable;
      import dagger.hilt.android.compose.ComposeComponentHost;
      import javax.inject.Inject;

      @ComposeComponentHost
      final class Host1 extends Hilt_Host1 {

        @Inject
        Host1() {}

        @Composable
        public void Composable() {}
      }
      """
          .trimIndent()
      )

    kspCompiler(hostFile).compile { subject -> subject.hasErrorCount(0) }
  }

  @Test
  fun compile_kapt_correctHost_succeedsCompilation() {
    val hostFile =
      Source.java(
        "$TEST_PACKAGE.Host1",
        """
      package $TEST_PACKAGE;

      import androidx.compose.runtime.Composable;
      import dagger.hilt.android.compose.ComposeComponentHost;
      import javax.inject.Inject;

      @ComposeComponentHost
      final class Host1 extends Hilt_Host1 {

        @Inject
        Host1() {}

        @Composable
        public void Composable() {}
      }
      """
          .trimIndent()
      )

    // TODO(b/288210593): Change this to KSP once this bug is fixed
    HiltCompilerTests.compileWithKapt(
      listOf(hostFile),
      listOf(ComposeComponentHostProcessor()),
      tempFolderRule
    ) { result ->
      assertThat(result.success).isTrue()
    }
  }
}

private fun kspCompiler(
  firstSource: Source,
  vararg additionalSources: Source
): HiltCompilerTests.HiltCompiler =
  HiltCompilerTests.hiltCompiler(firstSource, *additionalSources)
    .withAdditionalJavacProcessors(ComposeComponentHostProcessor())
    .withAdditionalKspProcessors(KspComposeComponentHostProcessor.Provider())

private const val TEST_PACKAGE = "dagger.hilt.android.processor.internal.compose.testing"
