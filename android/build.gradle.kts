import java.util.zip.ZipFile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val gdxVersion = "1.12.1"
val gdxNatives by configurations.creating

android {
    namespace = "com.sao.aincrad.android"
    compileSdk = 34

    sourceSets["main"].apply {
        assets.srcDirs("../core/assets")
        jniLibs.srcDirs("src/main/jniLibs")
    }

    defaultConfig {
        applicationId = "com.sao.aincrad.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")

    gdxNatives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    gdxNatives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    gdxNatives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    gdxNatives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
}

val extractGdxNatives by tasks.registering {
    doLast {
        val outRoot = file("src/main/jniLibs")
        outRoot.deleteRecursively()
        outRoot.mkdirs()

        gdxNatives.resolve().forEach { jarFile ->
            ZipFile(jarFile).use { zip ->
                zip.entries().asSequence()
                    .filter { !it.isDirectory && it.name.startsWith("lib/") && it.name.endsWith(".so") }
                    .forEach { entry ->
                        val parts = entry.name.split("/")
                        if (parts.size >= 3) {
                            val abi = parts[1]
                            val soName = parts.last()
                            val abiDir = File(outRoot, abi)
                            abiDir.mkdirs()
                            zip.getInputStream(entry).use { input ->
                                File(abiDir, soName).outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
            }
        }
    }
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(extractGdxNatives)
}
