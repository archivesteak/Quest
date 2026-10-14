package app.archivesteak.extension.gboard.settings;

/**
 * Patch settings, stored with Gboard's own settings. The patches add them to the Quest settings screen,
 * so their keys must match the preferences the patches add.
 */
public final class Settings {
    // region Clipboard

    public static final BooleanSetting REMOVE_CLIPBOARD_LENGTH_LIMIT = new BooleanSetting("quest_remove_clipboard_length_limit", true);

    // endregion

    // region GIFs

    public static final BooleanSetting USE_KLIPY = new BooleanSetting("quest_use_klipy", true);
    public static final StringSetting KLIPY_API_KEY = new StringSetting("quest_klipy_api_key");

    // endregion
}
