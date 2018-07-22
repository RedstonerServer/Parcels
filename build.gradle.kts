import com.github.jengelman.gradle.plugins.shadow.relocation.RelocateClassContext
import com.github.jengelman.gradle.plugins.shadow.relocation.RelocatePathContext
import com.github.jengelman.gradle.plugins.shadow.relocation.Relocator
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import shadow.org.apache.tools.zip.ZipOutputStream

plugins {
    kotlin("jvm") version "1.2.51"
    id("com.github.johnrengelman.plugin-shadow") version "2.0.3"
}

group = "io.dico"
version = "0.1"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://dl.bintray.com/kotlin/exposed")
}

kotlin.experimental.coroutines = Coroutines.ENABLE

dependencies {
    compile(kotlin("stdlib-jdk8"))

    shadow(files("../res/spigot-1.13-pre7.jar"))

    compile("io.dico.dicore:dicore3-core:1.2-mc-1.13")
    compile("io.dico.dicore:dicore3-command:1.2-mc-1.13")

    compile("org.jetbrains.exposed:exposed:0.10.3")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.23.4")
    compile("com.zaxxer:HikariCP:3.2.0")

    val jacksonVersion = "2.9.6"
    compile("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    compile("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    compile("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    compile("org.slf4j:slf4j-api:1.7.25")
    compile("ch.qos.logback:logback-classic:1.2.3")

    testCompile("junit:junit:4.12")
}

tasks {
    val jar by getting(Jar::class)

    val fatJar by creating(Jar::class) {
        destinationDir = file("$rootDir/debug/plugins")
        baseName = "parcels2-all"
        from(*configurations.compile.map(::zipTree).toTypedArray())
        with(jar)
    }

    val shadowJar by getting(ShadowJar::class) {
        destinationDir = file("$rootDir/debug/plugins")
        baseName = "parcels2-shaded"

        dependencies {
            exclude(dependency(files("../res/spigot-1.13-pre7.jar")))
        }

        relocate("", "io.dico.parcels2.util.") {
            exclude("META-INF/*")
            exclude("logback*")
            exclude("plugin*")
        }
    }
}