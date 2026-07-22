package com.example.brainhemorrhage;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

public class WelcomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View logoCard         = view.findViewById(R.id.welcomeLogoCard);
        TextView title        = view.findViewById(R.id.welcomeTitle);
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("NuerocheckPrefs", android.content.Context.MODE_PRIVATE);
        String name = prefs.getString("name", "Smith");
        title.setText(getString(R.string.welcome_doctor, name));

        TextView subtitle     = view.findViewById(R.id.welcomeSubtitle);
        LinearLayout featureRow = view.findViewById(R.id.featureCardsRow);
        Button dashBtn        = view.findViewById(R.id.goToDashboardButton);
        TextView skipText     = view.findViewById(R.id.skipText);

        // ── Staggered entrance sequence ───────────────────────────────────────
        animateIn(logoCard, 100, new OvershootInterpolator());
        animateSlideUp(title, 250);
        animateSlideUp(subtitle, 380);
        animateIn(featureRow, 500, new DecelerateInterpolator());
        animateSlideUp(dashBtn, 640);
        animateIn(skipText, 740, new DecelerateInterpolator());

        // ── Navigation handlers ───────────────────────────────────────────────
        dashBtn.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_welcome_to_dashboard));

        skipText.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_welcome_to_dashboard));
    }

    /** Fade + scale in from 0.7 */
    private void animateIn(View v, long delay, android.view.animation.Interpolator interp) {
        if (v == null) return;
        v.setAlpha(0f);
        v.setScaleX(0.75f);
        v.setScaleY(0.75f);

        ObjectAnimator alpha  = ObjectAnimator.ofFloat(v, "alpha",  0f, 1f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 0.75f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 0.75f, 1f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(alpha, scaleX, scaleY);
        set.setDuration(420);
        set.setStartDelay(delay);
        set.setInterpolator(interp);
        set.start();
    }

    /** Fade + slide up from 24dp */
    private void animateSlideUp(View v, long delay) {
        if (v == null) return;
        v.setAlpha(0f);
        v.setTranslationY(24f);

        ObjectAnimator alpha = ObjectAnimator.ofFloat(v, "alpha", 0f, 1f);
        ObjectAnimator transY = ObjectAnimator.ofFloat(v, "translationY", 24f, 0f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(alpha, transY);
        set.setDuration(380);
        set.setStartDelay(delay);
        set.setInterpolator(new DecelerateInterpolator(1.2f));
        set.start();
    }
}
