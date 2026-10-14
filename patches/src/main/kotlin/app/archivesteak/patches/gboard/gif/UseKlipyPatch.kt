package app.archivesteak.patches.gboard.gif

import app.archivesteak.patches.gboard.misc.settings.SETTINGS_SCREEN_TITLE
import app.archivesteak.patches.gboard.misc.settings.SwitchPreference
import app.archivesteak.patches.gboard.misc.settings.TextPreference
import app.archivesteak.patches.gboard.misc.settings.preferencesPatch
import app.archivesteak.patches.gboard.misc.settings.settingsPatch
import app.archivesteak.patches.gboard.misc.signature.signatureCheckPatch
import app.archivesteak.patches.gboard.shared.Constants.COMPATIBILITY_GBOARD
import app.archivesteak.patches.gboard.shared.EXTENSION_PACKAGE
import app.archivesteak.patches.gboard.shared.editString
import app.archivesteak.patches.gboard.shared.hookFlagReads
import app.archivesteak.patches.gboard.shared.strings
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import java.util.Properties

private const val EXTENSION_CLASS = "$EXTENSION_PACKAGE/KlipyPatch;"

/** Server flags holding the addresses of Tenor's API, which KLIPY's API mirrors. */
private val URL_FLAGS = listOf(
    "tenor_server_url_search_v2",
    "tenor_server_url_trending_terms_v2",
    "tenor_server_url_categories_v2",
    "tenor_server_url_autocomplete_v2",
    "tenor_server_url_posts_v2",
    "tenor_server_url_featured",
    "tenor_server_url_register_share",
)
private const val API_KEY_FLAG = "tenor_v2_dev_api_key"

/** Not read in 18.4.1, but hooked in case a version uses it. */
private val UNUSED_URL_FLAGS = setOf("tenor_server_url_posts_v2")

private val API_KEY_REGEX = Regex("^[A-Za-z0-9_-]*$")

/** Quest's KLIPY API key, set when building the patches, or empty. */
private val QUEST_API_KEY = object {}.javaClass.getResourceAsStream("/gboard/klipy.properties")
    ?.use { Properties().apply { load(it) }.getProperty("apiKey") }
    .orEmpty()

/**
 * KLIPY requires its name in the search box, so the GIF search hint, "Search Tenor", names KLIPY.
 */
private val searchHintPatch = resourcePatch {
    execute {
        val hintName = document("res/values/strings.xml").use { document ->
            document.strings().singleOrNull { it.textContent == "Search Tenor" }?.getAttribute("name")
        } ?: throw PatchException("GIF search hint not found")

        editString(hintName) { it.replace("Tenor", "KLIPY") }
    }
}

@Suppress("unused")
val useKlipyPatch = bytecodePatch(
    name = "Use KLIPY for GIFs",
    description = "Adds an option to get GIFs and stickers from KLIPY instead of Tenor.",
    default = true
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    val apiKey by stringOption(
        key = "klipyApiKey",
        default = QUEST_API_KEY,
        title = "KLIPY API key",
        description = "The key to use, from partner.klipy.com. Defaults to Quest's key. " +
            "Can also be set in Gboard's settings under $SETTINGS_SCREEN_TITLE.",
        required = false
    ) { it == null || API_KEY_REGEX.matches(it) }

    dependsOn(
        signatureCheckPatch,
        settingsPatch,
        preferencesPatch(
            "GIFs",
            SwitchPreference(
                key = "quest_use_klipy",
                title = "Use KLIPY",
                summary = "Get GIFs and stickers from KLIPY instead of Tenor",
                defaultValue = true,
            ),
            TextPreference(
                key = "quest_klipy_api_key",
                title = "KLIPY API key",
                summary = "Optional. Your own key from partner.klipy.com, used instead of Quest's",
            ),
        ),
        searchHintPatch,
    )

    execute {
        val urlHook = "$EXTENSION_CLASS->overrideUrl(Ljava/lang/Object;)Ljava/lang/Object;"
        hookFlagReads(
            URL_FLAGS.associateWith { urlHook } +
                (API_KEY_FLAG to "$EXTENSION_CLASS->overrideApiKey(Ljava/lang/Object;)Ljava/lang/Object;"),
            unusedFlags = UNUSED_URL_FLAGS,
        )

        mutableClassDefBy(EXTENSION_CLASS).methods.single { it.name == "getDefaultApiKey" }.addInstructions(
            0,
            """
                const-string v0, "${apiKey.orEmpty()}"
                return-object v0
            """
        )
    }
}
