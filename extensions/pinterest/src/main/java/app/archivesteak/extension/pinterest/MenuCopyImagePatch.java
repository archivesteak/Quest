package app.archivesteak.extension.pinterest;

import static app.archivesteak.extension.pinterest.shared.Utils.TAG;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import app.archivesteak.extension.pinterest.shared.Utils;

/**
 * Adds "Copy image" below "Download image" in the Pin menu.
 */
@SuppressWarnings("unused")
public final class MenuCopyImagePatch {
    private static final String TITLE_RESOURCE = "archivesteak_copy_image";

    /**
     * Injection point. Called when the menu of a Pin adds its download row.
     * The copy row taps the download row, whose handler then copies the image instead.
     */
    public static void onDownloadRowAdded(View downloadRow) {
        try {
            ViewGroup menu = (ViewGroup) downloadRow.getParent();
            View copyRow = createRow(menu, Utils.getResourceId(TITLE_RESOURCE, "string"), getCopyIcon(), 0, null);
            copyRow.setOnClickListener(v -> {
                MenuDownloadPatch.requestCopy();
                downloadRow.performClick();
            });
            menu.addView(copyRow, menu.indexOfChild(downloadRow) + 1);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to add copy image row", ex);
        }
    }

    /**
     * Makes a menu row with the menu's row factory. Implemented by the patch,
     * which uses the unused parameter as a register.
     *
     * @param defaults Which parameters the factory replaces with defaults. None are.
     */
    private static RelativeLayout createRow(Object menu, int title, Object icon, int defaults, Object unused) {
        return null;
    }

    /**
     * Implemented by the patch.
     */
    private static Object getCopyIcon() {
        return null;
    }
}
