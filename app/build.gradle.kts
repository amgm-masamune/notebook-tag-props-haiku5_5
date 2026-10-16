plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// CI（GitHub Actions）では GITHUB_RUN_NUMBER が入り、ビルドごとに増える versionCode になる。
// ローカル実行（環境変数なし）は従来どおり 1 / 1.0。
val ciRunNumber: Int? = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()

// CI 用の固定署名鍵。未設定なら既定のデバッグ鍵（実行環境ごとに変わる）で署名される。
// 鍵が毎回変わると、前回の CI 成果物の上に上書きインストールできなくなるため、
// ANDROID_KEYSTORE_PATH などを GitHub Secrets から渡して同じ鍵で署名する。
val ciKeystorePath: String? = System.getenv("ANDROID_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }

android {
    namespace = "com.amgm.notebook"
    compileSdk = 35

    signingConfigs {
        if (ciKeystorePath != null) {
            create("ci") {
                storeFile = file(ciKeystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    defaultConfig {
        applicationId = "com.amgm.notebook"
        minSdk = 26
        targetSdk = 35
        versionCode = ciRunNumber ?: 1
        versionName = if (ciRunNumber != null) "1.0.$ciRunNumber" else "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // スクリーンショットを AndroidX Test Storage 経由で回収する（connected_android_test_additional_output）
        testInstrumentationRunnerArguments["useTestStorageService"] = "true"
    }

    buildTypes {
        debug {
            if (ciKeystorePath != null) {
                signingConfig = signingConfigs.getByName("ci")
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    sourceSets {
        getByName("test").java.srcDir("src/sharedTest/java")
        getByName("androidTest").java.srcDir("src/sharedTest/java")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation("androidx.compose.material:material-icons-core")
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.services.storage)
    androidTestUtil(libs.androidx.test.services.apk)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
