plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "br.univates.ead.treino"
    compileSdk = 37

    defaultConfig {
        applicationId = "br.univates.ead.treino"
        minSdk = 24
        targetSdk = 36
        // Na Unidade 2 o material coloca versionCode/versionName no
        // AndroidManifest.xml. Desde o Gradle virar o sistema de build oficial
        // do Android, eles moram aqui — o manifesto final e gerado com estes
        // valores durante a compilacao.
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
        // Gera uma classe por layout (activity_main.xml -> ActivityMainBinding).
        // E o substituto do findViewById que o material usa.
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
}
