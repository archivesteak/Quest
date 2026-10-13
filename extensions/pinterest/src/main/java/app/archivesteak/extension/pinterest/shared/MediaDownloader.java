package app.archivesteak.extension.pinterest.shared;

import static app.archivesteak.extension.pinterest.shared.Utils.TAG;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            "V_720P",
    };

    /**
     * An HLS stream, such as https://v1.pinimg.com/videos/mc/hls/94/e0/e2/[hash].m3u8.
     * The same video is usually also at https://v1.pinimg.com/videos/mc/720p/94/e0/e2/[hash].mp4.
     */
    private static final Pattern HLS_URL = Pattern.compile("^(https://[^?#]+)/hls/([^?#]+)\\.m3u8([?#].*)?$");

    private static final int CHECK_TIMEOUT_MILLIS = 10_000;

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
     * @return Whether the Pin has a video and its download was started. If the video might only be a
     * stream, it's checked first, and the image is saved instead if the video can't be.
     */
    public static boolean downloadVideo(Context context, Object pin) {
        try {
            List<Map<String, ?>> videoLists = findVideoLists(pin);
            for (Map<String, ?> videoList : videoLists) {
                String url = findMp4Url(videoList);
                if (url != null) {
                    enqueueVideo(context, url);
                    return true;
                }
            }

            // Pins in feeds often only have streams.
            for (Map<String, ?> videoList : videoLists) {
                String url = findStreamMp4Url(videoList);
                if (url != null) {
                    downloadVideoIfAvailable(context.getApplicationContext(), pin, url);
                    return true;
                }
            }

            return false;
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

    private static void enqueueVideo(Context context, String url) {
        enqueue(context, url, "video/mp4", Environment.DIRECTORY_MOVIES);
        Toast.makeText(context, "Downloading video", Toast.LENGTH_SHORT).show();
    }

    private static void enqueue(Context context, String url, String mimeType, String directory) {
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri)
                .setMimeType(mimeType)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(directory, "Pinterest/" + uri.getLastPathSegment());
        context.getSystemService(DownloadManager.class).enqueue(request);
    }

    /**
     * Saves the video at the url if it exists, or the Pin's image otherwise.
     */
    private static void downloadVideoIfAvailable(Context context, Object pin, String url) {
        Handler mainThread = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            boolean available = exists(url);
            mainThread.post(() -> {
                if (available) {
                    enqueueVideo(context, url);
                } else if (!downloadImage(context, pin)) {
                    Toast.makeText(context, "Nothing to download", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private static boolean exists(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(CHECK_TIMEOUT_MILLIS);
            connection.setReadTimeout(CHECK_TIMEOUT_MILLIS);
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (IOException ex) {
            Log.e(TAG, "Failed to check video " + url, ex);
            return false;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    /**
     * Video encodings of the Pin's video, and of the videos of its pages if it's an Idea Pin.
     * Pins in feeds can have only a preview of the pages.
     */
    private static List<Map<String, ?>> findVideoLists(Object pin) {
        List<Map<String, ?>> videoLists = new ArrayList<>();
        addIfPresent(videoLists, PinModel.getVideoList(pin));

        for (List<?> pages : Arrays.asList(PinModel.getStoryPages(pin), PinModel.getStoryPagesPreview(pin))) {
            if (pages == null) continue;

            for (Object page : pages) {
                if (page == null) continue;

                List<?> blocks = PinModel.getPageBlocks(page);
                if (blocks != null) {
                    for (Object block : blocks) {
                        if (block != null) addIfPresent(videoLists, PinModel.getBlockVideoList(block));
                    }
                }
                addIfPresent(videoLists, PinModel.getPageVideoList(page));
            }
        }
        return videoLists;
    }

    private static void addIfPresent(List<Map<String, ?>> videoLists, Map<String, ?> videoList) {
        if (videoList != null && !videoList.isEmpty()) videoLists.add(videoList);
    }

    private static String findMp4Url(Map<String, ?> videoList) {
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

    /**
     * @return Where the MP4 version of an HLS stream in the list usually is, or null.
     */
    private static String findStreamMp4Url(Map<String, ?> videoList) {
        for (Object encoding : videoList.values()) {
            String url = videoUrlOf(encoding);
            Matcher matcher = url == null ? null : HLS_URL.matcher(url);
            if (matcher != null && matcher.matches()) {
                return matcher.group(1) + "/720p/" + matcher.group(2) + ".mp4";
            }
        }
        return null;
    }

    private static String videoUrlOf(Object encoding) {
        if (encoding == null) return null;

        String url = PinModel.getVideoUrl(encoding);
        return url == null || url.isEmpty() ? null : url;
    }

    /**
     * @return Url of the largest of the images, or null.
     */
    public static String findImageUrl(Map<String, ?> images) {
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
