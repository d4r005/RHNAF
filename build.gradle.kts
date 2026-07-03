// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization) apply false
    kotlin("multiplatform") version "2.2.10" apply false
    id("org.jetbrains.compose") version "1.7.0" apply false
    id("io.ktor.plugin") version "3.5.1" apply false
}
