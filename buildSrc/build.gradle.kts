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
    implementation(libs.publishPlugin)
    implementation(libs.shadowPlugin)
}

gradlePlugin {
    plugins {
        register("kotlinJvm") {
            id = libs.plugins.dagger.kotlinJvm.get().pluginId
            implementationClass = "dagger.gradle.build.KotlinJvmConventionPlugin"
        }
    }
    plugins {
        register("publish") {
            id = libs.plugins.dagger.publish.get().pluginId
            implementationClass = "dagger.gradle.build.PublishConventionPlugin"
        }
    }
    plugins {
        register("shadow") {
            id = libs.plugins.dagger.shadow.get().pluginId
            implementationClass = "dagger.gradle.build.ShadowConventionPlugin"
        }
    }
}