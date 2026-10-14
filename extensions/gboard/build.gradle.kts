extension {
    name = "extensions/gboard.mpe"
}

android {
    namespace = "app.archivesteak.extension.gboard"
}

dependencies {
    compileOnly(project(":extensions:gboard:stub"))
}
