package com.internal.timeoverlay;

/**
 * Effective touch-through policy. Android 12 (API 31)+ applies a system
 * obscuring-opacity ceiling for interactive overlays; this is independent of the
 * background transparency slider UX copy.
 */
public final class PassthroughPolicy {

    /** Mirrors InputManager.MAXIMUM_OBSCURING_OPACITY_FOR_TOUCH. */
    public static final float SYSTEM_TOUCH_OPACITY_CEILING = 0.8f;
    public static final int ANDROID_12_SDK = 31;

    private PassthroughPolicy() {
    }

    public static float backgroundObscuringOpacity(int transparencyPercent) {
        int t = Math.max(0, Math.min(100, transparencyPercent));
        int alpha = Math.round(255f * (100 - t) / 100f);
        return alpha / 255f;
    }

    public static boolean isEffectivePassthrough(boolean locked, int transparencyPercent, int sdkInt) {
        if (locked) {
            return true;
        }
        if (sdkInt < ANDROID_12_SDK) {
            return false;
        }
        return backgroundObscuringOpacity(transparencyPercent) < SYSTEM_TOUCH_OPACITY_CEILING;
    }
}
