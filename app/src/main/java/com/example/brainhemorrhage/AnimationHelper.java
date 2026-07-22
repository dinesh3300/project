package com.example.brainhemorrhage;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

public class AnimationHelper {

    /**
     * Applies a tactile press scale animation to make views feel physically responsive.
     */
    public static void applyBouncePress(View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f)
                            .setDuration(100)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f)
                            .setDuration(150)
                            .setInterpolator(new OvershootInterpolator(2.0f))
                            .start();
                    break;
            }
            return false;
        });
    }

    /**
     * Applies a smooth card entrance animation (Fade + Slide Up).
     */
    public static void animateCardEntrance(View view, long delay) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setTranslationY(40f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(delay)
                .setInterpolator(new DecelerateInterpolator(1.2f))
                .start();
    }

    /**
     * Staggers the fade-in and slide-up animation of views to create a cascading entrance.
     */
    public static void animateViewsInSequence(View... views) {
        long delay = 100;
        for (View view : views) {
            animateCardEntrance(view, delay);
            delay += 100;
        }
    }

    /**
     * Triggers a continuous, gentle pulse animation. Perfect for active call-to-actions.
     */
    public static void applyPulse(View view) {
        if (view == null) return;
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 1.03f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 1.03f, 1.0f);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(2200);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.start();
    }

    /**
     * Shakes a view horizontally to signify an alert or error, then breathes gently.
     */
    public static void applyShake(View view) {
        if (view == null) return;
        view.animate()
                .translationXBy(-20f)
                .setDuration(50)
                .withEndAction(() -> view.animate()
                        .translationXBy(40f)
                        .setDuration(50)
                        .withEndAction(() -> view.animate()
                                .translationXBy(-40f)
                                .setDuration(50)
                                .withEndAction(() -> view.animate()
                                        .translationXBy(40f)
                                        .setDuration(50)
                                        .withEndAction(() -> view.animate()
                                                .translationXBy(-20f)
                                                .setDuration(50)
                                                .withEndAction(() -> applyPulse(view))
                                                .start())
                                        .start())
                                .start())
                        .start())
                .start();
    }

    /**
     * Bounces a dialog view scale-in entrance.
     */
    public static void applyDialogEntrance(View view) {
        if (view == null) return;
        view.setScaleX(0.6f);
        view.setScaleY(0.6f);
        view.setAlpha(0f);
        view.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator(1.3f))
                .start();
    }

    /**
     * Applies a continuous, subtle floating animation on the Y-axis to make layout elements feel alive.
     */
    public static void applyFloatingEffect(View view, float deltaY, long duration, long startDelay) {
        if (view == null) return;
        view.setTranslationY(0f);
        ObjectAnimator floatAnim = ObjectAnimator.ofFloat(view, "translationY", 0f, -deltaY, 0f);
        floatAnim.setDuration(duration);
        floatAnim.setRepeatCount(ValueAnimator.INFINITE);
        floatAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        floatAnim.setStartDelay(startDelay);
        floatAnim.start();
    }
}
