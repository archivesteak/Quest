group = "app.archivesteak"

patches {
    about {
        name = "Quest Patches"
        description = "Patches for Pinterest and Gboard"
        source = "git@github.com:archivesteak/Quest.git"
        author = "archivesteak"
        contact = "https://github.com/archivesteak/Quest/issues"
        website = "https://github.com/archivesteak/Quest"
        license = "GPLv3"
    }
}

// Separate configuration so gson is available at runtime for the
// generatePatchesList task but never bundled into the APK.
val patchListGeneratorClasspath = configurations.create("patchListGeneratorClasspath")

dependencies {
    compileOnly(libs.gson)
    patchListGeneratorClasspath(libs.gson)
}

// Quest's KLIPY API key, the default key of "Use KLIPY for GIFs". It's kept out of the repository
// and set when building, as the klipyApiKey Gradle property or the KLIPY_API_KEY environment variable.
val klipyApiKey = providers.gradleProperty("klipyApiKey")
    .orElse(providers.environmentVariable("KLIPY_API_KEY"))
    .orElse("")

tasks {
    processResources {
        inputs.property("klipyApiKey", klipyApiKey)
        filesMatching("gboard/klipy.properties") {
            expand("apiKey" to klipyApiKey.get())
        }
    }

    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }

    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}
