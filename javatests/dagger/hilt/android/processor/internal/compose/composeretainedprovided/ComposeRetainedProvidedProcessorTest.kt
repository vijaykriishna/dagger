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

package dagger.hilt.android.processor.internal.compose.composeretainedprovided

import androidx.room.compiler.processing.ExperimentalProcessingApi
import androidx.room.compiler.processing.XProcessingEnv
import dagger.hilt.android.testing.compile.HiltCompilerTests
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@ExperimentalProcessingApi
@RunWith(JUnit4::class)
class ComposeRetainedProvidedProcessorTest {

  @Test
  fun compile_composeRetainedProvidedOnInterface_failsCompilation() {
    val providedType = HiltCompilerTests.javaSource(
      "$TEST_PACKAGE.InterfaceProvidedType",
      """
        package $TEST_PACKAGE;

        import dagger.hilt.android.compose.ComposeRetainedProvided;
        import dagger.hilt.android.compose.scopes.ComposeRetainedScoped;

        @ComposeRetainedScoped
        @ComposeRetainedProvided
        interface InterfaceProvidedType {
          void method();
        }
      """.trimIndent()
    )

    HiltCompilerTests.hiltCompiler(providedType)
        .withAdditionalJavacProcessors(ComposeRetainedProvidedProcessor())
        .withAdditionalKspProcessors(KspComposeRetainedProvidedProcessor.Provider())
        .compile() { subject ->
          subject.hasErrorCount(1)
          subject.hasErrorContaining(
            "@ComposeRetainedProvided can only be used with method and class types:"
          )
          subject.hasErrorContaining("$TEST_PACKAGE.InterfaceProvidedType")
        }
  }

  @Test
  fun compile_noInjectConstructor_failsCompilation() {
    val providedType = HiltCompilerTests.javaSource(
      "$TEST_PACKAGE.NoInjectConstructor",
      """
        package $TEST_PACKAGE;

        import dagger.hilt.android.compose.ComposeRetainedProvided;
        import dagger.hilt.android.compose.scopes.ComposeRetainedScoped;

        @ComposeRetainedScoped
        @ComposeRetainedProvided
        class NoInjectConstructor {
          NoInjectConstructor() {}
        }
      """.trimIndent()
    )

    HiltCompilerTests.hiltCompiler(providedType)
        .withAdditionalJavacProcessors(ComposeRetainedProvidedProcessor())
        .withAdditionalKspProcessors(KspComposeRetainedProvidedProcessor.Provider())
        .compile() { subject ->
          subject.hasErrorCount(1)
          subject.hasErrorContaining(
            "@ComposeRetainedProvided annotated classes should have an @Inject constructor"
          )
          subject.hasErrorContaining("$TEST_PACKAGE.NoInjectConstructor")
        }
  }

  @Test
  fun compile_unscopedProvidedType_failsCompilation() {
    val providedType = HiltCompilerTests.javaSource(
      "$TEST_PACKAGE.UnScopedProvidedType",
      """
        package $TEST_PACKAGE;

        import dagger.hilt.android.compose.ComposeRetainedProvided;
        import javax.inject.Inject;

        @ComposeRetainedProvided
        class UnScopedProvidedType {

          @Inject
          UnScopedProvidedType() {}
        }
      """.trimIndent()
    )

    HiltCompilerTests.hiltCompiler(providedType)
        .withAdditionalJavacProcessors(ComposeRetainedProvidedProcessor())
        .withAdditionalKspProcessors(KspComposeRetainedProvidedProcessor.Provider())
        .compile() { subject ->
          subject.hasErrorCount(1)
          subject.hasErrorContaining(
            "@ComposeRetainedProvided annotated classes and methods must be annotated " +
                "@ComposeRetainedScoped:"
          )
          subject.hasErrorContaining("$TEST_PACKAGE.UnScopedProvidedType")
        }
  }

  @Test
  fun compile_activityRetainedScopedProvidedType_failsCompilation() {
    val providedType = HiltCompilerTests.javaSource(
      "$TEST_PACKAGE.ActivityRetainedScopedProvidedType",
      """
        package $TEST_PACKAGE;

        import dagger.hilt.android.compose.ComposeRetainedProvided;
        import dagger.hilt.android.scopes.ActivityRetainedScoped;
        import javax.inject.Inject;

        @ActivityRetainedScoped
        @ComposeRetainedProvided
        class ActivityRetainedScopedProvidedType {

          @Inject
          ActivityRetainedScopedProvidedType() {}
        }
      """.trimIndent()
    )

    HiltCompilerTests.hiltCompiler(providedType)
        .withAdditionalJavacProcessors(ComposeRetainedProvidedProcessor())
        .withAdditionalKspProcessors(KspComposeRetainedProvidedProcessor.Provider())
        .compile() { subject ->
          subject.hasErrorCount(1)
          subject.hasErrorContaining(
            "@ComposeRetainedProvided annotated classes and methods can only be annotated " +
              "@ComposeRetainedScoped:"
          )
          subject.hasErrorContaining(
            "@ActivityRetainedScoped $TEST_PACKAGE.ActivityRetainedScopedProvidedType"
          )
        }
  }

  @Test
  fun compile_moduleNotInstallInRetainedComponent_failsCompilation() {
    val module = HiltCompilerTests.javaSource(
      "$TEST_PACKAGE.InstallInSingletonComponentModule",
      """
        package $TEST_PACKAGE;

        import dagger.Module;
        import dagger.Provides;
        import dagger.hilt.android.compose.ComposeRetainedProvided;
        import dagger.hilt.android.compose.scopes.ComposeRetainedScoped;
        import dagger.hilt.components.SingletonComponent;
        import dagger.hilt.InstallIn;

        @Module
        @InstallIn(SingletonComponent.class)
        class InstallInSingletonComponentModule {

          @Provides
          @ComposeRetainedScoped
          @ComposeRetainedProvided
          NonInjectableType method() {
            return new NonInjectableType();
          }
        }
      """.trimIndent()
    )

    HiltCompilerTests.hiltCompiler(notInjectType(), module)
        .withAdditionalJavacProcessors(ComposeRetainedProvidedProcessor())
        .withAdditionalKspProcessors(KspComposeRetainedProvidedProcessor.Provider())
        .compile() { subject ->
          subject.hasErrorCount(1)
          subject.hasErrorContaining(
            "@ComposeRetainedProvided annotated methods must be inside " +
              "@InstallIn(ComposeRetainedComponent.class) annotated classes:"
          )
          subject.hasErrorContaining("$TEST_PACKAGE.InstallInSingletonComponentModule")
        }
  }

  @Test
  fun compile_providesMethodWithoutReturn_failsCompilation() {
    val module = HiltCompilerTests.javaSource(
      "$TEST_PACKAGE.ProvidedTypeModule",
      """
        package $TEST_PACKAGE;

        import dagger.Module;
        import dagger.Provides;
        import dagger.hilt.InstallIn;
        import dagger.hilt.android.compose.components.ComposeRetainedComponent;
        import dagger.hilt.android.compose.ComposeRetainedProvided;
        import dagger.hilt.android.compose.scopes.ComposeRetainedScoped;

        @InstallIn(ComposeRetainedComponent.class)
        @Module
        abstract class ProvidedTypeModule {

          @Provides
          @ComposeRetainedScoped
          @ComposeRetainedProvided
          static void provideType() {}
        }
      """.trimIndent()
    )

    // Other Dagger processors verify this check, but ComposeRetainedProvidedMetadata relies on
    // there being a return type, so it's tested here.
    HiltCompilerTests.hiltCompiler(notInjectType(), module)
        .withAdditionalJavacProcessors(ComposeRetainedProvidedProcessor())
        .withAdditionalKspProcessors(KspComposeRetainedProvidedProcessor.Provider())
        .compile() { subject ->
          subject.hasErrorCount(1)
          subject.hasErrorContaining("@Provides methods must return a value")
        }
  }

  @Test
  fun compile_providedTypeNotFromProvidesOrBoundMethod_failsCompilation() {
    val module = HiltCompilerTests.javaSource(
      "$TEST_PACKAGE.ProvidedTypeModule",
      """
        package $TEST_PACKAGE;

        import dagger.Module;
        import dagger.hilt.InstallIn;
        import dagger.hilt.android.compose.components.ComposeRetainedComponent;
        import dagger.hilt.android.compose.ComposeRetainedProvided;
        import dagger.hilt.android.compose.scopes.ComposeRetainedScoped;

        @InstallIn(ComposeRetainedComponent.class)
        @Module
        abstract class ProvidedTypeModule {

          @ComposeRetainedScoped
          @ComposeRetainedProvided
          static NonInjectableType provideType() {
            return new NonInjectableType();
          }
        }
      """.trimIndent()
    )


    HiltCompilerTests.hiltCompiler(notInjectType(), module)
        .withAdditionalJavacProcessors(ComposeRetainedProvidedProcessor())
        .withAdditionalKspProcessors(KspComposeRetainedProvidedProcessor.Provider())
        .compile() { subject ->
          subject.hasErrorCount(1)
          subject.hasErrorContaining(
            "@ComposeRetainedProvided annotated methods must also be annotated @Provides or @Binds:"
          )
          subject.hasErrorContaining("provideType()")
        }
  }

  @Test
  fun compile_providesIntoSetMethod_failsCompilation() {
    val module = HiltCompilerTests.javaSource(
      "$TEST_PACKAGE.ProvidedTypeModule",
      """
        package $TEST_PACKAGE;

        import dagger.Module;
        import dagger.Provides;
        import dagger.hilt.InstallIn;
        import dagger.hilt.android.compose.components.ComposeRetainedComponent;
        import dagger.hilt.android.compose.ComposeRetainedProvided;
        import dagger.hilt.android.compose.scopes.ComposeRetainedScoped;
        import dagger.multibindings.IntoSet;

        @InstallIn(ComposeRetainedComponent.class)
        @Module
        abstract class ProvidedTypeModule {
          @Provides
          @IntoSet
          @ComposeRetainedScoped
          @ComposeRetainedProvided
          static NonInjectableType provideType() {
            return new NonInjectableType();
          }
        }
      """.trimIndent()
    )

    HiltCompilerTests.hiltCompiler(notInjectType(), module)
        .withAdditionalJavacProcessors(ComposeRetainedProvidedProcessor())
        .withAdditionalKspProcessors(KspComposeRetainedProvidedProcessor.Provider())
        .compile() { subject ->
          subject.hasErrorCount(1)
          subject.hasErrorContaining(
            "Multibindings are not supported for @ComposeRetainedProvided annotated methods:"
          )
          subject.hasErrorContaining("provideType()")
        }
  }

  @Test
  fun compile_providesIntoMapMethod_failsCompilation() {
    val module = HiltCompilerTests.javaSource(
      "$TEST_PACKAGE.ProvidedTypeModule",
      """
        package $TEST_PACKAGE;

        import dagger.Module;
        import dagger.Provides;
        import dagger.hilt.InstallIn;
        import dagger.hilt.android.compose.components.ComposeRetainedComponent;
        import dagger.hilt.android.compose.ComposeRetainedProvided;
        import dagger.hilt.android.compose.scopes.ComposeRetainedScoped;
        import dagger.multibindings.IntoMap;
        import dagger.multibindings.StringKey;

        @InstallIn(ComposeRetainedComponent.class)
        @Module
        abstract class ProvidedTypeModule {
          @Provides
          @IntoMap
          @StringKey("key")
          @ComposeRetainedScoped
          @ComposeRetainedProvided
          static NonInjectableType provideType() {
            return new NonInjectableType();
          }
        }
      """.trimIndent()
    )

    HiltCompilerTests.hiltCompiler(notInjectType(), module)
        .withAdditionalJavacProcessors(ComposeRetainedProvidedProcessor())
        .withAdditionalKspProcessors(KspComposeRetainedProvidedProcessor.Provider())
        .compile() { subject ->
          subject.hasErrorCount(1)
          subject.hasErrorContaining(
            "Multibindings are not supported for @ComposeRetainedProvided annotated methods:"
          )
          subject.hasErrorContaining("provideType()")
        }
  }

  @Test
  fun compile_providesElementsIntoMethod_failsCompilation() {
    val module = HiltCompilerTests.javaSource(
      "$TEST_PACKAGE.ProvidedTypeModule",
      """
        package $TEST_PACKAGE;

        import dagger.Module;
        import dagger.Provides;
        import dagger.hilt.InstallIn;
        import dagger.hilt.android.compose.components.ComposeRetainedComponent;
        import dagger.hilt.android.compose.ComposeRetainedProvided;
        import dagger.hilt.android.compose.scopes.ComposeRetainedScoped;
        import dagger.multibindings.ElementsIntoSet;
        import java.util.Set;
        import java.util.HashSet;

        @InstallIn(ComposeRetainedComponent.class)
        @Module
        abstract class ProvidedTypeModule {
          @Provides
          @ElementsIntoSet
          @ComposeRetainedScoped
          @ComposeRetainedProvided
          static Set<NonInjectableType> provideType() {
            Set<NonInjectableType> elements = new HashSet<>();
            elements.add(new NonInjectableType());
            return elements;
          }
        }
      """.trimIndent()
    )

    HiltCompilerTests.hiltCompiler(notInjectType(), module)
        .withAdditionalJavacProcessors(ComposeRetainedProvidedProcessor())
        .withAdditionalKspProcessors(KspComposeRetainedProvidedProcessor.Provider())
        .compile() { subject ->
          subject.hasErrorCount(1)
          subject.hasErrorContaining(
            "Multibindings are not supported for @ComposeRetainedProvided annotated methods:"
          )
          subject.hasErrorContaining("provideType()")
        }
  }

  @Test
  fun compile_providesMethodNotRetainedScoped_failsCompilation() {
    val module = HiltCompilerTests.javaSource(
      "$TEST_PACKAGE.ProvidedTypeModule",
      """
        package $TEST_PACKAGE;

        import dagger.Module;
        import dagger.Provides;
        import dagger.hilt.InstallIn;
        import dagger.hilt.android.compose.components.ComposeRetainedComponent;
        import dagger.hilt.android.compose.ComposeRetainedProvided;

        @InstallIn(ComposeRetainedComponent.class)
        @Module
        abstract class ProvidedTypeModule {
          @Provides
          @ComposeRetainedProvided
          static NonInjectableType provideType() {
            return new NonInjectableType();
          }
        }
      """.trimIndent()
    )

    HiltCompilerTests.hiltCompiler(notInjectType(), module)
        .withAdditionalJavacProcessors(ComposeRetainedProvidedProcessor())
        .withAdditionalKspProcessors(KspComposeRetainedProvidedProcessor.Provider())
        .compile() { subject ->
          subject.hasErrorCount(1)
          subject.hasErrorContaining(
            "@ComposeRetainedProvided annotated classes and methods must be annotated " +
              "@ComposeRetainedScoped:"
          )
          subject.hasErrorContaining("provideType()")
        }
  }

  @Test
  fun compile_providesMethodActivityRetainedScoped_failsCompilation() {
    val module = HiltCompilerTests.javaSource(
      "$TEST_PACKAGE.ProvidedTypeModule",
      """
        package $TEST_PACKAGE;

        import dagger.Module;
        import dagger.Provides;
        import dagger.hilt.InstallIn;
        import dagger.hilt.android.compose.components.ComposeRetainedComponent;
        import dagger.hilt.android.compose.ComposeRetainedProvided;
        import dagger.hilt.android.scopes.ActivityRetainedScoped;


        @InstallIn(ComposeRetainedComponent.class)
        @Module
        abstract class ProvidedTypeModule {
          @Provides
          @ActivityRetainedScoped
          @ComposeRetainedProvided
          static NonInjectableType provideType() {
            return new NonInjectableType();
          }
        }
      """.trimIndent()
    )

    HiltCompilerTests.hiltCompiler(notInjectType(), module)
        .withAdditionalJavacProcessors(ComposeRetainedProvidedProcessor())
        .withAdditionalKspProcessors(KspComposeRetainedProvidedProcessor.Provider())
        .compile() { subject ->
          subject.hasErrorCount(1)
          subject.hasErrorContaining(
            "@ComposeRetainedProvided annotated classes and methods can only be annotated " +
              "@ComposeRetainedScoped:"
          )
          subject.hasErrorContaining("@ActivityRetainedScoped provideType()")
        }
  }

  @Test
  fun compile_bindsMethodNotRetainedScoped_failsCompilation() {
    val module = HiltCompilerTests.javaSource(
      "$TEST_PACKAGE.ProvidedTypeModule",
      """
        package $TEST_PACKAGE;

        import dagger.Binds;
        import dagger.Module;
        import dagger.hilt.InstallIn;
        import dagger.hilt.android.compose.components.ComposeRetainedComponent;
        import dagger.hilt.android.compose.ComposeRetainedProvided;
        import javax.inject.Named;

        @InstallIn(ComposeRetainedComponent.class)
        @Module
        abstract class ProvidedTypeModule {
          @Binds
          @Named("name")
          @ComposeRetainedProvided
          abstract InjectableType bindType(InjectableType type);
        }
      """.trimIndent()
    )

    HiltCompilerTests.hiltCompiler(injectType(), module)
        .withAdditionalJavacProcessors(ComposeRetainedProvidedProcessor())
        .withAdditionalKspProcessors(KspComposeRetainedProvidedProcessor.Provider())
        .compile() { subject ->
          subject.hasErrorCount(1)
          subject.hasErrorContaining(
            "@ComposeRetainedProvided annotated classes and methods must be annotated " +
              "@ComposeRetainedScoped:"
          )
          subject.hasErrorContaining("bindType($TEST_PACKAGE.InjectableType)")
        }
  }

  @Test
  fun compile_providesMethodUnScopedReturnedTypeIsRetainedScoped_failsCompilation() {
    val module = HiltCompilerTests.javaSource(
      "$TEST_PACKAGE.ProvidedTypeModule",
      """
        package $TEST_PACKAGE;

        import dagger.Module;
        import dagger.Provides;
        import dagger.hilt.InstallIn;
        import dagger.hilt.android.compose.components.ComposeRetainedComponent;
        import dagger.hilt.android.compose.ComposeRetainedProvided;
        import dagger.hilt.android.compose.scopes.ComposeRetainedScoped;

        @InstallIn(ComposeRetainedComponent.class)
        @Module
        abstract class ProvidedTypeModule {
          @Provides
          @ComposeRetainedProvided
          static ScopedProvidedType provideType() {
            return new ScopedProvidedType();
          }
        }
      """.trimIndent()
    )

    // Even though the underlying ScopedProvidedType is @ComposeRetainedScoped, the Module provides
    // a different instance, so the method still needs to be scoped.
    HiltCompilerTests.hiltCompiler(retainedScopedProvidedType(), module)
        .withAdditionalJavacProcessors(ComposeRetainedProvidedProcessor())
        .withAdditionalKspProcessors(KspComposeRetainedProvidedProcessor.Provider())
        .compile() { subject ->
          subject.hasErrorCount(1)
          subject.hasErrorContaining(
            "@ComposeRetainedProvided annotated classes and methods must be annotated " +
              "@ComposeRetainedScoped:"
          )
          subject.hasErrorContaining("provideType()")
        }
  }

  @Test
  fun compile_bindsMethodReturnedTypeIsRetainedScoped_failsCompilation() {
    val module = HiltCompilerTests.javaSource(
      "$TEST_PACKAGE.ProvidedTypeModule",
      """
        package $TEST_PACKAGE;

        import dagger.Binds;
        import dagger.Module;
        import dagger.hilt.InstallIn;
        import dagger.hilt.android.compose.components.ComposeRetainedComponent;
        import dagger.hilt.android.compose.ComposeRetainedProvided;
        import javax.inject.Named;

        @InstallIn(ComposeRetainedComponent.class)
        @Module
        abstract class ProvidedTypeModule {
          @Binds
          @Named("name")
          @ComposeRetainedProvided
          abstract ScopedProvidedType bindType(ScopedProvidedType type);
        }
      """.trimIndent()
    )

    // Even though the underlying ScopedProvidedType is @ComposeRetainedScoped, the Module binds
    // a different instance, so the method still needs to be scoped
    HiltCompilerTests.hiltCompiler(retainedScopedProvidedType(), module)
        .withAdditionalJavacProcessors(ComposeRetainedProvidedProcessor())
        .withAdditionalKspProcessors(KspComposeRetainedProvidedProcessor.Provider())
        .compile() { subject ->
          subject.hasErrorCount(1)
          subject.hasErrorContaining(
            "@ComposeRetainedProvided annotated classes and methods must be annotated " +
              "@ComposeRetainedScoped:"
          )
          subject.hasErrorContaining("bindType($TEST_PACKAGE.ScopedProvidedType)")
        }
  }

  @Test
  fun compile_provideTypeHasMultipleQualifiers_failsCompilation() {
    val blueQualifier = HiltCompilerTests.javaSource(
      "$TEST_PACKAGE.Blue",
      """
      package $TEST_PACKAGE;

      import javax.inject.Qualifier;

      @Qualifier
      @interface Blue {}
      """.trimIndent()
    )

    val redQualifier = HiltCompilerTests.javaSource(
      "$TEST_PACKAGE.Red",
      """
      package $TEST_PACKAGE;

      import javax.inject.Qualifier;

      @Qualifier
      @interface Red {}
      """.trimIndent()
    )

    val module = HiltCompilerTests.javaSource(
      "$TEST_PACKAGE.ProvidedTypeModule",
      """
      package $TEST_PACKAGE;

      import dagger.Module;
      import dagger.Provides;
      import dagger.hilt.InstallIn;
      import dagger.hilt.android.compose.components.ComposeRetainedComponent;
      import dagger.hilt.android.compose.ComposeRetainedProvided;
      import dagger.hilt.android.compose.scopes.ComposeRetainedScoped;

      @InstallIn(ComposeRetainedComponent.class)
      @Module
      abstract class ProvidedTypeModule {
        @Provides
        @Blue
        @Red
        @ComposeRetainedScoped
        @ComposeRetainedProvided
        static NonInjectableType provideType() {
          return new NonInjectableType();
        }
      }
      """.trimIndent()
    )

    // Other Dagger processors verify this check, but ComposeRetainedProvidedMetadata relies on
    // there only being one qualifier, so it's tested here.
    HiltCompilerTests.hiltCompiler(notInjectType(), blueQualifier, redQualifier, module)
        .withAdditionalJavacProcessors(ComposeRetainedProvidedProcessor())
        .withAdditionalKspProcessors(KspComposeRetainedProvidedProcessor.Provider())
        .compile() { subject ->
          subject.hasErrorCount(2)
          subject.hasErrorContaining("@Provides methods may not use more than one @Qualifier")
            .onSource(module)
            .onLine(14)
          subject.hasErrorContaining("@Provides methods may not use more than one @Qualifier")
            .onSource(module)
            .onLine(15)
        }
  }

  private fun notInjectType() = HiltCompilerTests.javaSource(
    "$TEST_PACKAGE.NonInjectableType",
    """
    package $TEST_PACKAGE;

    class NonInjectableType {}
    """.trimIndent()
  )

  private fun injectType() = HiltCompilerTests.javaSource(
    "$TEST_PACKAGE.InjectableType",
    """
    package $TEST_PACKAGE;

    import javax.inject.Inject;

    class InjectableType {

      @Inject
      InjectableType() {}
    }
    """.trimIndent()
  )

  private fun retainedScopedProvidedType() = HiltCompilerTests.javaSource(
    "$TEST_PACKAGE.ScopedProvidedType",
    """
    package $TEST_PACKAGE;

    import dagger.hilt.android.compose.ComposeRetainedProvided;
    import dagger.hilt.android.compose.scopes.ComposeRetainedScoped;
    import javax.inject.Inject;

    @ComposeRetainedScoped
    @ComposeRetainedProvided
    class ScopedProvidedType {

      @Inject
      ScopedProvidedType() {}
    }
    """.trimIndent()
  )
}

private const val TEST_PACKAGE = "dagger.hilt.android.processor.internal.compose.testing"
