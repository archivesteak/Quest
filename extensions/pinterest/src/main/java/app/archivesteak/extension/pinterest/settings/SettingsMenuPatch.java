package app.archivesteak.extension.pinterest.settings;

import static app.archivesteak.extension.pinterest.shared.Utils.TAG;

import android.util.Log;
import android.view.View;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import app.archivesteak.extension.pinterest.shared.Utils;

/**
 * Adds a row to the app's settings that opens the patch settings, see {@link SettingsScreenPatch}.
 */
@SuppressWarnings("unused")
public final class SettingsMenuPatch {
    /**
     * Id of the Social permissions row, whose screen shows the patch settings when the patches row opens it.
     */
    private static final String SCREEN_ROW_ID = "settings_social_permission";

    /**
     * Rows created by {@link #addSettingsRow(List)}. A new one is made every time the list is built.
     */
    private static final Set<Object> patchRows = Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * The row each settings row view currently shows.
     */
    private static final Map<View, Object> boundRows = new WeakHashMap<>();

    /**
     * The Social permissions row of the main settings screen.
     */
    private static WeakReference<Object> screenRow = new WeakReference<>(null);

    /**
     * Injection point. Called with the rows of the main settings screen.
     * Adds the patches row first among the rows that open another screen.
     */
    public static void addSettingsRow(List<Object> rows) {
        try {
            screenRow = new WeakReference<>(findRow(rows, Utils.getResourceId(SCREEN_ROW_ID, "id")));

            Object patchRow = createRow(Settings.getStringResource("title"), null);
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

            view.setOnClickListener(SettingsMenuPatch::openSettings);
        });
    }

    /**
     * Opens the Social permissions screen the way tapping its row does,
     * and has it show the patch settings.
     */
    private static void openSettings(View rowView) {
        try {
            Object row = screenRow.get();
            if (row == null) throw new IllegalStateException("Social permissions row not found");

            Class<?> function = Class.forName("kotlin.jvm.functions.Function1");
            Object onRowClick = findFieldValue(rowView, function);
            if (onRowClick == null) throw new IllegalStateException("Row click handler not found");

            SettingsScreenPatch.onOpen();
            function.getMethod("invoke", Object.class).invoke(onRowClick, row);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to open settings", ex);
        }
    }

    private static Object findRow(List<Object> rows, int id) {
        for (Object row : rows) {
            try {
                if ((int) row.getClass().getMethod("getId").invoke(row) == id) return row;
            } catch (ReflectiveOperationException ignored) {
                // Rows without an id.
            }
        }
        return null;
    }

    /**
     * @return The value of the first field of the object that is an instance of the type, or null.
     */
    private static Object findFieldValue(Object object, Class<?> type) throws IllegalAccessException {
        for (Field field : object.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;

            field.setAccessible(true);
            Object value = field.get(object);
            if (type.isInstance(value)) return value;
        }
        return null;
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
