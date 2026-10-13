package app.archivesteak.extension.pinterest.shared;

import static app.archivesteak.extension.pinterest.shared.Utils.TAG;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

/**
 * Copies a Pin's image to the clipboard. The clipboard holds images as content URIs,
 * so the image is saved to the {@link SharedFiles shared cache}, which other apps can read.
 */
public final class ImageClipboard {
    /**
     * Directory in the shared cache directory that holds the copied image.
     */
    private static final String DIRECTORY = "archivesteak_clipboard";

    public static void copy(Context context, Object pin) {
        Context appContext = context.getApplicationContext();
        String url = MediaDownloader.findImageUrl(PinModel.getImages(pin));
        if (url == null) {
            Toast.makeText(appContext, "Nothing to copy", Toast.LENGTH_SHORT).show();
            return;
        }

        Handler mainThread = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                Uri uri = SharedFiles.download(appContext, url, DIRECTORY);
                mainThread.post(() -> setClip(appContext, uri));
            } catch (Exception ex) {
                Log.e(TAG, "Failed to copy image", ex);
                mainThread.post(() -> Toast.makeText(appContext, "Failed to copy image", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private static void setClip(Context context, Uri uri) {
        context.getSystemService(ClipboardManager.class)
                .setPrimaryClip(ClipData.newUri(context.getContentResolver(), "Image", uri));

        // Android 13 and later confirm copies themselves.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            int copied = Utils.getResourceId("copied", "string");
            Toast.makeText(context, copied == 0 ? "Copied" : context.getString(copied), Toast.LENGTH_SHORT).show();
        }
    }
}
