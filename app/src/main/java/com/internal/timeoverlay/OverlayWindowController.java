package com.internal.timeoverlay;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Choreographer;

/**
 * Manages the overlay window, optional menu, drag/tap, lock passthrough, and time refresh.
 */
public final class OverlayWindowController {

    public static final int BG_COLOR = 0xFF172438;

    private final Context context;
    private final OverlayPrefs prefs;
    private final WindowManager windowManager;
    private final TimeFormatter formatter = new TimeFormatter();

    private FrameLayout root;
    private TextView timeView;
    private GradientDrawable background;
    private WindowManager.LayoutParams params;
    private LinearLayout menuView;
    private WindowManager.LayoutParams menuParams;
    private boolean menuShowing;

    private OverlayTouchHelper touchHelper;
    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (root == null) {
                return;
            }
            updateTimeText();
            if (prefs.showMillis()) {
                Choreographer.getInstance().postFrameCallback(this);
            }
        }
    };
    private final Runnable secondTicker = new Runnable() {
        @Override
        public void run() {
            if (root == null) {
                return;
            }
            updateTimeText();
            if (!prefs.showMillis() && timeView != null) {
                timeView.postDelayed(this, 1000L - (System.currentTimeMillis() % 1000L));
            }
        }
    };

    public OverlayWindowController(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = new OverlayPrefs(this.context);
        this.windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
    }

    public void show() {
        if (root != null) {
            applyAll();
            return;
        }
        createWindow();
        applyAll();
        startClock();
    }

    public void hide() {
        stopClock();
        dismissMenu();
        if (root != null) {
            try {
                windowManager.removeView(root);
            } catch (IllegalArgumentException ignored) {
            }
            root = null;
            timeView = null;
            params = null;
        }
    }

    public boolean isShowing() {
        return root != null;
    }

    public void applyAll() {
        if (root == null) {
            return;
        }
        applyAppearance();
        applySizeAndPosition();
        applyLockFlags();
        updateTimeText();
        fitFont();
        restartClock();
        if (menuShowing) {
            dismissMenu();
        }
    }

    private void createWindow() {
        background = new GradientDrawable();
        background.setColor(withBgAlpha(prefs.getTransparency()));
        background.setCornerRadius(DpUtils.dp(context, 6));

        timeView = new TextView(context);
        timeView.setGravity(Gravity.CENTER);
        timeView.setTypeface(Typeface.MONOSPACE);
        timeView.setMaxLines(1);
        timeView.setSingleLine(true);
        timeView.setIncludeFontPadding(false);
        timeView.setTextColor(prefs.getTextColor());

        root = new FrameLayout(context);
        root.setBackground(background);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        root.addView(timeView, tp);

        int type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;

        SizeCalculator.Size size = SizeCalculator.resolve(
                prefs.getSizeMode(),
                prefs.showDate(),
                prefs.showMillis(),
                prefs.getCustomWidth(),
                prefs.getCustomHeight());
        params.width = DpUtils.dp(context, size.widthDp);
        params.height = DpUtils.dp(context, size.heightDp);

        int[] bounds = usableBounds();
        if (prefs.hasSavedPosition()) {
            params.x = prefs.getPosX();
            params.y = prefs.getPosY();
        } else {
            params.x = Math.max(0, (bounds[2] - params.width) / 2);
            params.y = Math.max(0, bounds[1] + DpUtils.dp(context, 48));
        }
        clampParamsToBounds(bounds);

        touchHelper = new OverlayTouchHelper(root, new OverlayTouchHelper.Callback() {
            @Override
            public void onDrag(int dx, int dy) {
                if (prefs.isLocked()) {
                    return;
                }
                dismissMenu();
                params.x += dx;
                params.y += dy;
                clampParamsToBounds(usableBounds());
                windowManager.updateViewLayout(root, params);
            }

            @Override
            public void onDragEnd() {
                if (prefs.isLocked()) {
                    return;
                }
                prefs.setPosition(params.x, params.y);
            }

            @Override
            public void onTap() {
                if (prefs.isLocked()) {
                    return;
                }
                if (menuShowing) {
                    dismissMenu();
                } else {
                    showMenu();
                }
            }
        });
        root.setOnTouchListener((v, event) -> touchHelper.onTouch(event));

        windowManager.addView(root, params);
    }

    private void applyAppearance() {
        background.setColor(withBgAlpha(prefs.getTransparency()));
        timeView.setTextColor(prefs.getTextColor());
        // Window alpha stays fully opaque; only background drawable alpha changes.
        params.alpha = 1f;
    }

    private void applySizeAndPosition() {
        SizeCalculator.Size size = SizeCalculator.resolve(
                prefs.getSizeMode(),
                prefs.showDate(),
                prefs.showMillis(),
                prefs.getCustomWidth(),
                prefs.getCustomHeight());
        // Expand custom below new min after format change.
        if (prefs.getSizeMode() == OverlayPrefs.SIZE_CUSTOM) {
            SizeCalculator.Size min = SizeCalculator.minCustomSize(prefs.showDate(), prefs.showMillis());
            int w = Math.max(prefs.getCustomWidth(), min.widthDp);
            int h = Math.max(prefs.getCustomHeight(), min.heightDp);
            w = Math.min(w, SizeCalculator.MAX_WIDTH);
            h = Math.min(h, SizeCalculator.MAX_HEIGHT);
            if (w != prefs.getCustomWidth() || h != prefs.getCustomHeight()) {
                prefs.setCustomSize(w, h);
            }
            size = new SizeCalculator.Size(w, h);
        }
        params.width = DpUtils.dp(context, size.widthDp);
        params.height = DpUtils.dp(context, size.heightDp);
        clampParamsToBounds(usableBounds());
        windowManager.updateViewLayout(root, params);
        fitFont();
    }

    private void applyLockFlags() {
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        // Only lock forces NOT_TOUCHABLE. Android 12+ may still pass touches through
        // when obscuring opacity is below the system ceiling; that is system-side.
        if (prefs.isLocked()) {
            flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }
        params.flags = flags;
        windowManager.updateViewLayout(root, params);
        if (prefs.isLocked()) {
            dismissMenu();
        }
    }

    public boolean isEffectivePassthrough() {
        return PassthroughPolicy.isEffectivePassthrough(
                prefs.isLocked(), prefs.getTransparency(), Build.VERSION.SDK_INT);
    }

    private static int withBgAlpha(int transparencyPercent) {
        int alpha = Math.round(255f * (100 - clamp(transparencyPercent, 0, 100)) / 100f);
        return (alpha << 24) | (BG_COLOR & 0x00FFFFFF);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private void updateTimeText() {
        if (timeView == null) {
            return;
        }
        timeView.setText(formatter.format(
                System.currentTimeMillis(), prefs.showDate(), prefs.showMillis()));
    }

    private void fitFont() {
        if (timeView == null || params == null) {
            return;
        }
        CharSequence text = timeView.getText();
        if (text == null || text.length() == 0) {
            return;
        }
        int availableW = params.width - DpUtils.dp(context, 8);
        int availableH = params.height - DpUtils.dp(context, 4);
        float low = 6f;
        float high = Math.max(availableH, 6f);
        for (int i = 0; i < 12; i++) {
            float mid = (low + high) / 2f;
            timeView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, mid);
            timeView.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            if (timeView.getMeasuredWidth() <= availableW
                    && timeView.getMeasuredHeight() <= availableH) {
                low = mid;
            } else {
                high = mid;
            }
        }
        timeView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, low);
    }

    private void startClock() {
        stopClock();
        if (prefs.showMillis()) {
            Choreographer.getInstance().postFrameCallback(frameCallback);
        } else if (timeView != null) {
            timeView.post(secondTicker);
        }
    }

    private void stopClock() {
        Choreographer.getInstance().removeFrameCallback(frameCallback);
        if (timeView != null) {
            timeView.removeCallbacks(secondTicker);
        }
    }

    private void restartClock() {
        startClock();
    }

    private void showMenu() {
        dismissMenu();
        menuView = new LinearLayout(context);
        menuView.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable menuBg = new GradientDrawable();
        menuBg.setColor(Color.WHITE);
        menuBg.setCornerRadius(DpUtils.dp(context, 4));
        menuView.setBackground(menuBg);
        int pad = DpUtils.dp(context, 2);
        menuView.setPadding(pad, pad, pad, pad);

        Button closeBtn = menuButton(context.getString(R.string.action_close_overlay));
        closeBtn.setOnClickListener(v -> {
            dismissMenu();
            Intent close = new Intent(context, OverlayService.class);
            close.setAction(OverlayService.ACTION_CLOSE);
            context.startService(close);
        });
        Button settingsBtn = menuButton(context.getString(R.string.action_settings));
        settingsBtn.setOnClickListener(v -> {
            dismissMenu();
            Intent settings = new Intent(context, MainActivity.class);
            settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(settings);
        });
        menuView.addView(closeBtn);
        menuView.addView(settingsBtn);

        int menuWidth = DpUtils.dp(context, 168);
        menuView.measure(
                View.MeasureSpec.makeMeasureSpec(menuWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int menuHeight = menuView.getMeasuredHeight();

        menuParams = new WindowManager.LayoutParams(
                menuWidth,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        menuParams.gravity = Gravity.TOP | Gravity.START;

        int[] bounds = usableBounds();
        int x = params.x;
        int yBelow = params.y + params.height + DpUtils.dp(context, 4);
        int yAbove = params.y - menuHeight - DpUtils.dp(context, 4);
        int y = yBelow;
        if (yBelow + menuHeight > bounds[3]) {
            y = Math.max(bounds[1], yAbove);
        }
        x = Math.min(Math.max(bounds[0], x), bounds[2] - menuWidth);
        y = Math.min(Math.max(bounds[1], y), bounds[3] - menuHeight);
        menuParams.x = x;
        menuParams.y = y;

        menuView.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_OUTSIDE) {
                dismissMenu();
                return true;
            }
            return false;
        });

        windowManager.addView(menuView, menuParams);
        menuShowing = true;
    }

    private Button menuButton(String label) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextSize(12);
        button.setMinHeight(DpUtils.dp(context, 32));
        button.setMinimumHeight(DpUtils.dp(context, 32));
        button.setAllCaps(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        button.setLayoutParams(lp);
        return button;
    }

    public void dismissMenu() {
        if (menuView != null) {
            try {
                windowManager.removeView(menuView);
            } catch (IllegalArgumentException ignored) {
            }
            menuView = null;
            menuParams = null;
        }
        menuShowing = false;
    }

    public void onConfigurationChanged() {
        if (root == null) {
            return;
        }
        dismissMenu();
        clampParamsToBounds(usableBounds());
        windowManager.updateViewLayout(root, params);
        prefs.setPosition(params.x, params.y);
    }

    /**
     * @return left, top, right, bottom of usable screen area in window coords.
     */
    private int[] usableBounds() {
        int left = 0;
        int top = 0;
        int right;
        int bottom;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics metrics = windowManager.getCurrentWindowMetrics();
            android.graphics.Rect bounds = metrics.getBounds();
            WindowInsets insets = metrics.getWindowInsets();
            int insetLeft = insets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars()).left;
            int insetTop = insets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars()).top;
            int insetRight = insets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars()).right;
            int insetBottom = insets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars()).bottom;
            left = insetLeft;
            top = insetTop;
            right = bounds.width() - insetRight;
            bottom = bounds.height() - insetBottom;
        } else {
            DisplayMetrics dm = new DisplayMetrics();
            windowManager.getDefaultDisplay().getRealMetrics(dm);
            right = dm.widthPixels;
            bottom = dm.heightPixels;
        }
        return new int[]{left, top, right, bottom};
    }

    private void clampParamsToBounds(int[] bounds) {
        int maxX = Math.max(bounds[0], bounds[2] - params.width);
        int maxY = Math.max(bounds[1], bounds[3] - params.height);
        params.x = Math.min(Math.max(bounds[0], params.x), maxX);
        params.y = Math.min(Math.max(bounds[1], params.y), maxY);
    }
}
