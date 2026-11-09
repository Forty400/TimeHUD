package com.internal.timeoverlay;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public final class DpUtils {

    private DpUtils() {
    }

    public static int dp(Context context, float dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()));
    }

    public static int sp(Context context, float sp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics()));
    }

    public static DisplayMetrics metrics(Context context) {
        return context.getResources().getDisplayMetrics();
    }
}
