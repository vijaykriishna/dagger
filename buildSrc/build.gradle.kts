plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain {
        languageVersion.set(libs.versions.jdk.map(JavaLanguageVersion::of))
    }
}

dependencies {
    implementation(gradleApi())
    implementation(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kotlinJvm") {
            id = libs.plugins.dagger.kotlinJvm.get().pluginId
            implementationClass = "dagger.gradle.build.KotlinJvmConventionPlugin"
        }
    }
}