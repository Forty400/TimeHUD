package com.internal.timeoverlay;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;

public class OverlayService extends Service {

    public static final String ACTION_CLOSE = "com.internal.timeoverlay.action.CLOSE";
    public static final String ACTION_REFRESH = "com.internal.timeoverlay.action.REFRESH";

    private OverlayPrefs prefs;
    private OverlayWindowController controller;
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new OverlayPrefs(this);
        controller = new OverlayWindowController(this);
        NotificationHelper.ensureChannel(this);

        prefListener = (sharedPreferences, key) -> {
            if (controller == null || !controller.isShowing()) {
                return;
            }
            if (OverlayPrefs.KEY_ENABLED.equals(key) && !prefs.isEnabled()) {
                stopOverlayAndSelf();
                return;
            }
            controller.applyAll();
        };
        prefs.register(prefListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_CLOSE.equals(action)) {
            prefs.setEnabled(false);
            stopOverlayAndSelf();
            return START_NOT_STICKY;
        }

        if (!Settings.canDrawOverlays(this)) {
            prefs.setEnabled(false);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!prefs.isEnabled()) {
            stopOverlayAndSelf();
            return START_NOT_STICKY;
        }

        startAsForeground();
        controller.show();

        if (ACTION_REFRESH.equals(action)) {
            controller.applyAll();
        }
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (controller != null && controller.isShowing()) {
            controller.onConfigurationChanged();
        }
    }

    @Override
    public void onDestroy() {
        if (prefListener != null) {
            prefs.unregister(prefListener);
        }
        if (controller != null) {
            controller.hide();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
