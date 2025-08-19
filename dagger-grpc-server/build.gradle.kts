import dagger.gradle.build.SoftwareType
import dagger.gradle.build.findXProcessingJar

plugins {
  alias(libs.plugins.daggerBuild)
  id(libs.plugins.kotlinJvm.get().pluginId)
  id(libs.plugins.binaryCompatibilityValidator.get().pluginId)
}

dependencies {
  api(project(":dagger"))
  api(project(":dagger-grpc-server-annotations"))
  api(libs.javax.inject)
  implementation(libs.guava.failureAccess)
  implementation(libs.guava.jre)
  implementation(libs.protobuf.java)
  implementation(libs.grpc.context)
  implementation(libs.grpc.core)
  implementation(libs.grpc.netty)
  implementation(libs.grpc.protobuf)

  annotationProcessor(project(":dagger-compiler", "unshaded"))
  annotationProcessor(libs.auto.common)
  annotationProcessor(files(project.findXProcessingJar()))
}

daggerBuild {
  type = SoftwareType.JVM_LIBRARY
  isPublished = true
}

kotlin { explicitApi() }
