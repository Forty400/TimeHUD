package com.internal.timeoverlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Low-importance running notification. Badge disabled on channel registration.
 * Legacy channel kept; new installs / upgrades use the no-badge channel id.
 */
public final class NotificationHelper {

    public static final String CHANNEL_ID = "overlay_running_v2";
    public static final String LEGACY_CHANNEL_ID = "overlay_running";
    public static final int NOTIFICATION_ID = 1001;

    private NotificationHelper() {
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) {
            return;
        }

        NotificationChannel existing = nm.getNotificationChannel(CHANNEL_ID);
        if (existing == null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(context.getString(R.string.notification_channel_desc));
            channel.setShowBadge(false);
            channel.enableLights(false);
            channel.enableVibration(false);
            nm.createNotificationChannel(channel);
        } else if (existing.canShowBadge()) {
            existing.setShowBadge(false);
            nm.createNotificationChannel(existing);
        }

        // Do not recreate/re-enable the legacy channel; leave user preference intact.
        NotificationChannel legacy = nm.getNotificationChannel(LEGACY_CHANNEL_ID);
        if (legacy != null && legacy.canShowBadge()) {
            legacy.setShowBadge(false);
            nm.createNotificationChannel(legacy);
        }
    }

    public static Notification build(Context context) {
        ensureChannel(context);

        Intent settings = new Intent(context, MainActivity.class);
        settings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent settingsPi = PendingIntent.getActivity(
                context, 1, settings,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent close = new Intent(context, OverlayService.class);
        close.setAction(OverlayService.ACTION_CLOSE);
        PendingIntent closePi = PendingIntent.getService(
                context, 2, close,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
            builder.setPriority(Notification.PRIORITY_LOW);
        }

        return builder
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_text))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(settingsPi)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setNumber(0)
                .addAction(0, context.getString(R.string.action_settings), settingsPi)
                .addAction(0, context.getString(R.string.action_close_overlay), closePi)
                .build();
    }
}
