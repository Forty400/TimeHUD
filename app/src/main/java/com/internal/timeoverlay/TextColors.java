package com.internal.timeoverlay;

import android.graphics.Color;

public final class TextColors {

    public static final int WHITE = Color.parseColor("#FFFFFF");
    public static final int BLACK = Color.parseColor("#000000");
    public static final int RED = Color.parseColor("#FF5252");
    public static final int YELLOW = Color.parseColor("#FFD54F");
    public static final int GREEN = Color.parseColor("#69F0AE");
    public static final int BLUE = Color.parseColor("#64B5F6");

    public static final int[] ALL = {
            WHITE, BLACK, RED, YELLOW, GREEN, BLUE
    };

    public static final int[] LABEL_RES_IDS = {
            R.string.color_white,
            R.string.color_black,
            R.string.color_red,
            R.string.color_yellow,
            R.string.color_green,
            R.string.color_blue
    };

    private TextColors() {
    }

    public static int indexOf(int color) {
        for (int i = 0; i < ALL.length; i++) {
            if (ALL[i] == color) {
                return i;
            }
        }
        return 0;
    }
}
