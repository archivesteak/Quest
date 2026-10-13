package app.archivesteak.extension.pinterest.shared;

import android.content.Context;

@SuppressWarnings("unused")
public final class Utils {
    public static final String TAG = "PinterestPatches";

    private static Context context;

    /**
     * Injection point. Called when the app starts.
     */
    public static void setContext(Context appContext) {
        context = appContext;
    }

    public static Context getContext() {
        return context;
    }

    /**
     * Id of an app resource, or 0 if it doesn't exist.
     */
    public static int getResourceId(String name, String type) {
        return context.getResources().getIdentifier(name, type, context.getPackageName());
    }
}
