plugins {
    id("com.android.library") version "8.10.0" apply false
}

val coreVersion = providers.gradleProperty("version").orNull ?: "0.1.0-SNAPSHOT"

allprojects {
    group = "com.heycyan.core"
    version = coreVersion
}
