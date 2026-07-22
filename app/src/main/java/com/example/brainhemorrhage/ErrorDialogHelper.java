package com.example.brainhemorrhage;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ErrorDialogHelper {
    public static void show(Context context, String title, String message) {
        if (context == null) return;
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_error_notification, null);
        TextView titleView = view.findViewById(R.id.errorTitle);
        TextView messageView = view.findViewById(R.id.errorMessage);
        Button dismissButton = view.findViewById(R.id.dismissButton);
        android.widget.ImageView errorIcon = view.findViewById(R.id.errorIcon);

        if (title != null && !title.isEmpty()) {
            titleView.setText(title);
        }
        if (message != null && !message.isEmpty()) {
            messageView.setText(message);
        }

        AlertDialog dialog = new AlertDialog.Builder(context, R.style.OtpDialogTheme)
                .setView(view)
                .create();

        // Apply bounce press scaling to dismiss button
        AnimationHelper.applyBouncePress(dismissButton);

        dismissButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        // Make window transparent so rounded borders show correctly
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // Apply custom dialog entry animation
        AnimationHelper.applyDialogEntrance(view);

        // Shake icon and then start continuous pulsing
        AnimationHelper.applyShake(errorIcon);
    }
}
