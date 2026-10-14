package app.archivesteak.extension.gboard;

import app.archivesteak.extension.gboard.settings.Settings;

@SuppressWarnings("unused")
public final class ClipboardPatch {
    /**
     * Injection point. Overrides the server flag holding the length copied text is cut to.
     */
    public static Object overrideLengthLimit(Object limit) {
        return Settings.REMOVE_CLIPBOARD_LENGTH_LIMIT.get() ? Long.valueOf(Integer.MAX_VALUE) : limit;
    }
}
