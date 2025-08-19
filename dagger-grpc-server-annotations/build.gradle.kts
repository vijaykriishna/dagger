import dagger.gradle.build.SoftwareType

plugins {
  alias(libs.plugins.daggerBuild)
  id(libs.plugins.kotlinJvm.get().pluginId)
  id(libs.plugins.binaryCompatibilityValidator.get().pluginId)
}

dependencies {
  api(libs.javax.inject)
}

daggerBuild {
  type = SoftwareType.JVM_LIBRARY
  isPublished = true
}

kotlin { explicitApi() }
