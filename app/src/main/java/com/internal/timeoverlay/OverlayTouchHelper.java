package com.internal.timeoverlay;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * Distinguishes drag vs tap; ignores multi-touch and cancelled gestures for menu.
 */
public final class OverlayTouchHelper {

    public interface Callback {
        void onDrag(int dx, int dy);

        void onDragEnd();

        void onTap();
    }

    private final int touchSlop;
    private final Callback callback;

    private float downX;
    private float downY;
    private float lastX;
    private float lastY;
    private boolean dragging;
    private boolean multiTouch;
    private boolean cancelled;

    public OverlayTouchHelper(View view, Callback callback) {
        this.touchSlop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
        this.callback = callback;
    }

    public boolean onTouch(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                multiTouch = false;
                cancelled = false;
                dragging = false;
                downX = event.getRawX();
                downY = event.getRawY();
                lastX = downX;
                lastY = downY;
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                multiTouch = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (multiTouch || cancelled) {
                    return true;
                }
                float x = event.getRawX();
                float y = event.getRawY();
                if (!dragging) {
                    float totalDx = x - downX;
                    float totalDy = y - downY;
                    if (Math.hypot(totalDx, totalDy) > touchSlop) {
                        dragging = true;
                    }
                }
                if (dragging) {
                    int dx = Math.round(x - lastX);
                    int dy = Math.round(y - lastY);
                    lastX = x;
                    lastY = y;
                    if (dx != 0 || dy != 0) {
                        callback.onDrag(dx, dy);
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!multiTouch && !cancelled && !dragging) {
                    callback.onTap();
                }
                if (dragging) {
                    callback.onDragEnd();
                }
                dragging = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                cancelled = true;
                if (dragging) {
                    callback.onDragEnd();
                }
                dragging = false;
                return true;
            default:
                return false;
        }
    }
}
