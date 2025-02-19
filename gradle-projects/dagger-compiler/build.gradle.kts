import dagger.gradle.build.daggerSources
import dagger.gradle.build.findBootstrapCompilerJar
import dagger.gradle.build.findXProcessingJar
import dagger.gradle.build.findXProcessingTestingJar

plugins {
  alias(libs.plugins.dagger.kotlinJvm)
  alias(libs.plugins.dagger.publish)
  alias(libs.plugins.dagger.shadow)
}

daggerSources {
  main.setPackages(
    listOf(
      "java/dagger/internal/codegen",
      "java/dagger/internal/codegen/base",
      "java/dagger/internal/codegen/binding",
      "java/dagger/internal/codegen/bindinggraphvalidation",
      "java/dagger/internal/codegen/compileroption",
      "java/dagger/internal/codegen/componentgenerator",
      "java/dagger/internal/codegen/javac",
      "java/dagger/internal/codegen/javapoet",
      "java/dagger/internal/codegen/kotlin",
      "java/dagger/internal/codegen/langmodel",
      "java/dagger/internal/codegen/model",
      "java/dagger/internal/codegen/processingstep",
      "java/dagger/internal/codegen/validation",
      "java/dagger/internal/codegen/writing",
      "java/dagger/internal/codegen/xprocessing",
    )
  )
  test.setPackages(listOf("javatests/dagger/internal/codegen"))
  test.setResources(mapOf("javatests/dagger/internal/codegen/goldens" to "goldens"))
}

dependencies {
  implementation(project(":dagger"))
  implementation(project(":dagger-spi"))

  implementation(libs.auto.value.annotations)
  annotationProcessor(libs.auto.value.compiler)
  implementation(libs.auto.service.annotations)
  annotationProcessor(libs.auto.service.compiler)
  implementation(libs.checkerFramework)
  implementation(libs.findBugs)
  implementation(libs.javaFormatter)
  implementation(libs.javaPoet)
  implementation(libs.javax.inject)
  implementation(libs.gradleIncap.annotations)
  annotationProcessor(libs.gradleIncap.compiler)
  implementation(libs.guava.failureAccess)
  implementation(libs.guava.jre)
  implementation(libs.kotlin.stdlib)
  implementation(libs.kotlin.metadataJvm)
  implementation(libs.kotlinPoet)
  implementation(libs.kotlinPoet.javaPoet)
  implementation(libs.ksp.api)

  annotationProcessor(files(project.findBootstrapCompilerJar()))

  // These dependencies are shaded into dagger-spi
  compileOnly(libs.auto.common)
  compileOnly(files(project.findXProcessingJar()))

  testImplementation(project(":dagger-producers"))
  testImplementation(project(":dagger-testing"))

  testImplementation(libs.junit)
  testImplementation(libs.truth)
  testImplementation(libs.auto.common)

  testImplementation(files(project.findXProcessingJar()))
  testImplementation(files(project.findXProcessingTestingJar()))
  testImplementation(libs.javaCompileTesting)
  testImplementation(libs.kotlin.compilerEmbeddable)
  testImplementation(libs.kotlin.annotationProcessingEmbeddable)
  testImplementation(libs.ksp)
  testImplementation(libs.ksp.common)
  testImplementation(libs.ksp.embeddable)

  testAnnotationProcessor(project(":dagger-compiler"))
}

shading {
  relocate("com.google.auto.common", "dagger.spi.internal.shaded.auto.common")
  relocate("androidx.room", "dagger.spi.internal.shaded.androidx.room")
}
