package app.archivesteak.extension.pinterest.shared;

import static app.archivesteak.extension.pinterest.shared.Utils.TAG;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Files in the app's cache that other apps can be given access to with the app's file provider.
 */
public final class SharedFiles {
    private static final String FILE_PROVIDER = "androidx.core.content.FileProvider";
    private static final String FILE_PROVIDER_PATHS = "android.support.FILE_PROVIDER_PATHS";

    private static final int TIMEOUT_MILLIS = 15_000;

    /**
     * Downloads a file into a directory of the shared cache, replacing the directory's previous files.
     * Must not be called on the main thread.
     *
     * @return The file's content URI.
     */
    public static Uri download(Context context, String url, String directoryName) throws Exception {
        SharedCache cache = findSharedCache(context);
        return cache.uriOf(download(url, new File(cache.directory, directoryName)));
    }

    /**
     * Downloads a file into the directory, replacing its previous files.
     */
    private static File download(String url, File directory) throws IOException {
        File[] oldFiles = directory.listFiles();
        if (oldFiles != null) {
            for (File oldFile : oldFiles) {
                if (!oldFile.delete()) Log.w(TAG, "Failed to delete " + oldFile);
            }
        }
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Failed to create " + directory);

        File file = new File(directory, Uri.parse(url).getLastPathSegment());
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        try (InputStream input = connection.getInputStream(); OutputStream output = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        } finally {
            connection.disconnect();
        }
        return file;
    }

    /**
     * The cache directory the app's file provider shares, read from the provider's paths.
     */
    private static SharedCache findSharedCache(Context context) throws Exception {
        PackageManager packageManager = context.getPackageManager();
        ProviderInfo[] providers = packageManager.getPackageInfo(context.getPackageName(),
                PackageManager.GET_PROVIDERS | PackageManager.GET_META_DATA).providers;
        if (providers == null) throw new IllegalStateException("No providers");

        for (ProviderInfo provider : providers) {
            if (!FILE_PROVIDER.equals(provider.name)) continue;

            try (XmlResourceParser paths = provider.loadXmlMetaData(packageManager, FILE_PROVIDER_PATHS)) {
                if (paths == null) continue;

                while (paths.next() != XmlPullParser.END_DOCUMENT) {
                    if (paths.getEventType() != XmlPullParser.START_TAG || !paths.getName().equals("cache-path")) {
                        continue;
                    }

                    String path = paths.getAttributeValue(null, "path");
                    File directory = new File(context.getCacheDir(), path == null ? "" : path).getCanonicalFile();
                    // Provider authorities can be a list.
                    String authority = provider.authority.split(";")[0];
                    return new SharedCache(authority, paths.getAttributeValue(null, "name"), directory);
                }
            }
        }
        throw new IllegalStateException("No file provider shares the cache");
    }

    private static final class SharedCache {
        final String authority;
        final String name;
        final File directory;

        SharedCache(String authority, String name, File directory) {
            this.authority = authority;
            this.name = name;
            this.directory = directory;
        }

        /**
         * The URI the file provider has for a file in this directory.
         */
        Uri uriOf(File file) throws IOException {
            String relativePath = file.getCanonicalPath().substring(directory.getPath().length() + 1);
            return new Uri.Builder()
                    .scheme("content")
                    .authority(authority)
                    .encodedPath(Uri.encode(name) + "/" + Uri.encode(relativePath, "/"))
                    .build();
        }
    }
}
