plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    api("com.badlogicgames.gdx:gdx:1.12.1")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
