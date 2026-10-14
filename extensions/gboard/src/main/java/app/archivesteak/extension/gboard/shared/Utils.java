package app.archivesteak.extension.gboard.shared;

import android.content.Context;
import android.content.SharedPreferences;

@SuppressWarnings("unused")
public final class Utils {
    public static final String TAG = "Quest";

    private static Context context;

    /**
     * Injection point. Called when the app starts, in each of its processes.
     */
    public static void setContext(Context appContext) {
        context = appContext;
    }

    /**
     * @return The app's context, or null before the app has started.
     */
    public static Context getContext() {
        return context;
    }

    /**
     * @return Gboard's own settings, which the patch settings are stored with, or null before the app has started.
     * A keyboard is used before the device is unlocked, so they're in device protected storage.
     */
    public static SharedPreferences getPreferences() {
        if (context == null) return null;
        Context storageContext = context.isDeviceProtectedStorage() ? context : context.createDeviceProtectedStorageContext();
        if (storageContext == null) storageContext = context;
        return storageContext.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }

    public static int getResourceId(String name, String type) {
        return context.getResources().getIdentifier(name, type, context.getPackageName());
    }
}
