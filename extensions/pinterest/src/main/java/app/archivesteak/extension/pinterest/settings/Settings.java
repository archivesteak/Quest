package app.archivesteak.extension.pinterest.settings;

import java.util.ArrayList;
import java.util.List;

/**
 * All patch settings. Declaring a setting here adds it to the settings screen,
 * grouped by category in declaration order.
 */
public final class Settings {
    static final String PREFERENCES_NAME = "archivesteak_patches";

    /**
     * Must be declared before the settings, which add themselves to it.
     */
    static final List<BooleanSetting> ALL = new ArrayList<>();

    private static final String PIN_PAGE = "Pin page";
    private static final String LONG_PRESS_MENU = "Long-press menu";

    // region Pin page

    public static final BooleanSetting SHOW_DATE_POSTED = new BooleanSetting("show_date_posted", true,
            PIN_PAGE, "Show date posted", "Shows when a Pin was posted, above its creator.");

    // endregion

    // region Long-press menu

    public static final BooleanSetting LONG_PRESS_DOWNLOAD = new BooleanSetting("long_press_download", true,
            LONG_PRESS_MENU, "Download", "Saves the image, or the video of video Pins.");

    // Keys end with the app's name for the menu item.
    public static final BooleanSetting LONG_PRESS_SAVE = new BooleanSetting("long_press_REPIN", true,
            LONG_PRESS_MENU, "Save", null);
    public static final BooleanSetting LONG_PRESS_SEND = new BooleanSetting("long_press_SEND", true,
            LONG_PRESS_MENU, "Send", null);
    public static final BooleanSetting LONG_PRESS_SEE_MORE = new BooleanSetting("long_press_SEE_MORE", true,
            LONG_PRESS_MENU, "See more like this", null);
    public static final BooleanSetting LONG_PRESS_SEE_LESS = new BooleanSetting("long_press_HIDE", true,
            LONG_PRESS_MENU, "See less", null);
    public static final BooleanSetting LONG_PRESS_VISUAL_SEARCH = new BooleanSetting("long_press_VISUAL_SEARCH", true,
            LONG_PRESS_MENU, "Search image", null);
    public static final BooleanSetting LONG_PRESS_EDIT = new BooleanSetting("long_press_EDIT", true,
            LONG_PRESS_MENU, "Edit", "Only on your own Pins.");
    public static final BooleanSetting LONG_PRESS_REORDER = new BooleanSetting("long_press_REORDER", true,
            LONG_PRESS_MENU, "Reorder", "Only on your boards.");

    // endregion

    /**
     * @return The setting with the given key, or null.
     */
    public static BooleanSetting find(String key) {
        for (BooleanSetting setting : ALL) {
            if (setting.key.equals(key)) return setting;
        }
        return null;
    }
}
