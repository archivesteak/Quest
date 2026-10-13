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
import app.archivesteak.extension.pinterest.shared.ImageClipboard;
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
        if (pin == null) return items;

        List<Object> withPatchItems = new ArrayList<>(items);
        if (Settings.LONG_PRESS_DOWNLOAD.get()) {
            addMenuItem(withPatchItems, menu, "download", getDownloadIcon(),
                    v -> MediaDownloader.download(v.getContext(), pin));
        }
        if (Settings.LONG_PRESS_COPY_IMAGE.get()) {
            addMenuItem(withPatchItems, menu, "copy", getCopyIcon(),
                    v -> ImageClipboard.copy(v.getContext(), pin));
        }
        return withPatchItems;
    }

    /**
     * @param label Name of the app's string to label the item with.
     */
    private static void addMenuItem(List<Object> items, View menu, String label, Object icon,
                                    View.OnClickListener onClick) {
        try {
            View item = createMenuItem(menu.getContext(), Utils.getResourceId(label, "string"), icon, null, null);
            // Replaces the item's own listener, which would run the null action passed above.
            item.setOnClickListener(onClick);
            items.add(item);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to add long-press menu item " + label, ex);
        }
    }

    /**
     * The Pin the menu builder holds. Implemented by the patch.
     */
    private static Object getMenuPin(Object menuBuilder) {
        return null;
    }

    /**
     * Creates a menu item view. Implemented by the patch,
     * which uses the last two parameters as registers for the null action and the item.
     */
    private static View createMenuItem(Context context, int label, Object icon, Object action, Object item) {
        return null;
    }

    /**
     * Implemented by the patch.
     */
    private static Object getDownloadIcon() {
        return null;
    }

    /**
     * Implemented by the patch.
     */
    private static Object getCopyIcon() {
        return null;
    }
}
