package app.archivesteak.extension.pinterest.settings;

import android.content.Context;

import app.archivesteak.extension.pinterest.shared.Utils;

/**
 * An on/off setting shown as a switch on the settings screen.
 * Its title and summary are app strings added by the patch the setting is for.
 */
public final class BooleanSetting {
    public final String key;
    public final boolean defaultValue;
    public final String category;

    BooleanSetting(String key, boolean defaultValue, String category) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.category = category;
        Settings.ALL.add(this);
    }

    public boolean get() {
        return Utils.getContext()
                .getSharedPreferences(Settings.PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getBoolean(key, defaultValue);
    }

    public void set(boolean value) {
        Utils.getContext()
                .getSharedPreferences(Settings.PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(key, value)
                .apply();
    }

    /**
     * @return Resource id of the setting's title, or 0 if the patch the setting is for isn't applied.
     */
    int getTitleResource() {
        return Settings.getStringResource(key + "_title");
    }

    /**
     * @return The setting's summary, or an empty string if it has none.
     */
    String getSummary() {
        int summary = Settings.getStringResource(key + "_summary");
        return summary == 0 ? "" : Utils.getContext().getString(summary);
    }
}
