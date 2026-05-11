plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kover)
}

kotlin {
    jvmToolchain(21)
}

kover {
    currentProject {
        createVariant("ci") {
            add("jvm")
        }
    }
}

dependencies {
    implementation("javax.inject:javax.inject:1")
    implementation(libs.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.coroutines.test)
}
