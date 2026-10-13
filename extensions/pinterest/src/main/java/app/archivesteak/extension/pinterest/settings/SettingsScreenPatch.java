package app.archivesteak.extension.pinterest.settings;

import static app.archivesteak.extension.pinterest.shared.Utils.TAG;

import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import app.archivesteak.extension.pinterest.shared.Utils;

/**
 * Shows the patch settings on the app's Social permissions screen when the patches row opens it,
 * so they show with the app's own header, rows and switches.
 */
@SuppressWarnings("unused")
public final class SettingsScreenPatch {
    private static final String SCREEN_TITLE_RESOURCE = "settings_social_permissions_title";

    /**
     * How long after the patches row is tapped its screen is expected to open.
     */
    private static final long OPEN_TIMEOUT_MILLIS = 5000;

    /**
     * When the patches row was last tapped, or -1 once its screen opened.
     */
    private static long openedAt = -1;

    /**
     * Sources of the rows of the screens showing the patch settings.
     */
    private static final Set<Object> rowSources = Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * The setting of each toggle row made by {@link #createRows()}.
     */
    private static final Map<Object, BooleanSetting> toggleRows = new WeakHashMap<>();

    /**
     * Called when the patches row is tapped, before the screen opens.
     */
    static void onOpen() {
        openedAt = SystemClock.uptimeMillis();
    }

    private static boolean isOpening() {
        return openedAt >= 0 && SystemClock.uptimeMillis() - openedAt < OPEN_TIMEOUT_MILLIS;
    }

    /**
     * Injection point. Called when a Social permissions screen creates the source of its rows,
     * which happens before the screen's header is made.
     */
    public static void onRowSourceCreated(Object rowSource) {
        if (isOpening()) rowSources.add(rowSource);
    }

    /**
     * Injection point. Called with the title of a settings screen's header.
     *
     * @return The title to show.
     */
    public static int getScreenTitle(int title) {
        if (!isOpening() || title != Utils.getResourceId(SCREEN_TITLE_RESOURCE, "string")) return title;

        openedAt = -1;
        int patchTitle = Settings.getStringResource("title");
        return patchTitle == 0 ? title : patchTitle;
    }

    /**
     * Injection point. Called when a screen loads its rows.
     *
     * @return Whether the patch settings were shown instead.
     */
    public static boolean loadRows(Object rowSource) {
        if (!rowSources.contains(rowSource)) return false;

        try {
            setRows(rowSource, createRows());
            return true;
        } catch (Exception ex) {
            Log.e(TAG, "Failed to show settings", ex);
            return false;
        }
    }

    /**
     * Injection point. Called for the view type of a row of a Social permissions screen,
     * which only knows the view types of its own rows.
     *
     * @return The view type, or -1 to let the screen get it.
     */
    public static int getRowViewType(Object rowSource, int position) {
        if (!rowSources.contains(rowSource)) return -1;

        try {
            Object row = rowSource.getClass().getMethod("getItem", int.class).invoke(rowSource, position);
            return (int) row.getClass().getMethod("getViewType").invoke(row);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to get settings row view type", ex);
            return -1;
        }
    }

    /**
     * Injection point. Called when a toggle row of any settings screen is checked or unchecked.
     */
    public static void onRowChanged(Object row) {
        BooleanSetting setting = toggleRows.get(row);
        if (setting != null) setting.set(isChecked(row));
    }

    private static List<Object> createRows() {
        List<Object> rows = new ArrayList<>();
        String category = null;
        for (BooleanSetting setting : Settings.ALL) {
            int title = setting.getTitleResource();
            if (title == 0) continue;

            if (!setting.category.equals(category)) {
                category = setting.category;
                int categoryTitle = Settings.getStringResource("category_" + category);
                if (categoryTitle != 0) rows.add(createHeaderRow(categoryTitle, null));
            }

            Object row = createToggleRow(title, setting.getSummary(), setting.get(), null, null, null);
            toggleRows.put(row, setting);
            rows.add(row);
        }
        return rows;
    }

    // region Implemented by the patch, which uses the unused parameters as registers.

    private static void setRows(Object rowSource, List<Object> rows) {
    }

    private static Object createHeaderRow(int title, Object row) {
        return null;
    }

    private static Object createToggleRow(int title, String summary, boolean checked,
                                          Object description, Object unused, Object row) {
        return null;
    }

    private static boolean isChecked(Object row) {
        return false;
    }

    // endregion
}
