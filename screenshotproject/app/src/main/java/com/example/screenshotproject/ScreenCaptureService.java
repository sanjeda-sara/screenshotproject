package com.example.screenshotproject;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class ScreenCaptureService extends Service {

    private MediaProjection mediaProjection;
    private ScreenCapture screenCapture;

    private static final int FOREGROUND_ID = 1;
    private static final String CHANNEL_ID = MainActivity.CHANNEL_ID;

    private final IBinder binder = new ScreenCaptureBinder();

    public class ScreenCaptureBinder extends Binder {
        ScreenCaptureService getService() {
            return ScreenCaptureService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("data")) {
            MediaProjectionManager mediaProjectionManager =
                    (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

            mediaProjection = mediaProjectionManager.getMediaProjection(
                    -1,
                    (Intent) intent.getParcelableExtra("data")
            );

            if (mediaProjection != null) {
                //startForegroundService();
                screenCapture = new ScreenCapture(this, mediaProjection);
                screenCapture.startProjection();
            }
        }
        return START_STICKY;
    }

    private void startForegroundService() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen Capture Service")
                .setContentText("Capturing screen...")
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentIntent(pendingIntent)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)  {
            NotificationChannel channel =
                    new NotificationChannel(CHANNEL_ID, "Capturing..",
                            NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
        startForeground(FOREGROUND_ID, notification);
    }

//    public void startProjection(Intent data) {
//        if (screenCapture != null) {
//            screenCapture.startProjection();
//        }
//    }

    public void startProjection() {
        if (screenCapture != null) {
            screenCapture.startProjection();
        }
    }

    public void startProjection(Intent data) {
        if (data != null) {
            startForegroundService();

            MediaProjectionManager mediaProjectionManager =
                    (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

            mediaProjection = mediaProjectionManager.getMediaProjection(-1, data);

            if (mediaProjection != null) {
                //startForegroundService();
                screenCapture = new ScreenCapture(this, mediaProjection);
                screenCapture.startProjection();
            }
        }
    }



    public void stopProjection() {
        if (screenCapture != null) {
            screenCapture.stopProjection();
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (screenCapture != null) {
            screenCapture.stopProjection();
        }
        stopForeground(true);
        stopSelf();
    }
}
