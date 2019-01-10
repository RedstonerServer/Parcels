@file:Suppress("RemoveRedundantBackticks", "IMPLICIT_CAST_TO_ANY", "UNUSED_VARIABLE")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.PrintWriter
import java.net.URL

val stdout = PrintWriter("gradle-output.txt")

group = "io.dico"
version = "0.3"

plugins {
    java
    kotlin("jvm") version "1.3.0"
    id("com.github.johnrengelman.plugin-shadow") version "2.0.3"
}


allprojects {
    apply<JavaPlugin>()
    apply(plugin = "idea")

    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/sonatype-nexus-snapshots/")
        maven("https://dl.bintray.com/kotlin/exposed/")
        maven("https://dl.bintray.com/kotlin/kotlin-dev/")
        maven("https://dl.bintray.com/kotlin/kotlin-eap/")
        maven("https://dl.bintray.com/kotlin/kotlinx/")
        maven("http://maven.sk89q.com/repo")
    }

    dependencies {
        val spigotVersion = "1.13.2-R0.1-SNAPSHOT"
        c.provided("org.bukkit:bukkit:$spigotVersion") { isTransitive = false }
        c.provided("org.spigotmc:spigot-api:$spigotVersion") { isTransitive = false }

        c.provided("net.sf.trove4j:trove4j:3.0.3")
        testCompile("junit:junit:4.12")
    }

    afterEvaluate {
        tasks.filter { it is Jar }.forEach { it.group = "artifacts" }
    }
}

project(":dicore3:dicore3-core") {
    dependencies {
        compile("org.jetbrains:annotations:16.0.3")
    }
}

val coroutinesCore = kotlinx("coroutines-core:0.26.1-eap13")

project(":dicore3:dicore3-command") {
    apply<KotlinPlatformJvmPlugin>()

    dependencies {
        c.kotlinStd(kotlin("stdlib-jdk8"))
        c.kotlinStd(kotlin("reflect"))
        c.kotlinStd(coroutinesCore)

        compile(project(":dicore3:dicore3-core"))
        compile("com.thoughtworks.paranamer:paranamer:2.8")
        c.provided("com.google.guava:guava:25.1-jre")
    }
}

dependencies {
    compile(project(":dicore3:dicore3-core"))
    compile(project(":dicore3:dicore3-command"))

    c.kotlinStd(kotlin("stdlib-jdk8"))
    c.kotlinStd(kotlin("reflect"))
    c.kotlinStd(coroutinesCore)
    c.kotlinStd("org.jetbrains.kotlinx:atomicfu-common:0.11.12")

    // not on sk89q maven repo yet
    //compileClasspath(files("$rootDir/debug/plugins/worldedit-bukkit-7.0.0-beta-01.jar"))
    // compileClasspath(files("$rootDir/debug/lib/spigot-1.13.2.jar"))
    compileClasspath("com.sk89q.worldedit:worldedit-bukkit:7.0.0-SNAPSHOT")

    compile("org.jetbrains.exposed:exposed:0.10.5") { isTransitive = false }
    compile("joda-time:joda-time:2.10")
    compile("com.zaxxer:HikariCP:3.2.0")
    compile("ch.qos.logback:logback-classic:1.2.3") { isTransitive = false }
    compile("ch.qos.logback:logback-core:1.2.3") { isTransitive = false }

    val jacksonVersion = "2.9.6"
    compile("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    compile("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    compile("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion") { isTransitive = false }
}

tasks {
    removeIf { it is ShadowJar }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            javaParameters = true
            suppressWarnings = true
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-XXLanguage:+InlineClasses", "-Xuse-experimental=kotlin.Experimental")
        }
    }

    val serverDir = "$rootDir/debug"
    val jar by getting(Jar::class)

    val kotlinStdlibJar by creating(Jar::class) {
        destinationDir = file("$serverDir/lib")
        archiveName = "kotlin-stdlib.jar"
        fromFiles(c.kotlinStd)
    }

    val releaseJar by creating(ShadowJar::class) {
        destinationDir = file("$serverDir/plugins")
        baseName = "parcels2-release"

        with(jar)
        fromFiles(c.compile)

        /*
        Shadow Jar is retarded so it also relocates packages not included in the releaseJar (such as the bukkit api)

        relocate("", "io.dico.parcels2.lib.") {
            exclude("*yml")
            exclude("*xml")
            exclude("META-INF/*")
            exclude("io/dico/*")
        }
        */*/*/

        // jackson-dataformat-yaml requires an older version of snakeyaml (1.19 or earlier)
        // snakeyaml made a breaking change in 1.20 and didn't really warn anyone afaik
        // it was like me changing the command library because I know I'm the only one using it
        // spigot ships a later version in the root, so we must relocate ours
        relocate("org.yaml.snakeyaml.", "io.dico.parcels2.lib.org.yaml.snakeyaml.")

        manifest.attributes["Class-Path"] = "../lib/kotlin-stdlib.jar ../lib/mariadb-java-client-2.2.6.jar"
        dependsOn(kotlinStdlibJar)
    }

    val createDebugServer by creating {
        // todo

        val jarUrl = URL("https://yivesmirror.com/files/spigot/spigot-latest.jar")
        val serverJarFile = file("$serverDir/lib/spigot.jar")
    }
}

stdout.flush()
stdout.close()

inline fun <reified T : Plugin<out Project>> Project.apply() =
    (this as PluginAware).apply<T>()

fun kotlinx(module: String, version: String? = null): Any =
    "org.jetbrains.kotlinx:kotlinx-$module${version?.let { ":$version" } ?: ""}"

val Project.c get() = configurations

val ConfigurationContainer.`provided`: Configuration
    get() = findByName("provided") ?: create("provided").let { compileClasspath.extendsFrom(it) }

val ConfigurationContainer.`kotlinStd`: Configuration
    get() = findByName("kotlinStd") ?: create("kotlinStd").let { compileClasspath.extendsFrom(it) }

fun Jar.fromFiles(files: Iterable<File>) {
    afterEvaluate { from(*files.map { if (it.isDirectory) it else zipTree(it) }.toTypedArray()) }
}

