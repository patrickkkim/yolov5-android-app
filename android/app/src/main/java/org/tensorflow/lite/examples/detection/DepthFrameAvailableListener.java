package org.tensorflow.lite.examples.detection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;

import java.nio.ShortBuffer;

public class DepthFrameAvailableListener implements ImageReader.OnImageAvailableListener {
    public static final int WIDTH = 240;
    public static final int HEIGHT = 180;
    public static final float MIN_CONFIDENCE = 0.1f;
    public static final float RANGE_MIN = 100.0f;
    public static final float RANGE_MAX = 1600.0f;

    private int[] rawDataMask;
    private TOFCameraActivity depthFrameActivity;

    public DepthFrameAvailableListener(Context activity) {
        depthFrameActivity = (TOFCameraActivity) activity;
    }

    @Override
    public void onImageAvailable(ImageReader imageReader) {
        try {
            Image image = imageReader.acquireNextImage();
            if (image != null) {
                if (image.getFormat() == ImageFormat.DEPTH16) {
                    processImage(image);
                    drawRawData();
                }
                image.close();
            }
        } catch (Exception e) {
            Log.e("TOF", "Next Image Not Acquired.");
            e.printStackTrace();
        }
    }

    // DEPTH16 이미지만 처리
    private void processImage(Image image) {
        ShortBuffer shortBuffer = image.getPlanes()[0].getBuffer().asShortBuffer();
        int[] mask = new int[WIDTH * HEIGHT];

        for (int y = 0; y < HEIGHT; ++y) {
            for (int x = 0; x < WIDTH; ++x) {
                int index = y * WIDTH + x;
                short depthSample = shortBuffer.get(index);
                int depthRange = extractRange(depthSample, MIN_CONFIDENCE);
                mask[index] = depthRange;
            }
        }

        rawDataMask = mask;
    }

    private int extractRange(short sample, float confidenceFilter) {
        int depthRange = (short) (sample & 0x1FFF);
        int depthConfidence = (short) ((sample >> 13) & 0x7);
        float depthPercentage = (depthConfidence == 0 ? 1.f : (depthConfidence - 1) / 7.f);

        return depthPercentage > confidenceFilter ? normalizeRange(depthRange) : 0;
    }

    private int normalizeRange(int range) {
        float normalized = (float)range - RANGE_MIN;

        // Min-max normalization
        normalized = Math.max(RANGE_MIN, normalized);
        normalized = Math.min(RANGE_MAX, normalized);
        // 0 ~ 255 Normalization
        normalized = normalized - RANGE_MIN;
        normalized = normalized / (RANGE_MAX - RANGE_MIN) * 255;
        return (int)normalized;
    }

    private Bitmap convertToRGBBitmap(int[] mask) {
        Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);

        for (int y = 0; y < HEIGHT; ++y) {
            for (int x = 0; x < WIDTH; ++x) {
                int index = y * WIDTH + x;
                bitmap.setPixel(x, y, Color.argb(255, 0, 0, mask[index]));
            }
        }

        return bitmap;
    }

    private void drawRawData() {
        if (depthFrameActivity != null) {
            Bitmap rawData = convertToRGBBitmap(rawDataMask);
            depthFrameActivity.draw(rawData);
        }
    }
}
