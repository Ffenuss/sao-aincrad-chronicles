plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    api("com.badlogicgames.gdx:gdx:1.12.1")
}

kotlin {
    jvmToolchain(17)
}
