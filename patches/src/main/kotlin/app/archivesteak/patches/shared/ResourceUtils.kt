package app.archivesteak.patches.shared

/**
 * Escapes text for use as the value of a string resource.
 */
internal fun String.escapeForResource() = replace("\\", "\\\\")
    .replace("'", "\\'")
    .replace("\"", "\\\"")
    .replace(Regex("^([@?])"), "\\\\$1")
