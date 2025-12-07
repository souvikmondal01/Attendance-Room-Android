buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.4")
    }
}

plugins {
    id("com.android.application") version "8.13.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("com.google.dagger.hilt.android") version "2.57.2" apply false
    id("com.google.devtools.ksp") version "2.2.20-2.0.3" apply false
}