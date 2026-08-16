plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "br.univates.ead.treinodb"
    compileSdk = 37

    defaultConfig {
        // applicationId diferente do app da Unidade 2 de proposito: assim os
        // dois ficam instalados lado a lado no mesmo emulador.
        applicationId = "br.univates.ead.treinodb"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        viewBinding = true
    }
}

// Nenhuma dependencia nova em relacao ao app da Unidade 2.
// SQLite e notificacao ja vem no proprio Android / androidx.core.
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
}
