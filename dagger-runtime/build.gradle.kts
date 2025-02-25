plugins {
  alias(libs.plugins.dagger.kotlinJvm)
  alias(libs.plugins.dagger.publish)
  alias(libs.plugins.binaryCompatibilityValidator)
}

// TODO: Configure via convention plugin
sourceSets {
  main {
    java.srcDirs("main/java")
    kotlin.srcDirs("main/java")
    resources.srcDirs("main/resources")
  }
  test {
    java.srcDirs("test/javatests")
    kotlin.srcDirs("test/javatests")
  }
}

dependencies {
  api(libs.javax.inject)
  api(libs.jakarta.inject)
  api(libs.jspecify)

  testImplementation(libs.junit)
  testImplementation(libs.truth)
  testImplementation(libs.guava.jre)
}

kotlin { explicitApi() }
