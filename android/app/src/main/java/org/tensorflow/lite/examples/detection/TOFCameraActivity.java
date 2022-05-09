package org.tensorflow.lite.examples.detection;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;

public class TOFCameraActivity extends AppCompatActivity {
    private Matrix defaultBitmapTransform;
    private TextureView screen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_tofcamera);

//        screen = findViewById(R.id.textureView);

        TOFDetector tof = new TOFDetector(this, (CameraManager) getSystemService(CAMERA_SERVICE), null);
        tof.openCamera(tof.getTOFCamera());
    }

//    public void draw(Bitmap bitmap) {
//        if (screen != null) {
//            drawBitmapOnView(bitmap, screen);
//        } else {
//            Log.e("TOF", "TextureView is not loaded.");
//        }
//    }

    private void drawBitmapOnView(Bitmap bitmap, TextureView textureView) {
        Canvas canvas = textureView.lockCanvas();
        canvas.drawBitmap(bitmap, bitmapTransform(textureView), null);
        textureView.unlockCanvasAndPost(canvas);
    }

    private Matrix bitmapTransform(TextureView textureView) {
        if (defaultBitmapTransform == null || textureView.getWidth() == 0 || textureView.getHeight() == 0) {
            Matrix matrix = new Matrix();
            int centerX = textureView.getWidth() / 2;
            int centerY = textureView.getHeight() / 2;
            int bufferWidth = DepthFrameAvailableListener.WIDTH;
            int bufferHeight = DepthFrameAvailableListener.HEIGHT;

            RectF bufferRect = new RectF(0, 0, bufferWidth, bufferHeight);
            RectF viewRect = new RectF(0, 0, textureView.getWidth(), textureView.getHeight());
            matrix.setRectToRect(bufferRect, viewRect, Matrix.ScaleToFit.CENTER);
            matrix.postRotate(90, centerX, centerY);

            defaultBitmapTransform = matrix;
        }
        return defaultBitmapTransform;
    }
}