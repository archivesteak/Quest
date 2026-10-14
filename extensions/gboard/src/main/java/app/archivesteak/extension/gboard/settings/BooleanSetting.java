package app.archivesteak.extension.gboard.settings;

import android.content.SharedPreferences;

import app.archivesteak.extension.gboard.shared.Utils;

/**
 * An on/off setting, shown as a switch on the Quest settings screen.
 */
public final class BooleanSetting {
    public final String key;
    public final boolean defaultValue;

    BooleanSetting(String key, boolean defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    /**
     * @return The setting's value, or its default before the app has started.
     */
    public boolean get() {
        SharedPreferences preferences = Utils.getPreferences();
        return preferences == null ? defaultValue : preferences.getBoolean(key, defaultValue);
    }
}
