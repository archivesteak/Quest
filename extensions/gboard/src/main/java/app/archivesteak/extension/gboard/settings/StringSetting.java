package app.archivesteak.extension.gboard.settings;

import android.content.SharedPreferences;

import app.archivesteak.extension.gboard.shared.Utils;

/**
 * A text setting, shown as a text field on the Quest settings screen.
 */
public final class StringSetting {
    public final String key;

    StringSetting(String key) {
        this.key = key;
    }

    /**
     * @return The setting's value without surrounding whitespace, or an empty string if it isn't set.
     */
    public String get() {
        SharedPreferences preferences = Utils.getPreferences();
        String value = preferences == null ? null : preferences.getString(key, null);
        return value == null ? "" : value.trim();
    }
}
