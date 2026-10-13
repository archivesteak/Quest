package app.archivesteak.extension.pinterest.shared;

import static app.archivesteak.extension.pinterest.shared.Utils.TAG;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.util.Map;

/**
 * Saves a Pin's image or video to the device with Android's download manager.
 */
public final class MediaDownloader {
    /**
     * Progressive MP4 encodings in the Pin's video list, best first.
     * HLS and DASH streams are split into segments and can't be saved as one file.
     */
    private static final String[] MP4_VARIANTS = {
            "V_HEVC_MP4_T5_V2",
            "V_HEVC_MP4_T4_V2",
            "V_HEVC_MP4_T3_V2",
            "V_HEVC_MP4_T2_V2",
            "V_HEVC_MP4_T1_V2",
            "V_AV1_MP4_T5",
            "V_AV1_MP4_T4",
            "V_AV1_MP4_T3",
            "V_AV1_MP4_T2",
            "V_AV1_MP4_T1",
            "V_AV1_MP4",
    };

    /**
     * Image sizes in the Pin's image list, largest first.
     */
    private static final String[] IMAGE_SIZES = {"orig", "1200x", "736x", "564x", "474x", "236x"};

    /**
     * Saves the Pin's video, or its image if it has no downloadable video.
     */
    public static void download(Context context, Object pin) {
        if (downloadVideo(context, pin) || downloadImage(context, pin)) return;

        Toast.makeText(context, "Nothing to download", Toast.LENGTH_SHORT).show();
    }

    /**
     * @return Whether the Pin has an MP4 video and its download was started.
     */
    public static boolean downloadVideo(Context context, Object pin) {
        try {
            String url = findVideoUrl(PinModel.getVideoList(pin));
            if (url == null) return false;

            enqueue(context, url, "video/mp4", Environment.DIRECTORY_MOVIES);
            Toast.makeText(context, "Downloading video", Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception ex) {
            Log.e(TAG, "Video download failed", ex);
            return false;
        }
    }

    /**
     * @return Whether the Pin has an image and its download was started.
     */
    public static boolean downloadImage(Context context, Object pin) {
        try {
            String url = findImageUrl(PinModel.getImages(pin));
            if (url == null) return false;

            String extension = MimeTypeMap.getFileExtensionFromUrl(url);
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            enqueue(context, url, mimeType == null ? "image/jpeg" : mimeType, Environment.DIRECTORY_PICTURES);
            Toast.makeText(context, "Downloading image", Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception ex) {
            Log.e(TAG, "Image download failed", ex);
            return false;
        }
    }

    private static void enqueue(Context context, String url, String mimeType, String directory) {
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri)
                .setMimeType(mimeType)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(directory, "Pinterest/" + uri.getLastPathSegment());
        context.getSystemService(DownloadManager.class).enqueue(request);
    }

    private static String findVideoUrl(Map<String, ?> videoList) {
        if (videoList == null || videoList.isEmpty()) return null;

        for (String variant : MP4_VARIANTS) {
            String url = videoUrlOf(videoList.get(variant));
            if (url != null) return url;
        }

        // Encodings this list doesn't know about yet.
        for (Object encoding : videoList.values()) {
            String url = videoUrlOf(encoding);
            String path = url == null ? null : Uri.parse(url).getPath();
            if (path != null && path.endsWith(".mp4")) return url;
        }

        return null;
    }

    private static String videoUrlOf(Object encoding) {
        if (encoding == null) return null;

        String url = PinModel.getVideoUrl(encoding);
        return url == null || url.isEmpty() ? null : url;
    }

    private static String findImageUrl(Map<String, ?> images) {
        if (images == null || images.isEmpty()) return null;

        for (String size : IMAGE_SIZES) {
            Object image = images.get(size);
            String url = image == null ? null : PinModel.getImageUrl(image);
            if (url != null && !url.isEmpty()) return url;
        }

        for (Object image : images.values()) {
            String url = image == null ? null : PinModel.getImageUrl(image);
            if (url != null && !url.isEmpty()) return url;
        }

        return null;
    }
}
