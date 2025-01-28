import dagger.gradle.build.daggerSources
import dagger.gradle.build.findXProcessingJar
import dagger.gradle.build.findXProcessingTestingJar

plugins { alias(libs.plugins.dagger.kotlinJvm) }

daggerSources {
  main.setPackages(
    listOf(
      "java/dagger/spi/model/testing",
      "java/dagger/testing",
      "java/dagger/testing/compile",
      "java/dagger/testing/golden",
    )
  )
}

dependencies {
  implementation(project(":dagger"))
  implementation(project(":dagger-spi"))
  implementation(project(":dagger-compiler"))
  implementation(files(project.findXProcessingJar()))
  implementation(files(project.findXProcessingTestingJar()))
  implementation(libs.auto.value.annotations)
  annotationProcessor(libs.auto.value.compiler)
  implementation(libs.checkerFramework)
  implementation(libs.guava.jre)
  implementation(libs.javaCompileTesting)
  implementation(libs.javax.inject)
  implementation(libs.junit)
  implementation(libs.ksp.api)
  implementation(libs.truth)
}
