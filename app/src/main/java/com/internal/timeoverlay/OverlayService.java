package com.internal.timeoverlay;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;

public class OverlayService extends Service {

    public static final String ACTION_CLOSE = "com.internal.timeoverlay.action.CLOSE";
    public static final String ACTION_REFRESH = "com.internal.timeoverlay.action.REFRESH";
    public static final String ACTION_STATE_CHANGED = "com.internal.timeoverlay.action.STATE_CHANGED";

    private OverlayPrefs prefs;
    private OverlayWindowController controller;
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable applyRunnable = () -> {
        if (controller != null && controller.isShowing() && prefs.isEnabled()) {
            controller.applyAll();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new OverlayPrefs(this);
        controller = new OverlayWindowController(this);
        NotificationHelper.ensureChannel(this);

        prefListener = (sharedPreferences, key) -> mainHandler.post(() -> {
            if (OverlayPrefs.KEY_ENABLED.equals(key)) {
                broadcastState();
                if (!prefs.isEnabled()) {
                    stopOverlayAndSelf();
                } else if (Settings.canDrawOverlays(this)) {
                    startAsForeground();
                    controller.show();
                }
                return;
            }
            if (controller != null && controller.isShowing()) {
                controller.applyAll();
            }
        });
        prefs.register(prefListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_CLOSE.equals(action)) {
            prefs.setEnabled(false);
            broadcastState();
            stopOverlayAndSelf();
            return START_NOT_STICKY;
        }

        if (!Settings.canDrawOverlays(this)) {
            prefs.setEnabled(false);
            broadcastState();
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!prefs.isEnabled()) {
            broadcastState();
            stopOverlayAndSelf();
            return START_NOT_STICKY;
        }

        startAsForeground();
        controller.show();
        // Always re-apply latest prefs (style / format / size).
        mainHandler.removeCallbacks(applyRunnable);
        mainHandler.post(applyRunnable);
        broadcastState();
        return START_STICKY;
    }

    private void startAsForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                    NotificationHelper.NOTIFICATION_ID,
                    NotificationHelper.build(this),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID, NotificationHelper.build(this));
        }
    }

    private void stopOverlayAndSelf() {
        mainHandler.removeCallbacks(applyRunnable);
        if (controller != null) {
            controller.hide();
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.cancel(NotificationHelper.NOTIFICATION_ID);
        }
        stopSelf();
    }

    private void broadcastState() {
        Intent i = new Intent(ACTION_STATE_CHANGED);
        i.setPackage(getPackageName());
        i.putExtra(OverlayPrefs.KEY_ENABLED, prefs.isEnabled());
        sendBroadcast(i);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (controller != null && controller.isShowing()) {
            controller.onConfigurationChanged();
        }
    }

    @Override
    public void onDestroy() {
        mainHandler.removeCallbacks(applyRunnable);
        if (prefListener != null) {
            prefs.unregister(prefListener);
        }
        if (controller != null) {
            controller.hide();
        }
        broadcastState();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
