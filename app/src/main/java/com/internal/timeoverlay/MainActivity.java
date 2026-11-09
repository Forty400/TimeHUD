package com.internal.timeoverlay;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQ_OVERLAY = 1001;

    private OverlayPrefs prefs;
    private final TimeFormatter formatter = new TimeFormatter();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView previewTime;
    private TextView passthroughStatus;
    private Switch switchOverlay;
    private TextView permissionHint;
    private Button btnGrantPermission;
    private RadioGroup groupPrecision;
    private Switch switchShowDate;
    private RadioGroup groupSize;
    private TextView sizeCurrent;
    private EditText editWidth;
    private EditText editHeight;
    private TextView sizeError;
    private TextView transparencyLabel;
    private SeekBar seekTransparency;
    private RadioGroup groupColor;
    private Switch switchLock;

    private boolean bindingUi;
    private GradientDrawable previewBg;

    private final Runnable previewTicker = new Runnable() {
        @Override
        public void run() {
            refreshPreviewText();
            long delay = prefs.showMillis() ? 16L : (1000L - (System.currentTimeMillis() % 1000L));
            handler.postDelayed(this, delay);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = new OverlayPrefs(this);
        NotificationHelper.ensureChannel(this);
        bindViews();
        wireListeners();
        bindFromPrefs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncOverlaySwitchWithPermission();
        bindFromPrefs();
        handler.removeCallbacks(previewTicker);
        handler.post(previewTicker);
        if (prefs.isEnabled() && Settings.canDrawOverlays(this)) {
            startOverlayService();
        }
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(previewTicker);
        super.onPause();
    }

    private void bindViews() {
        previewTime = findViewById(R.id.previewTime);
        passthroughStatus = findViewById(R.id.passthroughStatus);
        switchOverlay = findViewById(R.id.switchOverlay);
        permissionHint = findViewById(R.id.permissionHint);
        btnGrantPermission = findViewById(R.id.btnGrantPermission);
        groupPrecision = findViewById(R.id.groupPrecision);
        switchShowDate = findViewById(R.id.switchShowDate);
        groupSize = findViewById(R.id.groupSize);
        sizeCurrent = findViewById(R.id.sizeCurrent);
        editWidth = findViewById(R.id.editWidth);
        editHeight = findViewById(R.id.editHeight);
        sizeError = findViewById(R.id.sizeError);
        transparencyLabel = findViewById(R.id.transparencyLabel);
        seekTransparency = findViewById(R.id.seekTransparency);
        groupColor = findViewById(R.id.groupColor);
        switchLock = findViewById(R.id.switchLock);

        previewBg = new GradientDrawable();
        previewBg.setCornerRadius(DpUtils.dp(this, 6));
        previewTime.setBackground(previewBg);
    }

    private void wireListeners() {
        switchOverlay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingUi) {
                return;
            }
            if (isChecked) {
                if (!Settings.canDrawOverlays(this)) {
                    bindingUi = true;
                    switchOverlay.setChecked(false);
                    bindingUi = false;
                    prefs.setEnabled(false);
                    showPermissionUi(true);
                    return;
                }
                prefs.setEnabled(true);
                startOverlayService();
            } else {
                prefs.setEnabled(false);
                Intent close = new Intent(this, OverlayService.class);
                close.setAction(OverlayService.ACTION_CLOSE);
                startService(close);
            }
            refreshPassthroughStatus();
        });

        btnGrantPermission.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQ_OVERLAY);
        });

        groupPrecision.setOnCheckedChangeListener((group, checkedId) -> {
            if (bindingUi) {
                return;
            }
            prefs.setShowMillis(checkedId == R.id.radioMillis);
            ensureCustomAboveMin();
            refreshSizeLabels();
            refreshPreviewChrome();
            notifyOverlay();
        });

        switchShowDate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingUi) {
                return;
            }
            prefs.setShowDate(isChecked);
            ensureCustomAboveMin();
            refreshSizeLabels();
            refreshPreviewChrome();
            notifyOverlay();
        });

        groupSize.setOnCheckedChangeListener((group, checkedId) -> {
            if (bindingUi) {
                return;
            }
            int mode = OverlayPrefs.SIZE_STANDARD;
            if (checkedId == R.id.radioCompact) {
                mode = OverlayPrefs.SIZE_COMPACT;
            } else if (checkedId == R.id.radioLarge) {
                mode = OverlayPrefs.SIZE_LARGE;
            }
            prefs.setSizeMode(mode);
            refreshSizeLabels();
            refreshPreviewChrome();
            notifyOverlay();
        });

        findViewById(R.id.btnApplySize).setOnClickListener(v -> applyCustomSize());

        seekTransparency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                transparencyLabel.setText(getString(R.string.transparency_label, progress));
                if (!fromUser || bindingUi) {
                    return;
                }
                prefs.setTransparency(progress);
                refreshPreviewChrome();
                refreshPassthroughStatus();
                notifyOverlay();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        groupColor.setOnCheckedChangeListener((group, checkedId) -> {
            if (bindingUi) {
                return;
            }
            prefs.setTextColor(colorForRadio(checkedId));
            refreshPreviewChrome();
            notifyOverlay();
        });

        switchLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingUi) {
                return;
            }
            prefs.setLocked(isChecked);
            refreshPassthroughStatus();
            notifyOverlay();
        });
    }

    private void bindFromPrefs() {
        bindingUi = true;
        switchOverlay.setChecked(prefs.isEnabled() && Settings.canDrawOverlays(this));
        showPermissionUi(!Settings.canDrawOverlays(this));

        ((RadioButton) findViewById(prefs.showMillis() ? R.id.radioMillis : R.id.radioSecond))
                .setChecked(true);
        switchShowDate.setChecked(prefs.showDate());

        int sizeId = R.id.radioStandard;
        if (prefs.getSizeMode() == OverlayPrefs.SIZE_COMPACT) {
            sizeId = R.id.radioCompact;
        } else if (prefs.getSizeMode() == OverlayPrefs.SIZE_LARGE) {
            sizeId = R.id.radioLarge;
        }
        // Custom keeps last selected preset radio unchecked visually — show none selected
        // only when custom; still allow choosing a preset later.
        if (prefs.getSizeMode() == OverlayPrefs.SIZE_CUSTOM) {
            groupSize.clearCheck();
        } else {
            ((RadioButton) findViewById(sizeId)).setChecked(true);
        }

        SizeCalculator.Size size = currentSize();
        editWidth.setText(String.valueOf(
                prefs.getSizeMode() == OverlayPrefs.SIZE_CUSTOM
                        ? prefs.getCustomWidth() : size.widthDp));
        editHeight.setText(String.valueOf(
                prefs.getSizeMode() == OverlayPrefs.SIZE_CUSTOM
                        ? prefs.getCustomHeight() : size.heightDp));
        sizeError.setVisibility(View.GONE);

        seekTransparency.setProgress(prefs.getTransparency());
        transparencyLabel.setText(getString(R.string.transparency_label, prefs.getTransparency()));

        int colorId = radioForColor(prefs.getTextColor());
        ((RadioButton) findViewById(colorId)).setChecked(true);

        switchLock.setChecked(prefs.isLocked());
        bindingUi = false;

        refreshSizeLabels();
        refreshPreviewChrome();
        refreshPassthroughStatus();
        refreshPreviewText();
    }

    private void applyCustomSize() {
        String wText = editWidth.getText() != null ? editWidth.getText().toString().trim() : "";
        String hText = editHeight.getText() != null ? editHeight.getText().toString().trim() : "";
        int width;
        int height;
        try {
            width = Integer.parseInt(wText);
            height = Integer.parseInt(hText);
        } catch (NumberFormatException e) {
            showSizeError(getString(R.string.size_error_invalid));
            return;
        }
        String code = SizeCalculator.validateCustom(
                width, height, prefs.showDate(), prefs.showMillis());
        if ("min".equals(code)) {
            showSizeError(getString(R.string.size_error_min));
            return;
        }
        if ("max".equals(code)) {
            showSizeError(getString(R.string.size_error_max));
            return;
        }
        sizeError.setVisibility(View.GONE);
        prefs.setCustomSize(width, height);
        bindingUi = true;
        groupSize.clearCheck();
        bindingUi = false;
        refreshSizeLabels();
        refreshPreviewChrome();
        notifyOverlay();
        // 不弹「尺寸已保存」
    }

    private void showSizeError(String message) {
        sizeError.setText(message);
        sizeError.setVisibility(View.VISIBLE);
    }

    private void ensureCustomAboveMin() {
        if (prefs.getSizeMode() != OverlayPrefs.SIZE_CUSTOM) {
            return;
        }
        SizeCalculator.Size min = SizeCalculator.minCustomSize(prefs.showDate(), prefs.showMillis());
        int w = Math.max(prefs.getCustomWidth(), min.widthDp);
        int h = Math.max(prefs.getCustomHeight(), min.heightDp);
        w = Math.min(w, SizeCalculator.MAX_WIDTH);
        h = Math.min(h, SizeCalculator.MAX_HEIGHT);
        if (w != prefs.getCustomWidth() || h != prefs.getCustomHeight()) {
            prefs.setCustomSize(w, h);
            bindingUi = true;
            editWidth.setText(String.valueOf(w));
            editHeight.setText(String.valueOf(h));
            bindingUi = false;
        }
    }

    private SizeCalculator.Size currentSize() {
        return SizeCalculator.resolve(
                prefs.getSizeMode(),
                prefs.showDate(),
                prefs.showMillis(),
                prefs.getCustomWidth(),
                prefs.getCustomHeight());
    }

    private void refreshSizeLabels() {
        SizeCalculator.Size size = currentSize();
        sizeCurrent.setText(getString(R.string.size_current, size.widthDp, size.heightDp));
    }

    private void refreshPreviewText() {
        previewTime.setText(formatter.format(
                System.currentTimeMillis(), prefs.showDate(), prefs.showMillis()));
    }

    private void refreshPreviewChrome() {
        int transparency = prefs.getTransparency();
        int alpha = Math.round(255f * (100 - transparency) / 100f);
        int bg = (alpha << 24) | (OverlayWindowController.BG_COLOR & 0x00FFFFFF);
        previewBg.setColor(bg);
        previewTime.setTextColor(prefs.getTextColor());

        SizeCalculator.Size size = currentSize();
        ViewGroup.LayoutParams lp = previewTime.getLayoutParams();
        lp.width = DpUtils.dp(this, size.widthDp);
        lp.height = DpUtils.dp(this, size.heightDp);
        previewTime.setLayoutParams(lp);
        fitPreviewFont(size);
    }

    private void fitPreviewFont(SizeCalculator.Size size) {
        CharSequence text = previewTime.getText();
        if (text == null || text.length() == 0) {
            text = formatter.format(System.currentTimeMillis(), prefs.showDate(), prefs.showMillis());
            previewTime.setText(text);
        }
        int availableW = DpUtils.dp(this, size.widthDp) - DpUtils.dp(this, 8);
        int availableH = DpUtils.dp(this, size.heightDp) - DpUtils.dp(this, 4);
        float low = 6f;
        float high = Math.max(availableH, 6f);
        for (int i = 0; i < 12; i++) {
            float mid = (low + high) / 2f;
            previewTime.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, mid);
            previewTime.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            if (previewTime.getMeasuredWidth() <= availableW
                    && previewTime.getMeasuredHeight() <= availableH) {
                low = mid;
            } else {
                high = mid;
            }
        }
        previewTime.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, low);
    }

    private void refreshPassthroughStatus() {
        boolean pass = PassthroughPolicy.isEffectivePassthrough(
                prefs.isLocked(), prefs.getTransparency(), Build.VERSION.SDK_INT);
        passthroughStatus.setText(pass
                ? R.string.passthrough_active
                : R.string.passthrough_inactive);
    }

    private void syncOverlaySwitchWithPermission() {
        if (!Settings.canDrawOverlays(this) && prefs.isEnabled()) {
            prefs.setEnabled(false);
            Intent close = new Intent(this, OverlayService.class);
            close.setAction(OverlayService.ACTION_CLOSE);
            startService(close);
            Toast.makeText(this, R.string.permission_denied_hint, Toast.LENGTH_SHORT).show();
        }
        showPermissionUi(!Settings.canDrawOverlays(this));
    }

    private void showPermissionUi(boolean need) {
        permissionHint.setVisibility(need ? View.VISIBLE : View.GONE);
        btnGrantPermission.setVisibility(need ? View.VISIBLE : View.GONE);
    }

    private void startOverlayService() {
        Intent intent = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void notifyOverlay() {
        if (!prefs.isEnabled() || !Settings.canDrawOverlays(this)) {
            return;
        }
        Intent intent = new Intent(this, OverlayService.class);
        intent.setAction(OverlayService.ACTION_REFRESH);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private static int colorForRadio(int id) {
        if (id == R.id.colorBlack) {
            return TextColors.BLACK;
        }
        if (id == R.id.colorRed) {
            return TextColors.RED;
        }
        if (id == R.id.colorYellow) {
            return TextColors.YELLOW;
        }
        if (id == R.id.colorGreen) {
            return TextColors.GREEN;
        }
        if (id == R.id.colorBlue) {
            return TextColors.BLUE;
        }
        return TextColors.WHITE;
    }

    private static int radioForColor(int color) {
        if (color == TextColors.BLACK) {
            return R.id.colorBlack;
        }
        if (color == TextColors.RED) {
            return R.id.colorRed;
        }
        if (color == TextColors.YELLOW) {
            return R.id.colorYellow;
        }
        if (color == TextColors.GREEN) {
            return R.id.colorGreen;
        }
        if (color == TextColors.BLUE) {
            return R.id.colorBlue;
        }
        return R.id.colorWhite;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY) {
            syncOverlaySwitchWithPermission();
            bindFromPrefs();
        }
    }
}
