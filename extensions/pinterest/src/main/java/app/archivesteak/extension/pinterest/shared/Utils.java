package app.archivesteak.extension.pinterest.shared;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import java.lang.ref.WeakReference;

@SuppressWarnings("unused")
public final class Utils {
    public static final String TAG = "PinterestPatches";

    private static Context context;

    private static WeakReference<Activity> resumedActivity = new WeakReference<>(null);

    /**
     * Injection point. Called when the app starts.
     */
    public static void setContext(Context appContext) {
        context = appContext;
        if (appContext instanceof Application) {
            ((Application) appContext).registerActivityLifecycleCallbacks(new ActivityTracker());
        }
    }

    public static Context getContext() {
        return context;
    }

    /**
     * @return The activity in the foreground, or null.
     */
    public static Activity getResumedActivity() {
        return resumedActivity.get();
    }

    /**
     * Id of an app resource, or 0 if it doesn't exist.
     */
    public static int getResourceId(String name, String type) {
        return context.getResources().getIdentifier(name, type, context.getPackageName());
    }

    private static final class ActivityTracker implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityResumed(Activity activity) {
            resumedActivity = new WeakReference<>(activity);
        }

        @Override
        public void onActivityPaused(Activity activity) {
            if (resumedActivity.get() == activity) resumedActivity = new WeakReference<>(null);
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }
    }
}
