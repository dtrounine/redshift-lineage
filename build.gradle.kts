val antlrKotlinVersion = "1.0.1"

plugins {
    kotlin("jvm") version "1.9.21"
    id("com.strumenta.antlr-kotlin") version "1.0.1"
}

group = "io.github.dtrounine.lineage"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation("com.strumenta:antlr-kotlin-runtime:$antlrKotlinVersion")
}

sourceSets {
    main {
        kotlin {
            // telling that output generateGrammarSource should be part of main source set
            // actual passed value will be equal to `outputDirectory` that we configured above
            srcDir(layout.buildDirectory.dir("generatedAntlr").get().asFile)
        }
    }
}


tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

val generateKotlinGrammarSource = tasks.register<com.strumenta.antlrkotlin.gradle.AntlrKotlinTask>("generateKotlinGrammarSource") {
    dependsOn("cleanGenerateKotlinGrammarSource")

    // ANTLR .g4 files are under {example-project}/antlr
    // Only include *.g4 files. This allows tools (e.g., IDE plugins)
    // to generate temporary files inside the base path
    source = fileTree(layout.projectDirectory.dir("src/main/antlr")) {
        include("**/*.g4")
    }

    // We want the generated source files to have this package name
    val pkgName = "io.github.dtrounine.lineage.sql.parser.generated"
    packageName = pkgName

    // We want visitors alongside listeners.
    // The Kotlin target language is implicit, as is the file encoding (UTF-8)
    arguments = listOf("-visitor")

    // Generated files are outputted inside build/generatedAntlr/{package-name}
    val outDir = "generatedAntlr/${pkgName.replace(".", "/")}"
    outputDirectory = layout.buildDirectory.dir(outDir).get().asFile
}
