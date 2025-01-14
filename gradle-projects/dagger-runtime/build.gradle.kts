import dagger.gradle.build.daggerSources

plugins {
    alias(libs.plugins.dagger.kotlinJvm)
    alias(libs.plugins.dagger.publish)
    alias(libs.plugins.binaryCompatibilityValidator)
}

daggerSources {
    main.setPackages(
        listOf(
            "dagger",
            "dagger/assisted",
            "dagger/internal",
            "dagger/multibindings",
        )
    )
    main.setResources(
        mapOf(
            "dagger/proguard.pro" to "META-INF/com.android.tools/proguard",
            "dagger/r8.pro" to "META-INF/com.android.tools/r8"
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

kotlin {
    explicitApi()
}