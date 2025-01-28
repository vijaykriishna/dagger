import dagger.gradle.build.daggerSources
import dagger.gradle.build.findXProcessingJar

plugins { alias(libs.plugins.dagger.kotlinJvm) }

daggerSources {
  // TODO(danysantiago): Add packages that test Kotlin sources
  test.setPackages(
    listOf(
      "javatests/dagger/functional/assisted",
      "javatests/dagger/functional/assisted/subpackage",
      "javatests/dagger/functional/basic",
      "javatests/dagger/functional/basic/subpackage",
      "javatests/dagger/functional/binds",
      "javatests/dagger/functional/binds/subpackage",
      "javatests/dagger/functional/builder",
      "javatests/dagger/functional/builderbinds",
      "javatests/dagger/functional/componentdependency",
      "javatests/dagger/functional/componentdependency/subpackage",
      "javatests/dagger/functional/cycle",
      "javatests/dagger/functional/factory",
      "javatests/dagger/functional/generated",
      "javatests/dagger/functional/generictypes",
      "javatests/dagger/functional/generictypes/subpackage",
      "javatests/dagger/functional/guava",
      "javatests/dagger/functional/guava/a",
      "javatests/dagger/functional/jakarta",
      "javatests/dagger/functional/jdk8",
      "javatests/dagger/functional/jdk8/a",
      "javatests/dagger/functional/membersinject",
      "javatests/dagger/functional/membersinject/subpackage",
      "javatests/dagger/functional/membersinject/subpackage/a",
      "javatests/dagger/functional/membersinject/subpackage/b",
      "javatests/dagger/functional/modules",
      "javatests/dagger/functional/modules/subpackage",
      "javatests/dagger/functional/multibindings",
      "javatests/dagger/functional/multibindings/subpackage",
      "javatests/dagger/functional/multipackage",
      "javatests/dagger/functional/multipackage/a",
      "javatests/dagger/functional/multipackage/b",
      "javatests/dagger/functional/multipackage/c",
      "javatests/dagger/functional/multipackage/d",
      "javatests/dagger/functional/multipackage/foo",
      "javatests/dagger/functional/multipackage/grandsub",
      "javatests/dagger/functional/multipackage/moduleconstructor",
      "javatests/dagger/functional/multipackage/primitives",
      "javatests/dagger/functional/multipackage/sub",
      "javatests/dagger/functional/names",
      "javatests/dagger/functional/nullables",
      "javatests/dagger/functional/rawtypes",
      "javatests/dagger/functional/reusable",
      "javatests/dagger/functional/scope",
      "javatests/dagger/functional/staticproviders",
      "javatests/dagger/functional/subcomponent",
      "javatests/dagger/functional/subcomponent/hiding",
      "javatests/dagger/functional/subcomponent/hiding/a",
      "javatests/dagger/functional/subcomponent/hiding/b",
      "javatests/dagger/functional/subcomponent/module",
      "javatests/dagger/functional/subcomponent/multibindings",
      "javatests/dagger/functional/subcomponent/pruning",
      "javatests/dagger/functional/subcomponent/repeat",
    )
  )
}

dependencies {
  testImplementation(project(":dagger"))
  testAnnotationProcessor(project(":dagger-compiler"))
  testAnnotationProcessor(project(":dagger-spi"))
  // Functional tests rely on the non-shaded version of the compiler, so we need to bring
  // those compile only dependencies.
  testAnnotationProcessor(libs.auto.common)
  testAnnotationProcessor(files(project.findXProcessingJar()))

  testImplementation(libs.junit)
  testImplementation(libs.truth)
  testImplementation(libs.auto.factory.annotations)
  testAnnotationProcessor(libs.auto.factory.compiler)
  testImplementation(libs.auto.value.annotations)
  testAnnotationProcessor(libs.auto.value.compiler)
  testImplementation(libs.kotlin.stdlib)
}
