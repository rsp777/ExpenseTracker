plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

fun getGitOutput(vararg args: String): String {
    return providers.exec {
        commandLine("git", *args)
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().ifEmpty { "dev" }
}

android {
    namespace = "com.tracker.expensetracker"
    compileSdk = 37

    val baseVersionName = "1.0.1"
    val branch = runCatching { getGitOutput("rev-parse", "--abbrev-ref", "HEAD") }.getOrDefault("dev")
    val commitHash = runCatching { getGitOutput("rev-parse", "--short", "HEAD") }.getOrDefault("dev")
    val isDevBranch = branch == "dev" || branch.startsWith("dev-") || branch.startsWith("feature/")
    val computedVersionName = if (isDevBranch) "$baseVersionName-dev.$commitHash" else baseVersionName

    defaultConfig {
        applicationId = "com.tracker.expensetracker"
        minSdk = 26
        targetSdk = 37

        versionCode = 2
        versionName = computedVersionName

        buildConfigField("String", "BASE_VERSION_NAME", "\"$baseVersionName\"")
        buildConfigField("String", "GIT_BRANCH", "\"$branch\"")
        buildConfigField("String", "GIT_COMMIT_HASH", "\"$commitHash\"")
        buildConfigField("Boolean", "IS_DEV_BUILD", "$isDevBranch")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)

    // Android / UI Testing
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
