package app.archivesteak.extension.gboard.settings;

import com.google.android.libraries.inputmethod.preferencewidgets.CommonPreferenceFragment;

import app.archivesteak.extension.gboard.shared.Utils;

/**
 * The Quest screen of Gboard's settings. It shows the preferences the patches add to
 * {@code res/xml/quest_settings.xml}, with Gboard's own preference widgets.
 */
@SuppressWarnings("unused")
public final class QuestSettingsFragment extends CommonPreferenceFragment {
    /**
     * Renamed by the settings patch to the method of {@link CommonPreferenceFragment} that returns
     * the resource of a screen's preferences, which this then overrides.
     */
    public int getPreferencesResource() {
        return Utils.getResourceId("quest_settings", "xml");
    }
}
