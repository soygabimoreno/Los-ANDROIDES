import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension

plugins {
    alias(libs.plugins.androidApplication)
    kotlin("android")
    alias(libs.plugins.google.services)
    id("dagger.hilt.android.plugin")
    id("com.google.firebase.crashlytics")
    alias(libs.plugins.secrets.gradle.plugin)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

@Suppress("UnstableApiUsage")
android {
    if (isLocalBuild()) {
        signingConfigs {
            create("release") {
                val localProperties = gradleLocalProperties(rootDir, providers)
                keyAlias = localProperties.getProperty("keystore.keyAlias")
                storeFile = file(localProperties.getProperty("keystore.storeFile") ?: "fakePath")
                storePassword = localProperties.getProperty("keystore.storePassword")
                keyPassword = localProperties.getProperty("keystore.keyPassword")
            }
        }
    }
    namespace = "soy.gabimoreno"
    compileSdk =
        libs.versions.sdk.compile
            .get()
            .toInt()
    defaultConfig {
        applicationId = "soy.gabimoreno"
        minSdk =
            libs.versions.sdk.minimum
                .get()
                .toInt()
        targetSdk =
            libs.versions.sdk.target
                .get()
                .toInt()
        versionCode = 74
        versionName = "4.1.3"
        testInstrumentationRunner = "soy.gabimoreno.di.HiltTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    bundle {
        density { enableSplit = true }
        abi { enableSplit = true }
        language { enableSplit = false }
    }
    buildTypes {
        debug {
            versionNameSuffix = "-DEBUG"
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            isCrunchPngs = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro",
            )
            extra["enableCrashlytics"] = false
            extra["alwaysUpdateBuildId"] = false
            splits {
                abi.isEnable = false
            }
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }
        release {
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (isLocalBuild()) {
                signingConfig = signingConfigs.getByName(name)
            }
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }
    }
    applicationVariants.forEach { variant ->
        if (variant.buildType.name == "debug") {
            variant.mergedFlavor.resourceConfigurations.clear()
            variant.mergedFlavor.resourceConfigurations.add("es")
            variant.mergedFlavor.resourceConfigurations.add("xhdpi")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_21.toString()
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    room {
        schemaDirectory("$projectDir/schemas")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE.md,LICENSE-notice.md}"
        }
    }
}

dependencies {
    implementation(projects.core.core)
    implementation(projects.core.coreDomain)
    implementation(projects.core.coreTesting)
    implementation(projects.core.coreView)

    implementation(projects.feature.auth)
    implementation(projects.feature.podcast)

    implementation(projects.library.player)
    implementation(projects.library.framework)
    implementation(projects.library.remoteConfig.api)
    implementation(projects.library.remoteConfig.impl)

    implementation(libs.core.ktx)
    implementation(libs.google.material)
    implementation(libs.compose.material)
    implementation(libs.material.icons.extended)
    implementation(libs.navigation.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.compose.runtime)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.moshi)
    ksp(libs.moshi.codegen)
    implementation(libs.logging.interceptor)
    implementation(libs.rssparser)
    implementation(libs.accompanist.insets)
    implementation(libs.coil.compose)
    implementation(libs.arrow.core)
    implementation(libs.datastore.preferences)
    implementation(libs.room.runtime)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)
    implementation(libs.gson)
    implementation(libs.clarity)
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)
    implementation(libs.playinapp.review)
    implementation(libs.playinapp.review.ktx)
    implementation(libs.reorderable)

    testImplementation(libs.junit)
    testImplementation(libs.kluent)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.json)

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.kluent.android)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.android.compiler)
    androidTestImplementation(libs.mockwebserver)
    androidTestImplementation(libs.okhttp3.idling.resource)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}

fun isLocalBuild(): Boolean = System.getenv("CI") == null
