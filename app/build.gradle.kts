import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("jacoco")
}

android {
    namespace = "com.example.adbkit"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.adbkit"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // isTestCoverageEnabled yerine güncel metodlar kullanıldı
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // JVM unit testler için Android kaynaklarını yükle
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "21"
    }
}

jacoco {
    toolVersion = "0.8.10"
}

tasks.register<JacocoReport>("jacocoTestReportAll") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    // Derlenen sınıflar (Java/Kotlin) için doğru dizinleri ve hariç tutulacak dosyaları tanımla
    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("intermediates/javac/debug/classes")) {
            exclude(
                "**/R.class", "**/R$*.class",
                "**/BuildConfig.*", "**/Manifest*.*",
                "**/*Test*.*", "android/**/*.*", "androidx/**/*.*",
                "**/*\$Lambda$*.*", // Lambda ifadeleri
                "**/*\$inlined$*.*" // Inlined fonksiyonlar
            )
        },
        fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
            exclude(
                "**/R.class", "**/R$*.class",
                "**/BuildConfig.*", "**/Manifest*.*",
                "**/*Test*.*", "android/**/*.*", "androidx/**/*.*",
                "**/*\$Lambda$*.*",
                "**/*\$inlined$*.*"
            )
        }
    )

    // Hem Java hem de Kotlin kaynak kodunu JaCoCo'ya bildir
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))

    executionData.setFrom(
        fileTree(layout.buildDirectory.asFile) {
            include(
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                "jacoco/testDebugUnitTest.exec"
            )
        }
    )
}

// **JVM uyumsuzluğu sorununu çözmenin en güvenilir yolu**
// Bu blok, tüm 'Test' görevlerine açıkça JDK 21 kullanması gerektiğini söyler.
tasks.withType<Test>().configureEach {
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    )

    // Robolectric + Jacoco için
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }

    // Ek güvenli açmalar (özellikle JDK 21+ için faydalı)
    jvmArgs(
        "--add-opens", "java.base/jdk.internal.reflect=ALL-UNNAMED",
        "--add-opens", "java.base/java.io=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang=ALL-UNNAMED"
    )

    testLogging {
        events("failed", "skipped", "standardOut", "standardError")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showCauses = true
        showExceptions = true
        showStackTraces = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // JVM unit tests
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.mockito:mockito-core:5.12.0")

    // Instrumentation tests (opsiyonel)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Desugaring (Java 8+ API)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}