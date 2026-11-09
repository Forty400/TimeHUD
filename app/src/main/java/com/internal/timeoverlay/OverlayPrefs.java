package com.internal.timeoverlay;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Local settings for the overlay. Defaults match v0.1.4:
 * overlay off, date+millis, standard 280x36, transparency 10%, white, unlocked.
 */
public final class OverlayPrefs {

    public static final String PREFS_NAME = "time_overlay_prefs";

    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_SHOW_DATE = "show_date";
    public static final String KEY_SHOW_MILLIS = "show_millis";
    public static final String KEY_SIZE_MODE = "size_mode";
    public static final String KEY_CUSTOM_WIDTH = "custom_width";
    public static final String KEY_CUSTOM_HEIGHT = "custom_height";
    public static final String KEY_TRANSPARENCY = "transparency";
    public static final String KEY_TEXT_COLOR = "text_color";
    public static final String KEY_LOCKED = "locked";
    public static final String KEY_POS_X = "pos_x";
    public static final String KEY_POS_Y = "pos_y";
    public static final String KEY_HAS_POS = "has_pos";

    public static final int SIZE_COMPACT = SizeModes.COMPACT;
    public static final int SIZE_STANDARD = SizeModes.STANDARD;
    public static final int SIZE_LARGE = SizeModes.LARGE;
    public static final int SIZE_CUSTOM = SizeModes.CUSTOM;

    private final SharedPreferences prefs;

    public OverlayPrefs(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        return prefs.getBoolean(KEY_ENABLED, false);
    }

    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public boolean showDate() {
        return prefs.getBoolean(KEY_SHOW_DATE, true);
    }

    public void setShowDate(boolean showDate) {
        prefs.edit().putBoolean(KEY_SHOW_DATE, showDate).apply();
    }

    public boolean showMillis() {
        return prefs.getBoolean(KEY_SHOW_MILLIS, true);
    }

    public void setShowMillis(boolean showMillis) {
        prefs.edit().putBoolean(KEY_SHOW_MILLIS, showMillis).apply();
    }

    public int getSizeMode() {
        return prefs.getInt(KEY_SIZE_MODE, SIZE_STANDARD);
    }

    public void setSizeMode(int mode) {
        prefs.edit().putInt(KEY_SIZE_MODE, mode).apply();
    }

    public int getCustomWidth() {
        return prefs.getInt(KEY_CUSTOM_WIDTH, 280);
    }

    public int getCustomHeight() {
        return prefs.getInt(KEY_CUSTOM_HEIGHT, 36);
    }

    public void setCustomSize(int widthDp, int heightDp) {
        prefs.edit()
                .putInt(KEY_CUSTOM_WIDTH, widthDp)
                .putInt(KEY_CUSTOM_HEIGHT, heightDp)
                .putInt(KEY_SIZE_MODE, SIZE_CUSTOM)
                .apply();
    }

    /** 0–100: 0 opaque, 100 fully transparent. */
    public int getTransparency() {
        return prefs.getInt(KEY_TRANSPARENCY, 10);
    }

    public void setTransparency(int transparency) {
        prefs.edit().putInt(KEY_TRANSPARENCY, clamp(transparency, 0, 100)).apply();
    }

    public int getTextColor() {
        return prefs.getInt(KEY_TEXT_COLOR, TextColors.WHITE);
    }

    public void setTextColor(int color) {
        prefs.edit().putInt(KEY_TEXT_COLOR, color).apply();
    }

    public boolean isLocked() {
        return prefs.getBoolean(KEY_LOCKED, false);
    }

    public void setLocked(boolean locked) {
        prefs.edit().putBoolean(KEY_LOCKED, locked).apply();
    }

    public boolean hasSavedPosition() {
        return prefs.getBoolean(KEY_HAS_POS, false);
    }

    public int getPosX() {
        return prefs.getInt(KEY_POS_X, 0);
    }

    public int getPosY() {
        return prefs.getInt(KEY_POS_Y, 0);
    }

    public void setPosition(int x, int y) {
        prefs.edit()
                .putInt(KEY_POS_X, x)
                .putInt(KEY_POS_Y, y)
                .putBoolean(KEY_HAS_POS, true)
                .apply();
    }

    public SharedPreferences.OnSharedPreferenceChangeListener register(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.registerOnSharedPreferenceChangeListener(listener);
        return listener;
    }

    public void unregister(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
