import dagger.gradle.build.daggerSources

plugins {
  alias(libs.plugins.dagger.kotlinJvm)
  alias(libs.plugins.dagger.publish)
}

daggerSources {
  main.setPackages(
    listOf(
      "java/dagger/producers",
      "java/dagger/producers/internal",
      "java/dagger/producers/monitoring",
      "java/dagger/producers/monitoring/internal",
    )
  )
}

dependencies {
  api(project(":dagger"))
  implementation(libs.checkerFramework)
  implementation(libs.guava.jre)
}

kotlin { explicitApi() }
