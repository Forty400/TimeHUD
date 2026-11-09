package com.internal.timeoverlay;

/**
 * Overlay size table in dp (v0.1.4). Heights for presets are always 28 / 36 / 44.
 */
public final class SizeCalculator {

    public static final int MAX_WIDTH = 600;
    public static final int MAX_HEIGHT = 160;

    public static final class Size {
        public final int widthDp;
        public final int heightDp;

        public Size(int widthDp, int heightDp) {
            this.widthDp = widthDp;
            this.heightDp = heightDp;
        }
    }

    private SizeCalculator() {
    }

    public static Size presetSize(int sizeMode, boolean showDate, boolean showMillis) {
        int index;
        if (sizeMode == SizeModes.COMPACT) {
            index = 0;
        } else if (sizeMode == SizeModes.LARGE) {
            index = 2;
        } else {
            index = 1; // standard
        }
        int[] widths = widthsFor(showDate, showMillis);
        int[] heights = {28, 36, 44};
        return new Size(widths[index], heights[index]);
    }

    public static Size minCustomSize(boolean showDate, boolean showMillis) {
        if (showDate && showMillis) {
            return new Size(200, 28);
        }
        if (showDate) {
            return new Size(168, 28);
        }
        if (showMillis) {
            return new Size(112, 24);
        }
        return new Size(80, 24);
    }

    public static Size resolve(
            int sizeMode,
            boolean showDate,
            boolean showMillis,
            int customWidthDp,
            int customHeightDp) {
        if (sizeMode != SizeModes.CUSTOM) {
            return presetSize(sizeMode, showDate, showMillis);
        }
        Size min = minCustomSize(showDate, showMillis);
        int w = Math.max(customWidthDp, min.widthDp);
        int h = Math.max(customHeightDp, min.heightDp);
        w = Math.min(w, MAX_WIDTH);
        h = Math.min(h, MAX_HEIGHT);
        return new Size(w, h);
    }

    public static String validateCustom(int width, int height, boolean showDate, boolean showMillis) {
        Size min = minCustomSize(showDate, showMillis);
        if (width < min.widthDp || height < min.heightDp) {
            return "min";
        }
        if (width > MAX_WIDTH || height > MAX_HEIGHT) {
            return "max";
        }
        return null;
    }

    private static int[] widthsFor(boolean showDate, boolean showMillis) {
        if (showDate && showMillis) {
            return new int[]{240, 280, 320};
        }
        if (showDate) {
            return new int[]{208, 240, 272};
        }
        if (showMillis) {
            return new int[]{144, 168, 192};
        }
        return new int[]{96, 112, 128};
    }
}
