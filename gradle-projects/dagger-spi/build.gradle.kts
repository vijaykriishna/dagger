import dagger.gradle.build.daggerSources

plugins {
    alias(libs.plugins.dagger.kotlinJvm)
    alias(libs.plugins.dagger.publish)
}

daggerSources {
    main.setPackages(
        listOf(
            "dagger/internal/codegen/extension",
            "dagger/model",
            "dagger/spi",
            "dagger/spi/model",
        )
    )
}

// TODO(danysantiago): Shadow / jarjar: 1. xprocessing, 2. auto-common and 3. kotlin-metadata-jvm
dependencies {
    implementation(project(":dagger"))

    implementation(libs.auto.common)
    implementation(libs.auto.value.annotations)
    annotationProcessor(libs.auto.value.compiler)
    implementation(libs.findBugs)
    implementation(libs.javaPoet)
    implementation(libs.javax.inject)
    implementation(libs.guava.failureAccess)
    implementation(libs.guava.jre)
    implementation(libs.ksp.api)
}