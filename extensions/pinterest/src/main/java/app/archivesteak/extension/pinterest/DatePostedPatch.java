package app.archivesteak.extension.pinterest;

import static app.archivesteak.extension.pinterest.shared.Utils.TAG;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

import app.archivesteak.extension.pinterest.settings.Settings;
import app.archivesteak.extension.pinterest.shared.PinModel;
import app.archivesteak.extension.pinterest.shared.Utils;

@SuppressWarnings("unused")
public final class DatePostedPatch {
    private static final String CREATOR_VIEW_CLASS = "com.pinterest.activity.pin.view.PinCloseupCreatorAttributionView";
    private static final String DATE_VIEW_TAG = "archivesteak_date_posted";
    private static final String APP_FONT = "pin_sans_regular";

    /**
     * Injection point. Called when the Pin page's creator module is bound to a Pin.
     *
     * @param module The creator module, which contains the creator view.
     */
    public static void onCreatorModuleUpdated(View module, Object pin) {
        try {
            ViewGroup creatorView = findCreatorView(module);
            if (creatorView == null || creatorView.getChildCount() == 0) return;

            // Holds the avatar and name row, and the text below it.
            ViewGroup content = (ViewGroup) creatorView.getChildAt(0);
            TextView dateView = content.findViewWithTag(DATE_VIEW_TAG);

            Date datePosted = Settings.SHOW_DATE_POSTED.get() ? PinModel.getCreatedAt(pin) : null;
            if (datePosted == null) {
                if (dateView != null) dateView.setVisibility(View.GONE);
                return;
            }

            if (dateView == null) {
                dateView = createDateView(content.getContext());
                content.addView(dateView, 0);
            }
            dateView.setText(DateFormat.getDateInstance(DateFormat.LONG).format(datePosted));
            dateView.setVisibility(View.VISIBLE);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to show date posted", ex);
        }
    }

    private static ViewGroup findCreatorView(View view) {
        if (!(view instanceof ViewGroup)) return null;

        ViewGroup group = (ViewGroup) view;
        if (group.getClass().getName().equals(CREATOR_VIEW_CLASS)) return group;

        for (int i = 0; i < group.getChildCount(); i++) {
            ViewGroup creatorView = findCreatorView(group.getChildAt(i));
            if (creatorView != null) return creatorView;
        }
        return null;
    }

    private static TextView createDateView(Context context) {
        TextView dateView = new TextView(context);
        dateView.setTag(DATE_VIEW_TAG);
        dateView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

        try {
            dateView.setTypeface(context.getResources().getFont(Utils.getResourceId(APP_FONT, "font")));
        } catch (Exception ex) {
            Log.e(TAG, "Failed to load the app's font", ex);
        }

        TypedArray attributes = context.obtainStyledAttributes(new int[]{android.R.attr.textColorSecondary});
        dateView.setTextColor(attributes.getColorStateList(0));
        attributes.recycle();

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.bottomMargin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 4, context.getResources().getDisplayMetrics());
        dateView.setLayoutParams(layoutParams);
        return dateView;
    }
}
