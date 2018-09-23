pluginManagement.repositories {
    maven("http://dl.bintray.com/kotlin/kotlin-eap")
    mavenCentral()
    maven("https://plugins.gradle.org/m2/")
}

rootProject.name = "parcels2"

include("dicore3:core")
findProject(":dicore3:core")?.name = "dicore3-core"

include("dicore3:command")
findProject(":dicore3:command")?.name = "dicore3-command"
