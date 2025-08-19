import dagger.gradle.build.SoftwareType

plugins {
  alias(libs.plugins.daggerBuild)
  id(libs.plugins.kotlinJvm.get().pluginId)
}

dependencies {
  implementation(project(":dagger"))
  implementation(project(":dagger-grpc-server-annotations"))
  implementation(project(":dagger-spi", "unshaded"))
  implementation(libs.auto.common)
  implementation(libs.auto.service.annotations)
  annotationProcessor(libs.auto.service.compiler)
  implementation(libs.javaFormatter)
  implementation(libs.guava.failureAccess)
  implementation(libs.guava.jre)
  implementation(libs.javaPoet)
}

daggerBuild {
  type = SoftwareType.PROCESSOR
  isPublished = true
}

kotlin { explicitApi() }
