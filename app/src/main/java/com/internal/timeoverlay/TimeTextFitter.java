package com.internal.timeoverlay;

import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

/** Shared auto-fit so settings preview matches the overlay window. */
public final class TimeTextFitter {

    private TimeTextFitter() {
    }

    public static void fit(TextView view, int widthPx, int heightPx) {
        if (view == null || widthPx <= 0 || heightPx <= 0) {
            return;
        }
        CharSequence text = view.getText();
        if (text == null || text.length() == 0) {
            return;
        }
        int padX = DpUtils.dp(view.getContext(), 8);
        int padY = DpUtils.dp(view.getContext(), 4);
        int availableW = Math.max(1, widthPx - padX);
        int availableH = Math.max(1, heightPx - padY);
        float low = 6f;
        float high = Math.max(availableH, 6f);
        for (int i = 0; i < 14; i++) {
            float mid = (low + high) / 2f;
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, mid);
            view.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            if (view.getMeasuredWidth() <= availableW && view.getMeasuredHeight() <= availableH) {
                low = mid;
            } else {
                high = mid;
            }
        }
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, low);
    }
}
