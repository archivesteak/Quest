package app.archivesteak.extension.pinterest.settings;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.view.MenuItem;

/**
 * Screen listing every patch setting. Opened from the app's settings.
 */
@SuppressWarnings("deprecation")
public final class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static final class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            getPreferenceManager().setSharedPreferencesName(Settings.PREFERENCES_NAME);

            Context context = getActivity();
            PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
            setPreferenceScreen(screen);

            PreferenceCategory category = null;
            for (BooleanSetting setting : Settings.ALL) {
                if (category == null || !setting.category.contentEquals(category.getTitle())) {
                    category = new PreferenceCategory(context);
                    category.setTitle(setting.category);
                    screen.addPreference(category);
                }

                SwitchPreference preference = new SwitchPreference(context);
                preference.setKey(setting.key);
                preference.setTitle(setting.title);
                preference.setSummary(setting.summary);
                preference.setDefaultValue(setting.defaultValue);
                preference.setChecked(setting.get());
                category.addPreference(preference);
            }
        }
    }
}
