package org.tensorflow.lite.examples.detection;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.util.Log;
import android.util.Range;
import android.util.SizeF;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.List;

public class TOFDetector extends CameraDevice.StateCallback {
    private static final int PREVIEW_WIDTH = 240;
    private static final int PREVIEW_HEIGHT = 180;

    private static TOFDetector instance;
    private static Context context;
    private static CameraManager cameraManager;

    private static ImageReader previewReader;
    private static DepthFrameAvailableListener imageAvailableListener;

    private TOFDetector(Context con, CameraManager camMan) {
        context = con;
        cameraManager = camMan;
    }

    public static TOFDetector getInstance(Context con, CameraManager camMan) {
        if (instance == null) {
            instance = new TOFDetector(con, camMan);
        }
        return instance;
    }

    public String getTOFCamera() {
        try {
            for (String camera : cameraManager.getCameraIdList()) {
                CameraCharacteristics chars = cameraManager.getCameraCharacteristics(camera);
                final int[] capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);

                boolean backFacing = (chars.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK);
                boolean depthCapable = false;
                for (int capability : capabilities) {
                    boolean capable = (capability == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT);
                    depthCapable = (depthCapable || capable);
                }

                if (depthCapable && backFacing) {
                    // 센서 크기는 실제 캡쳐 화면 크기보다 클 수 있다.
                    SizeF sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                    Log.i("TOF", "Sensor Size = " + sensorSize);

                    // FOV 리사이징용 정보 출력
                    float[] focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                    if (focalLengths.length > 0) {
                        float focalLength = focalLengths[0];
                        double fov = 2 * Math.atan(sensorSize.getWidth() / (2 * focalLength));
                        Log.i("TOF", "Computed FOV = " + fov);
                    }
                    return camera;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void openCamera(String cameraID) {
        try {
            int permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA);
            if (PackageManager.PERMISSION_GRANTED == permission) {
                cameraManager.openCamera(cameraID, this, null);
            }
            else {
                Log.e("TOF", "Camera Permission Not Available");
            }
        }
        catch (Exception e) {
            Log.e("TOF", "Open Camera Exception" + e);
            e.printStackTrace();
        }
    }

    @Override
    public void onOpened(@NonNull CameraDevice cameraDevice) {
        imageAvailableListener = new DepthFrameAvailableListener(context);
        previewReader = ImageReader.newInstance(PREVIEW_WIDTH, PREVIEW_HEIGHT, ImageFormat.DEPTH16, 2);
        previewReader.setOnImageAvailableListener(imageAvailableListener, null);
        try {
            CaptureRequest.Builder previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.set(CaptureRequest.JPEG_ORIENTATION, 0);
            Range<Integer> fpsRange = new Range<>(15, 30);
            previewBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
            previewBuilder.addTarget(previewReader.getSurface());

            List<Surface> targetSurfaces = Arrays.asList(previewReader.getSurface());
            cameraDevice.createCaptureSession(
                    targetSurfaces,
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.i("TOF", "Capture Session Created");
                            previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                            try {
                                cameraCaptureSession.setRepeatingRequest(previewBuilder.build(), null, null);
                            }
                            catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.e("TOF", "Camera Capture Session Callback Internal Error!!");
                        }
                    }, null
            );
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice cameraDevice) { }
    @Override
    public void onError(@NonNull CameraDevice cameraDevice, int i) { }
}
