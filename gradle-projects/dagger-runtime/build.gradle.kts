import dagger.gradle.build.daggerSources

plugins {
    id(libs.plugins.kotlinJvm.get().pluginId)
}

// TODO(danysantiago): Add proguard files as META-INF resources
daggerSources {
    main.setPackages(
        listOf(
            "dagger",
            "dagger/assisted",
            "dagger/internal",
            "dagger/multibindings",
        )
    )
    test.setPackages(
        listOf(
            "dagger",
            "dagger/internal",
        )
    )
}

// TODO(danysantiago): Move configuration to a buildSrc plugin so it is applied to all projects
kotlin {
    jvmToolchain(18)
    compilerOptions {
        languageVersion = KotlinVersion.KOTLIN_1_8
        apiVersion = KotlinVersion.KOTLIN_1_8
        jvmTarget = JvmTarget.JVM_1_8
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