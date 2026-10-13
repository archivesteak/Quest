package app.archivesteak.extension.pinterest;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.util.Map;

@SuppressWarnings("unused")
public final class VideoDownloadPatch {
    private static final String TAG = "PinterestPatches";

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
     * Injection point. Called when "Download image" is tapped in the Pin menu.
     *
     * @param videoList Pin video encodings, keyed by encoding name.
     * @param urlGetter Name of the obfuscated URL getter of the encoding class.
     * @return Whether a video download was started. If not, the image is downloaded instead.
     */
    public static boolean downloadVideo(Context context, Map<String, ?> videoList, String urlGetter) {
        try {
            String url = findMp4Url(videoList, urlGetter);
            if (url == null) return false;

            Uri uri = Uri.parse(url);
            DownloadManager.Request request = new DownloadManager.Request(uri)
                    .setMimeType("video/mp4")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES,
                            "Pinterest/" + uri.getLastPathSegment());
            context.getSystemService(DownloadManager.class).enqueue(request);

            Toast.makeText(context, "Downloading video", Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception ex) {
            Log.e(TAG, "Video download failed", ex);
            return false;
        }
    }

    private static String findMp4Url(Map<String, ?> videoList, String urlGetter) throws Exception {
        if (videoList == null || videoList.isEmpty()) return null;

        for (String variant : MP4_VARIANTS) {
            String url = urlOf(videoList.get(variant), urlGetter);
            if (url != null) return url;
        }

        // Encodings this list doesn't know about yet.
        for (Object encoding : videoList.values()) {
            String url = urlOf(encoding, urlGetter);
            String path = url == null ? null : Uri.parse(url).getPath();
            if (path != null && path.endsWith(".mp4")) return url;
        }

        return null;
    }

    private static String urlOf(Object encoding, String urlGetter) throws Exception {
        if (encoding == null) return null;

        String url = (String) encoding.getClass().getMethod(urlGetter).invoke(encoding);
        return url == null || url.isEmpty() ? null : url;
    }
}
