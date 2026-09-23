// Root buildscript — explicit Maven Central for AGP transitive deps
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // nothing here — AGP pulled by plugin alias
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
