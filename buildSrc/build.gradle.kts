plugins {
    alias(libs.plugins.kotlinJvm)
}

kotlin {
    jvmToolchain(18)
}

dependencies {
    implementation(gradleApi())
    implementation(libs.kotlin.gradlePlugin)
}
