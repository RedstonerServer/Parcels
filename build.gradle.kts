import org.jetbrains.kotlin.gradle.dsl.Coroutines

plugins {
    kotlin("jvm") version "1.2.51"
}

group = "io.dico"
version = "0.1"

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/kotlin/exposed")
}

kotlin.experimental.coroutines = Coroutines.ENABLE

dependencies {
    compile(files("../res/spigot-1.13-pre7.jar"))
    compile(kotlin("stdlib-jdk8"))
    compile("org.jetbrains.exposed:exposed:0.10.3")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.23.4")
    testCompile("junit:junit:4.12")
}

val jar by tasks.getting(Jar::class)
val fatJar by tasks.creating(Jar::class) {
    baseName = "parcels2-all"
    manifest.attributes["Main-Class"] = ""
    destinationDir = file("debug/plugins")
    from(*configurations.compile.map(::zipTree).toTypedArray())
    with(jar)
}
