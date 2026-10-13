package app.archivesteak.extension.pinterest;

import static app.archivesteak.extension.pinterest.shared.Utils.TAG;

import android.content.Context;
import android.util.Log;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import app.archivesteak.extension.pinterest.settings.BooleanSetting;
import app.archivesteak.extension.pinterest.settings.Settings;
import app.archivesteak.extension.pinterest.shared.MediaDownloader;
import app.archivesteak.extension.pinterest.shared.Utils;

@SuppressWarnings("unused")
public final class LongPressMenuPatch {
    /**
     * Pin of the long-press menu being built. Other long-press menus, such as the board menu,
     * don't set it, so they don't get a download item.
     */
    private static WeakReference<Object> menuPin = new WeakReference<>(null);

    /**
     * Injection point. Called when a Pin's long-press menu is built.
     *
     * @param menuBuilder Builds the menu, and holds the Pin it's for.
     */
    public static void onBuildPinMenu(Object menuBuilder) {
        menuPin = new WeakReference<>(getMenuPin(menuBuilder));
    }

    /**
     * Injection point. Called with the item types of a Pin's long-press menu,
     * such as REPIN or SEND, before their views are made.
     */
    public static void filterMenuItems(List<?> items) {
        try {
            items.removeIf(item -> {
                if (!(item instanceof Enum)) return false;

                BooleanSetting setting = Settings.find("long_press_" + ((Enum<?>) item).name());
                return setting != null && !setting.get();
            });
        } catch (Exception ex) {
            Log.e(TAG, "Failed to filter long-press menu", ex);
        }
    }

    /**
     * Injection point. Called with the item views before a long-press menu shows them.
     *
     * @return The item views to show.
     */
    public static List<?> addMenuItems(View menu, List<?> items) {
        Object pin = menuPin.get();
        menuPin = new WeakReference<>(null);
        if (pin == null || !Settings.LONG_PRESS_DOWNLOAD.get()) return items;

        try {
            View download = createMenuItem(menu.getContext(), Utils.getResourceId("download", "string"), null, null, null);
            // Replaces the item's own listener, which would run the null action passed above.
            download.setOnClickListener(v -> MediaDownloader.download(v.getContext(), pin));

            List<Object> withDownload = new ArrayList<>(items);
            withDownload.add(download);
            return withDownload;
        } catch (Exception ex) {
            Log.e(TAG, "Failed to add long-press download item", ex);
            return items;
        }
    }

    /**
     * The Pin the menu builder holds. Implemented by the patch.
     */
    private static Object getMenuPin(Object menuBuilder) {
        return null;
    }

    /**
     * Creates a menu item view with a download icon. Implemented by the patch,
     * which uses the last three parameters as registers for the icon, the null action and the item.
     */
    private static View createMenuItem(Context context, int label, Object icon, Object action, Object item) {
        return null;
    }
}
