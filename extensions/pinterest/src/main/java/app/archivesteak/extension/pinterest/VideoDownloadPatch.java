package app.archivesteak.extension.pinterest;

import android.content.Context;

import app.archivesteak.extension.pinterest.shared.MediaDownloader;

@SuppressWarnings("unused")
public final class VideoDownloadPatch {
    /**
     * Injection point. Called when "Download image" is tapped in the Pin menu.
     *
     * @return Whether a video download was started. If not, the image is downloaded instead.
     */
    public static boolean downloadVideo(Context context, Object pin) {
        return MediaDownloader.downloadVideo(context, pin);
    }
}
