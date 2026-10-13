package app.archivesteak.extension.pinterest;

import android.text.SpannableStringBuilder;
import android.util.Log;

import java.text.DateFormat;
import java.util.Date;

@SuppressWarnings("unused")
public final class DatePostedPatch {
    private static final String TAG = "PinterestPatches";

    /**
     * Injection point. Called with a Pin's description just before it is shown.
     * The collapsed description is cut to one or two lines, so the date shows once expanded.
     *
     * @param closeupModule The Pin closeup module the description belongs to.
     */
    public static void appendDatePosted(CharSequence description, Object closeupModule) {
        try {
            if (!(description instanceof SpannableStringBuilder)) return;

            Date datePosted = getDatePosted(closeupModule);
            if (datePosted == null) return;

            SpannableStringBuilder text = (SpannableStringBuilder) description;
            if (text.length() > 0) text.append("\n\n");
            text.append("Posted ").append(DateFormat.getDateInstance(DateFormat.LONG).format(datePosted));
        } catch (Exception ex) {
            Log.e(TAG, "Failed to add date posted", ex);
        }
    }

    /**
     * Creation date of the module's Pin, or null. Implemented by the patch,
     * because the Pin class and its getters are obfuscated.
     */
    private static Date getDatePosted(Object closeupModule) {
        return null;
    }
}
