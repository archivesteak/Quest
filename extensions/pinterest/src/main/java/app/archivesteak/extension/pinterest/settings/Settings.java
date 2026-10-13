package app.archivesteak.extension.pinterest.settings;

import java.util.ArrayList;
import java.util.List;

import app.archivesteak.extension.pinterest.shared.Utils;

/**
 * All patch settings. Declaring a setting here adds it to the settings screen,
 * grouped by category in declaration order. Its strings are added by the patch it's for.
 */
public final class Settings {
    static final String PREFERENCES_NAME = "archivesteak_patches";

    /**
     * Prefix of the app strings the settings use, added by the patches.
     */
    private static final String STRING_PREFIX = "archivesteak_settings_";

    /**
     * Must be declared before the settings, which add themselves to it.
     */
    static final List<BooleanSetting> ALL = new ArrayList<>();

    // Categories. Their titles are the strings "category_<category>".
    private static final String PIN_PAGE = "pin_page";
    private static final String LONG_PRESS_MENU = "long_press_menu";
    private static final String SHARING = "sharing";

    // region Pin page

    public static final BooleanSetting SHOW_DATE_POSTED = new BooleanSetting("show_date_posted", true, PIN_PAGE);

    // endregion

    // region Long-press menu

    public static final BooleanSetting LONG_PRESS_DOWNLOAD = new BooleanSetting("long_press_download", true, LONG_PRESS_MENU);
    public static final BooleanSetting LONG_PRESS_COPY_IMAGE = new BooleanSetting("long_press_copy_image", true, LONG_PRESS_MENU);

    // Keys end with the app's name for the menu item.
    public static final BooleanSetting LONG_PRESS_SAVE = new BooleanSetting("long_press_REPIN", true, LONG_PRESS_MENU);
    public static final BooleanSetting LONG_PRESS_SEND = new BooleanSetting("long_press_SEND", true, LONG_PRESS_MENU);
    // The app's item for sending with the app the Pin was last shared with.
    public static final BooleanSetting LONG_PRESS_SHARE_APP = new BooleanSetting("long_press_share_app", true, LONG_PRESS_MENU);
    public static final BooleanSetting LONG_PRESS_SEE_MORE = new BooleanSetting("long_press_SEE_MORE", true, LONG_PRESS_MENU);
    public static final BooleanSetting LONG_PRESS_SEE_LESS = new BooleanSetting("long_press_HIDE", true, LONG_PRESS_MENU);
    public static final BooleanSetting LONG_PRESS_VISUAL_SEARCH = new BooleanSetting("long_press_VISUAL_SEARCH", true, LONG_PRESS_MENU);
    public static final BooleanSetting LONG_PRESS_EDIT = new BooleanSetting("long_press_EDIT", true, LONG_PRESS_MENU);
    public static final BooleanSetting LONG_PRESS_REORDER = new BooleanSetting("long_press_REORDER", true, LONG_PRESS_MENU);

    // endregion

    // region Sharing

    public static final BooleanSetting SHARE_WITH_ANDROID = new BooleanSetting("share_with_android", true, SHARING);

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

    /**
     * @return Resource id of a settings string, or 0 if no applied patch added it.
     */
    static int getStringResource(String name) {
        return Utils.getResourceId(STRING_PREFIX + name, "string");
    }
}
