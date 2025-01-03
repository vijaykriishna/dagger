import dagger.gradle.build.daggerSources

plugins {
    alias(libs.plugins.dagger.kotlinJvm)
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

dependencies {
    api(libs.javax.inject)
    api(libs.jakarta.inject)
    api(libs.jspecify)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.guava.jre)
}