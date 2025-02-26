import dagger.gradle.build.daggerSources

plugins {
  alias(libs.plugins.dagger.kotlinJvm)
  alias(libs.plugins.dagger.publish)
  alias(libs.plugins.dagger.shadow)
}

daggerSources {
  main.setPackages(
    listOf(
      "java/dagger/internal/codegen/extension",
      "java/dagger/model",
      "java/dagger/spi",
      "java/dagger/spi/model",
    )
  )
}

dependencies {
  implementation(project(":dagger"))

  implementation(libs.findBugs)
  implementation(libs.auto.value.annotations)
  annotationProcessor(libs.auto.value.compiler)
  implementation(libs.ksp.api)
  implementation(libs.guava.failureAccess)
  implementation(libs.guava.jre)
  implementation(libs.javaPoet)
  implementation(libs.javax.inject)

  // These dependencies will be shaded (included) within the artifact of this project and are
  // shared with other processors, such as dagger-compiler.
  val shaded by configurations.getting
  shaded(libs.auto.common)
  shaded(
    files(
      project.rootProject.layout.projectDirectory
        .dir("java/dagger/internal/codegen/xprocessing")
        .file("xprocessing-internal.jar")
    )
  )
}

shading {
  relocate("com.google.auto.common", "dagger.spi.internal.shaded.auto.common")
  relocate("androidx.room", "dagger.spi.internal.shaded.androidx.room")
}
