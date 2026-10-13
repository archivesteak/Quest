package app.archivesteak.extension.pinterest.settings;

import app.archivesteak.extension.pinterest.shared.Utils;

/**
 * An on/off setting shown as a switch on the settings screen.
 */
public final class BooleanSetting {
    public final String key;
    public final boolean defaultValue;
    public final String category;
    public final String title;
    public final String summary;

    BooleanSetting(String key, boolean defaultValue, String category, String title, String summary) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.category = category;
        this.title = title;
        this.summary = summary;
        Settings.ALL.add(this);
    }

    public boolean get() {
        return Utils.getContext()
                .getSharedPreferences(Settings.PREFERENCES_NAME, 0)
                .getBoolean(key, defaultValue);
    }
}
