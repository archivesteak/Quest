package app.archivesteak.extension.pinterest.settings;

import static app.archivesteak.extension.pinterest.shared.Utils.TAG;

import android.content.Intent;
import android.util.Log;
import android.view.View;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import app.archivesteak.extension.pinterest.shared.Utils;

/**
 * Adds a row to the app's settings that opens {@link SettingsActivity}.
 */
@SuppressWarnings("unused")
public final class SettingsMenuPatch {
    private static final String TITLE_RESOURCE = "archivesteak_patches_settings_title";

    /**
     * Rows created by {@link #addSettingsRow(List)}. A new one is made every time the list is built.
     */
    private static final Set<Object> patchRows = Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * The row each settings row view currently shows.
     */
    private static final Map<View, Object> boundRows = new WeakHashMap<>();

    /**
     * Injection point. Called with the rows of the main settings screen.
     * Adds the patches row first among the rows that open another screen.
     */
    public static void addSettingsRow(List<Object> rows) {
        try {
            Object patchRow = createRow(Utils.getResourceId(TITLE_RESOURCE, "string"), null);
            patchRows.add(patchRow);

            int viewType = viewTypeOf(patchRow);
            int index = rows.size();
            for (int i = 0; i < rows.size(); i++) {
                if (viewTypeOf(rows.get(i)) == viewType) {
                    index = i;
                    break;
                }
            }
            rows.add(index, patchRow);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to add settings row", ex);
        }
    }

    /**
     * Injection point. Called when a settings row view is bound to a row.
     */
    public static void onBindRow(View view, Object row) {
        boundRows.put(view, row);
        if (!patchRows.contains(row)) return;

        // Runs after the view sets its own click listener, which would open
        // the screen of the row the patches row is made from.
        view.post(() -> {
            if (boundRows.get(view) != row) return;

            view.setOnClickListener(v ->
                    v.getContext().startActivity(new Intent(v.getContext(), SettingsActivity.class)));
        });
    }

    private static int viewTypeOf(Object row) throws Exception {
        return (int) row.getClass().getMethod("getViewType").invoke(row);
    }

    /**
     * Creates a settings row that opens another screen. Implemented by the patch,
     * which uses the unused parameter as a register for the row.
     */
    private static Object createRow(int titleResource, Object unused) {
        return null;
    }
}
