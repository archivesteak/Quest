package app.archivesteak.extension.pinterest;

import static app.archivesteak.extension.pinterest.shared.Utils.TAG;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import app.archivesteak.extension.pinterest.shared.ImageClipboard;
import app.archivesteak.extension.pinterest.shared.MediaDownloader;

/**
 * Handles taps on "Download image" in the Pin menu before the app downloads the image.
 */
@SuppressWarnings("unused")
public final class MenuDownloadPatch {
    /**
     * How long after {@link #requestCopy()} a tap on the download row copies the image.
     */
    private static final long COPY_REQUEST_TIMEOUT_MILLIS = 1000;

    private static long copyRequestedAt = -1;

    /**
     * Whether the last tap was handled here, so the app's download only closes the menu.
     */
    private static boolean appDownloadHandled;

    /**
     * Has the next tap on the download row copy the image instead.
     */
    static void requestCopy() {
        copyRequestedAt = SystemClock.uptimeMillis();
    }

    /**
     * Injection point. Called when the download row of a Pin's menu is tapped.
     */
    public static void onDownloadClick(Context context, Object pin) {
        boolean copy = copyRequestedAt >= 0 && SystemClock.uptimeMillis() - copyRequestedAt < COPY_REQUEST_TIMEOUT_MILLIS;
        copyRequestedAt = -1;

        if (copy) {
            ImageClipboard.copy(context, pin);
            appDownloadHandled = true;
        } else {
            appDownloadHandled = isVideoDownloadEnabled() && MediaDownloader.downloadVideo(context, pin);
        }
    }

    /**
     * Injection point. Called when the app starts downloading the image of a Pin's menu.
     *
     * @param onDone Closes the menu.
     * @return Whether the tap was handled already, so the app shouldn't download the image.
     */
    public static boolean onAppDownload(Object onDone) {
        if (!appDownloadHandled) return false;
        appDownloadHandled = false;

        try {
            Class.forName("kotlin.jvm.functions.Function0").getMethod("invoke").invoke(onDone);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to close the Pin menu", ex);
        }
        return true;
    }

    /**
     * Implemented by the video download patch.
     */
    private static boolean isVideoDownloadEnabled() {
        return false;
    }
}
