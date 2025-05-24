val antlrKotlinVersion = "1.0.1"

plugins {
    kotlin("jvm") version "1.9.22"
    application
    id("com.google.cloud.artifactregistry.gradle-plugin") version "2.2.1"
    id("com.strumenta.antlr-kotlin") version "1.0.1"
    // Used as part of the release process: bumps the version number, commits the change, and creates a tag
    // ./gradlew release
    id("net.researchgate.release") version "3.1.0"
    // Used to create a GitHub release with the artifacts
    id("com.github.breadmoirai.github-release") version "2.4.1"
    kotlin("plugin.serialization") version "1.9.22"
}

group = "io.github.dtrounine.lineage"

repositories {
    mavenCentral()
    maven("artifactregistry://europe-west9-maven.pkg.dev/dtrunin-data/lineage")
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation("com.strumenta:antlr-kotlin-runtime:$antlrKotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("io.openlineage:openlineage-java:1.33.0")
    implementation("org.slf4j:slf4j-simple:1.6.2")
    implementation("com.github.ajalt.clikt:clikt:4.4.0")
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

application {
    mainClass = "io.github.dtrounine.lineage.MainKt"
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

release {
    failOnCommitNeeded = true
}

githubRelease {
    token(System.getenv("GITHUB_TOKEN")) // This is your personal access token with Repo permissions
    // You get this from your user settings > developer settings > Personal Access Tokens
    owner.set("dtrounine")  // default is the last part of your group. Eg group: "com.github.breadmoirai" => owner: "breadmoirai"
    repo.set("redshift-lineage")   // by default this is set to your project name
    tagName.set(project.version.toString()) // by default this is set to "v${project.version}"
    draft.set(true) // by default this is true
    prerelease.set(false) // by default this is false
    releaseAssets.setFrom(
        provider {
            fileTree("build/distributions") {
                include("*-" + project.version.toString() + ".zip")
            }.files
        }
    )
    overwrite.set(false) // by default false; if set to true, will delete an existing release with the same tag and name
    dryRun.set(false) // by default false; you can use this to see what actions would be taken without making a release
}

tasks.named("compileKotlin") {
    dependsOn(generateKotlinGrammarSource)
}

tasks.named("githubRelease") {
    dependsOn("distZip") // Ensure the project is built before tagging
}

tasks.named("updateVersion") {
    dependsOn("githubRelease") // Ensure the release is uploaded before the version is updated
}

tasks.named("githubRelease") {
    dependsOn("createReleaseTag") // Ensure the tag is created before uploading the release asset
    mustRunAfter("createReleaseTag") // Ensure it runs after the tag is created
}

tasks.named("githubRelease") {
    doFirst {
        println("Verifying that the Git tag exists on the remote...")
        val tag = project.version.toString()
        val tagExists = "git ls-remote --tags origin $tag".runCommand().isNotBlank()
        if (!tagExists) {
            throw GradleException("Git tag $tag does not exist on the remote repository.")
        }

        println("Verifying the ZIP distribution file exists...")
        val zipFile = file("build/distributions/redshift-lineage-$tag.zip")
        if (!zipFile.exists()) {
            throw GradleException("ZIP distribution file $zipFile does not exist.")
        }

    }
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

fun String.runCommand(): String {
    return ProcessBuilder(*this.split(" ").toTypedArray())
        .redirectErrorStream(true)
        .start()
        .inputStream
        .bufferedReader()
        .readText()
        .trim()
}
