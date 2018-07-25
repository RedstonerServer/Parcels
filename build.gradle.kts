@file:Suppress("UNUSED_VARIABLE")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import java.io.PrintWriter

plugins {
    kotlin("jvm") version "1.2.51"
    id("com.github.johnrengelman.plugin-shadow") version "2.0.3"
}

kotlin.experimental.coroutines = Coroutines.ENABLE

group = "io.dico"
version = "0.1"

allprojects {
    apply {
        plugin(JavaPlugin::class.java)
    }
    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://dl.bintray.com/kotlin/exposed")
    }
    dependencies {
        compile(files("${rootProject.rootDir.parentFile}/res/spigot-1.13-pre7.jar"))
        compile("net.sf.trove4j:trove4j:3.0.3")
        testCompile("junit:junit:4.12")
    }
}

project(":dicore3:dicore3-command") {
    dependencies {
        compile(project(":dicore3:dicore3-core"))
        compile("com.thoughtworks.paranamer:paranamer:2.8")
    }
}

dependencies {
    compile(project(":dicore3:dicore3-core"))
    compile(project(":dicore3:dicore3-command"))
    compile(kotlin("stdlib-jdk8"))

    compile("org.jetbrains.exposed:exposed:0.10.3")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.23.4")
    compile("com.zaxxer:HikariCP:3.2.0")
    compile("com.h2database:h2:1.4.197")

    val jacksonVersion = "2.9.6"
    compile("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    compile("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    compile("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")

    compile("org.slf4j:slf4j-api:1.7.25")
    compile("ch.qos.logback:logback-classic:1.2.3")
}

tasks {
    fun Jar.packageDependencies(vararg names: String) {
        from(*project.configurations.compile.resolvedConfiguration.firstLevelModuleDependencies
            .filter { it.moduleName in names }
            .flatMap { it.allModuleArtifacts }
            .map { it.file }
            .map(::zipTree)
            .toTypedArray()
        )
    }

    fun Jar.packageDependency(name: String, configure: ModuleDependency.() -> Unit) {
        val configuration = project.configurations.compile.copyRecursive()

        configuration.dependencies.removeIf {
            if (it is ModuleDependency && it.name == name) {
                it.configure()
                false
            } else true
        }

        from(*configuration.resolvedConfiguration.resolvedArtifacts
            .map { it.file }
            .map(::zipTree)
            .toTypedArray())
    }

    fun Jar.packageArtifacts(vararg names: String) {
        val stream = PrintWriter(File("$rootDir/gradle-output.txt"))
        from(*project.configurations.compile.resolvedConfiguration.resolvedArtifacts
            .filter {
                val id = it.moduleVersion.id
                (id.name in names).also {
                    if (!it) stream.println("Not including artifact: ${id.group}:${id.name}")
                }
            }
            .map { it.file }
            .map(::zipTree)
            .toTypedArray())
        stream.flush()
    }

    val serverDir = "$rootDir/debug"

    val jar by getting(Jar::class)

    val kotlinStdlibJar by creating(Jar::class) {
        destinationDir = file("$serverDir/lib")
        archiveName = "kotlin-stdlib.jar"
        packageDependencies("kotlin-stdlib-jdk8")
    }

    val shadowJar by getting(ShadowJar::class) {
        relocate("", "")
    }

    val releaseJar by creating(ShadowJar::class) {
        destinationDir = file("$serverDir/plugins")
        baseName = "parcels2-release"

        with(jar)

        packageArtifacts(
            "jackson-core",
            "jackson-databind",
            "jackson-module-kotlin",
            "jackson-annotations",
            "jackson-dataformat-yaml",
            "snakeyaml",

            "slf4j-api",
            "logback-core",
            "logback-classic",

            "h2",
            "HikariCP",
            "kotlinx-coroutines-core",
            "kotlinx-coroutines-core-common",
            "atomicfu-common",
            "exposed",

            "dicore3-core",
            "dicore3-command",
            "paranamer",

            "trove4j",
            "joda-time",
            "annotations"
        )

        relocate("org.yaml.snakeyaml", "io.dico.parcels2.util.snakeyaml")

        manifest.attributes["Class-Path"] = "lib/kotlin-stdlib.jar"
        dependsOn(kotlinStdlibJar)
    }

}

allprojects {
    tasks.filter { it is Jar }.forEach { it.group = "artifacts" }
}