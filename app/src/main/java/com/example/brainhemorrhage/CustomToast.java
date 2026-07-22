package com.example.brainhemorrhage;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.snackbar.Snackbar;

public class CustomToast {

    /**
     * Shows a beautiful 3D floating custom notification banner.
     *
     * @param rootView  The anchor root view to display the notification in
     * @param message   The message to display
     * @param isSuccess True for success state (green gradient), false for warning/error state (red gradient)
     */
    public static void show(View rootView, String message, boolean isSuccess) {
        if (rootView == null) return;

        try {
            Snackbar snackbar = Snackbar.make(rootView, "", Snackbar.LENGTH_LONG);
            Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();
            layout.setBackgroundColor(Color.TRANSPARENT);
            layout.setPadding(0, 0, 0, 0);

            Context context = rootView.getContext();
            View customView = LayoutInflater.from(context).inflate(R.layout.custom_toast_layout, null);

            TextView text = customView.findViewById(R.id.toast_text);
            text.setText(message);

            ImageView icon = customView.findViewById(R.id.toast_icon);
            icon.setImageResource(isSuccess ? R.drawable.ic_check_circle : R.drawable.ic_warning);

            View container = customView.findViewById(R.id.toast_container);
            container.setBackgroundResource(isSuccess ? R.drawable.layered_normal_card : R.drawable.layered_abnormal_card);

            layout.addView(customView, 0);

            ViewGroup.LayoutParams params = layout.getLayoutParams();
            if (params instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams fParams = (FrameLayout.LayoutParams) params;
                fParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                fParams.bottomMargin = (int) (64 * context.getResources().getDisplayMetrics().density);
                fParams.leftMargin = (int) (24 * context.getResources().getDisplayMetrics().density);
                fParams.rightMargin = (int) (24 * context.getResources().getDisplayMetrics().density);
                layout.setLayoutParams(fParams);
            }

            customView.setAlpha(0f);
            customView.setTranslationY(80f);
            customView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                    .start();

            snackbar.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Shows a beautiful 3D floating custom notification banner using an Activity context.
     *
     * @param context   The activity context
     * @param message   The message to display
     * @param isSuccess True for success state (green gradient), false for warning/error state (red gradient)
     */
    public static void show(Context context, String message, boolean isSuccess) {
        if (context instanceof Activity) {
            View rootView = ((Activity) context).findViewById(android.R.id.content);
            show(rootView, message, isSuccess);
        }
    }
}
