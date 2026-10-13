package app.archivesteak.extension.pinterest;

import static app.archivesteak.extension.pinterest.shared.Utils.TAG;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import app.archivesteak.extension.pinterest.settings.Settings;
import app.archivesteak.extension.pinterest.shared.MediaDownloader;
import app.archivesteak.extension.pinterest.shared.PinModel;
import app.archivesteak.extension.pinterest.shared.SharedFiles;
import app.archivesteak.extension.pinterest.shared.Utils;

/**
 * Shares a Pin's image with Android's share sheet instead of opening the app's share menu.
 */
@SuppressWarnings("unused")
public final class SharePatch {
    /**
     * Directory in the shared cache that holds the shared image.
     */
    private static final String DIRECTORY = "archivesteak_share";

    /**
     * Injection point. Called when the app opens its share menu for a Pin.
     *
     * @return Whether the image is shared instead.
     */
    public static boolean shareImage(Object pin) {
        try {
            if (!Settings.SHARE_WITH_ANDROID.get()) return false;

            String url = MediaDownloader.findImageUrl(PinModel.getImages(pin));
            if (url == null) return false;

            Context context = Utils.getContext();
            Handler mainThread = new Handler(Looper.getMainLooper());
            new Thread(() -> {
                try {
                    Uri image = SharedFiles.download(context, url, DIRECTORY);
                    mainThread.post(() -> openShareSheet(context, image));
                } catch (Exception ex) {
                    Log.e(TAG, "Failed to share image", ex);
                    mainThread.post(() -> Toast.makeText(context, "Failed to share image", Toast.LENGTH_SHORT).show());
                }
            }).start();
            return true;
        } catch (Exception ex) {
            Log.e(TAG, "Failed to share image", ex);
            return false;
        }
    }

    private static void openShareSheet(Context context, Uri image) {
        String type = context.getContentResolver().getType(image);
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType(type == null ? "image/*" : type)
                .putExtra(Intent.EXTRA_STREAM, image)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        // Lets the share sheet preview the image, and the chosen app read it.
        send.setClipData(ClipData.newRawUri(null, image));

        Intent chooser = Intent.createChooser(send, null);
        Activity activity = Utils.getResumedActivity();
        if (activity != null) {
            activity.startActivity(chooser);
        } else {
            context.startActivity(chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }
}
