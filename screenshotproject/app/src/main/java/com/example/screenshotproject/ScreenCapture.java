package com.example.screenshotproject;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.view.WindowManager;



import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.view.Display;
import android.view.WindowManager;

import java.nio.ByteBuffer;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScreenCapture {

    private static final int MSG_START_PROJECTION = 1;
    private static final int MSG_STOP_PROJECTION = 2;

    private MediaProjection mediaProjection;
    private Context context;
    private HandlerThread handlerThread;
    private Handler handler;
    private ImageReader imageReader;

    public ScreenCapture(Context context, MediaProjection mediaProjection) {
        this.context = context;
        this.mediaProjection = mediaProjection;

        handlerThread = new HandlerThread("ScreenCaptureThread");
        handlerThread.start();

        handler = new Handler(handlerThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_START_PROJECTION:
                        startProjectionInternal();
                        break;
                    case MSG_STOP_PROJECTION:
                        stopProjectionInternal();
                        break;
                }
                return true;
            }
        });
    }

    public void startProjection() {
        handler.sendEmptyMessage(MSG_START_PROJECTION);
    }

    private void startProjectionInternal() {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int density = metrics.densityDpi;
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);

        mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                width,
                height,
                density,
                DisplayMetrics.DENSITY_DEFAULT,
                imageReader.getSurface(),
                null,
                null);

        // Implement your logic to handle the captured screen content here
        SystemClock.sleep(1000);  // Simulating some delay (adjust as needed)
        captureAndSaveScreenshot();
    }

    private void captureAndSaveScreenshot() {
        Image image = imageReader.acquireLatestImage();
        if (image != null) {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            Bitmap bitmap = Bitmap.createBitmap(
                    image.getWidth(),
                    image.getHeight(),
                    Bitmap.Config.ARGB_8888
            );
            buffer.rewind();
            bitmap.copyPixelsFromBuffer(buffer);

            // Save the screenshot to the "Pictures" folder
            saveScreenshot(bitmap);

            image.close();
        }
    }

    private void saveScreenshot(Bitmap bitmap) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "Screenshot_" + timeStamp + ".png";

        File storageDir = context.getExternalFilesDir("Pictures");
        File imageFile = new File(storageDir, fileName);

        try {
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopProjection() {
        handler.sendEmptyMessage(MSG_STOP_PROJECTION);
    }

    private void stopProjectionInternal() {
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }

        // Implement any cleanup code or release additional resources if necessary
    }
}
