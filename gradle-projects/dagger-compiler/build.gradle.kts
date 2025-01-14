import dagger.gradle.build.daggerSources

plugins {
    alias(libs.plugins.dagger.kotlinJvm)
    alias(libs.plugins.dagger.publish)
}

daggerSources {
    main.setPackages(
        listOf(
            "dagger/internal/codegen",
            "dagger/internal/codegen/base",
            "dagger/internal/codegen/binding",
            "dagger/internal/codegen/bindinggraphvalidation",
            "dagger/internal/codegen/compileroption",
            "dagger/internal/codegen/componentgenerator",
            "dagger/internal/codegen/javapoet",
            "dagger/internal/codegen/kotlin",
            "dagger/internal/codegen/langmodel",
            "dagger/internal/codegen/model",
            "dagger/internal/codegen/processingstep",
            "dagger/internal/codegen/validation",
            "dagger/internal/codegen/writing",
            "dagger/internal/codegen/xprocessing",
        )
    )
}

dependencies {
    implementation(project(":dagger"))
    implementation(project(":dagger-spi"))

    implementation(libs.auto.common)
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
    implementation(libs.ksp.api)

    annotationProcessor(
        files(project.rootProject.layout.projectDirectory
            .dir("java/dagger/internal/codegen/bootstrap")
            .file("bootstrap_compiler_deploy.jar"))
    )
    implementation(
        files(project.rootProject.layout.projectDirectory
            .dir("java/dagger/internal/codegen/xprocessing")
            .file("xprocessing.jar"))
    )
}