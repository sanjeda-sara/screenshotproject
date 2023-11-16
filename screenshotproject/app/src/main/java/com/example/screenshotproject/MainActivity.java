package com.example.screenshotproject;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ToggleButton;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import android.provider.Settings;
import android.graphics.PixelFormat;
import android.view.Gravity;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public static final String CHANNEL_ID = "screen_capture_channel";
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_CODE_DRAW_OVERLAY = 2;

    private WindowManager windowManager;
    private View floatingButton;

    private ScreenCaptureService screenCaptureService;

    private static final int REQUEST_MEDIA_PROJECTION = 10001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindScreenCaptureService();
        createFloatingButton();
        // Request necessary permissions
        requestPermissions();
    }

    private void createFloatingButton() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivityForResult(intent, REQUEST_CODE_DRAW_OVERLAY);
        } else {
            initializeFloatingButton();
        }
    }

    private void initializeFloatingButton() {
        floatingButton = LayoutInflater.from(this).inflate(R.layout.floating_button, null);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.START | Gravity.TOP;
        params.x = 0;
        params.y = 100;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Objects.requireNonNull(windowManager).addView(floatingButton, params);

        Button btnFloating = floatingButton.findViewById(R.id.btnFloating);
        btnFloating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestProjection();
            }
        });
    }

    void requestProjection(){
        startActivityForResult(
                ((MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE))
                        .createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            ScreenCaptureService.ScreenCaptureBinder binder = (ScreenCaptureService.ScreenCaptureBinder) iBinder;
            screenCaptureService = binder.getService();
            screenCaptureService.startProjection(/* pass your MediaProjection data here */);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            // Handle disconnection (if needed)
        }
    };

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                Toast.makeText(this, "Permission Denied. Screenshot functionality may not work properly.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_DRAW_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                // Bind the service after getting overlay permission
                //bindScreenCaptureService();
                initializeFloatingButton();
            } else {
                Toast.makeText(this, "Overlay permission is required for the floating button.", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == RESULT_OK && data != null) {
            if (screenCaptureService != null) {
                screenCaptureService.startProjection(data);
            }
            // start the service
        }
    }

    private void bindScreenCaptureService() {
        Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (windowManager != null && floatingButton != null) {
            windowManager.removeView(floatingButton);
        }
        // Unbind the service to prevent leaks
        if (serviceConnection != null) {
            unbindService(serviceConnection);
        }
    }
}

